/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meituan.dorado.codec.octo;

import com.meituan.dorado.codec.Codec;
import com.meituan.dorado.codec.octo.meta.Header;
import com.meituan.dorado.codec.octo.meta.MessageType;
import com.meituan.dorado.common.Constants;
import com.meituan.dorado.common.exception.ProtocolException;
import com.meituan.dorado.common.exception.TimeoutException;
import com.meituan.dorado.common.util.CommonUtil;
import com.meituan.dorado.rpc.meta.RpcInvocation;
import com.meituan.dorado.rpc.meta.RpcResult;
import com.meituan.dorado.serialize.DoradoSerializerFactory;
import com.meituan.dorado.serialize.thrift.ThriftCodecSerializer;
import com.meituan.dorado.trace.meta.TraceTimeline;
import com.meituan.dorado.transport.Channel;
import com.meituan.dorado.transport.meta.DefaultRequest;
import com.meituan.dorado.transport.meta.DefaultResponse;
import com.meituan.dorado.util.BytesUtil;
import com.meituan.dorado.util.ChecksumUtil;
import com.meituan.dorado.util.CompressUtil.CompressType;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Meituan && Dianping(XMD) Rpc Protocol
 * unified protocol                                                                    *
 * +-----------------------------------------------------------+--------------------+--------------------+--------------------+-------------------------+
 * +                      Package Header                                            |       Header       |       Body         |         Footer          |
 * +--------+----------------+----------------+----------------+--------------------+--------------------+--------------------+-------------------------+
 * + 2Byte  |    1Byte       |      1Byte     |    4Byte       |       2Byte        | header length Byte |  body length Byte  |          4Byte          |
 * +--------+----------------+----------------+----------------+--------------------+--------------------+--------------------+-------------------------+
 * + magic  |   version      |    protocol    |  total length  |   header length    |        header      |       body         |         checksum        |
 * +--------+----------------+----------------+----------------+--------------------+--------------------+--------------------+-------------------------+
 * + 0xABBA |                |                |                |len before compress |                    |                    |         optional        |
 * +--------+----------------+----------------+----------------+--------------------+--------------------+--------------------+--------------+----------+
 * <p>
 */
public abstract class OctoCodec implements Codec {

    private static final int PACKAGE_HEAD_LENGTH = 10;
    private static final int PACKAGE_HEAD_INFO_LENGTH = 4;
    private static final int TOTAL_LEN_FIELD_LENGTH = 4;
    private static final int HEADER_LEN_FIELD_LENGTH = 2;
    private static final int CHECKSUM_FIELD_LENGTH = 4;

    private static final short MAGIC = (short) 0xABBA;
    private static final short MAGIC_LENGTH = 2;
    private static final byte MAGIC_FIRST = (byte) 0xAB;
    private static final byte MAGIC_SECOND = (byte) 0xBA;

    private static final String ATTACH_INFO_IS_DO_CHECK = "checksum";
    private static final String ATTACH_INFO_COMPRESS_TYPE = "compressType";
    private static final String ATTACH_INFO_SERIALIZE_CODE = "serializeCode";

    @Override
    public byte[] encode(Channel channel, Object message, Map<String, Object> attachments) throws ProtocolException {
        Object obj;
        byte[] bodyBytes = new byte[0];
        if (message instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) message;
            if (!request.isOctoProtocol()) {
                return encodeThrift(message);
            }

            TraceTimeline.record(TraceTimeline.ENCODE_START_TS, request);

            if (request.getMessageType() == MessageType.Normal.getValue()) {
                byte serialize = request.getSerialize();
                bodyBytes = encodeReqBody(serialize, request);
            }
            TraceTimeline.record(TraceTimeline.ENCODE_BODY_END_TS, request);
            obj = request;
        } else if (message instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) message;
            response.setPort(channel.getLocalAddress().getPort());
            if (!response.isOctoProtocol()) {
                return encodeThrift(message);
            }
            TraceTimeline.record(TraceTimeline.ENCODE_START_TS, response.getRequest());
            if (response.getMessageType() == MessageType.Normal.getValue()) {
                byte serialize = response.getSerialize();
                bodyBytes = encodeRspBody(serialize, response);
            }
            TraceTimeline.record(TraceTimeline.ENCODE_BODY_END_TS, response.getRequest());
            obj = response;
        } else {
            throw new ProtocolException("Encode object type is invalid.");
        }

        byte[] headerBytes;
        try {
            headerBytes = encodeOctoHeader(obj);
        } catch (TException e) {
            throw new ProtocolException("Serialize Octo protocol header failed, cause " + e.getMessage(), e);
        }

        byte[] msgBuff = generateSendMessageBuff(obj, headerBytes, bodyBytes);
        MetaUtil.recordTraceInfoInEncode(msgBuff, message);
        return msgBuff;
    }

    @Override
    public Object decode(Channel channel, byte[] buffer, Map<String, Object> attachments) throws ProtocolException {
        if (buffer.length < PACKAGE_HEAD_LENGTH) {
            throw new ProtocolException("Message length less than header length");
        }
        if (!isOctoProtocol(buffer)) {
            // 非Octo协议
            return decodeThrift(buffer, attachments);
        }
        TraceTimeline timeline = TraceTimeline.newRecord(CommonUtil.objectToBool(attachments.get(Constants.TRACE_IS_RECORD_TIMELINE), false),
                TraceTimeline.DECODE_START_TS);

        Map<String, Object> attachInfo = new HashMap<String, Object>();
        byte[] headerBodyBytes = getHeaderBodyBuff(buffer, attachInfo);
        int headerBodyLength = headerBodyBytes.length;
        short headerLength = BytesUtil.bytes2short(buffer, PACKAGE_HEAD_INFO_LENGTH + TOTAL_LEN_FIELD_LENGTH);

        byte[] headerBytes = new byte[headerLength];
        System.arraycopy(headerBodyBytes, 0, headerBytes, 0, headerLength);
        byte[] bodyBytes = new byte[headerBodyLength - headerLength];
        System.arraycopy(headerBodyBytes, headerLength, bodyBytes, 0, headerBodyLength - headerLength);

        byte serialize = (byte) attachInfo.get(ATTACH_INFO_SERIALIZE_CODE);
        Header header = null;
        try {
            header = decodeOctoHeader(headerBytes);
        } catch (TException e) {
            throw new ProtocolException("Deserialize Octo protocol header failed, cause " + e.getMessage(), e);
        }

        if (header.isSetRequestInfo()) {
            DefaultRequest request = MetaUtil.convertHeaderToRequest(header);
            request.setSerialize(serialize);
            request.setCompressType((CompressType) attachInfo.get(ATTACH_INFO_COMPRESS_TYPE));
            request.setDoChecksum((Boolean) attachInfo.get(ATTACH_INFO_IS_DO_CHECK));
            if (request.getMessageType() != MessageType.Normal.getValue()) {
                if (request.getMessageType() == MessageType.NormalHeartbeat.getValue() ||
                        request.getMessageType() == MessageType.ScannerHeartbeat.getValue()) {
                    request.setHeartbeat(true);
                }
                return request;
            }

            timeline.record(TraceTimeline.DECODE_BODY_START_TS);
            RpcInvocation bodyObj = decodeReqBody(serialize, bodyBytes, request);
            request.setData(bodyObj);

            bodyObj.putAttachment(Constants.TRACE_TIMELINE, timeline);
            MetaUtil.recordTraceInfoInDecode(buffer, request);
            return request;
        } else if (header.isSetResponseInfo()) {
            DefaultResponse response = MetaUtil.convertHeaderToResponse(header);
            try {
                if (response.getMessageType() != MessageType.Normal.getValue()) {
                    if (response.getMessageType() == MessageType.NormalHeartbeat.getValue() ||
                            response.getMessageType() == MessageType.ScannerHeartbeat.getValue()) {
                        response.setHeartbeat(true);
                    }
                    return response;
                }
                timeline.record(TraceTimeline.DECODE_BODY_START_TS);
                RpcResult bodyObj = decodeRspBody(serialize, bodyBytes, response);
                response.setResult(bodyObj);
            } catch (Exception e) {
                if (e instanceof TimeoutException) {
                    throw e;
                }
                response.setException(e);
            }
            if (response.getRequest() != null && response.getRequest().getData() != null) {
                TraceTimeline.copyRecord(timeline, response.getRequest().getData());
            }
            MetaUtil.recordTraceInfoInDecode(buffer, response);
            return response;
        } else {
            throw new ProtocolException("Deserialize header lack request or response info.");
        }
    }

    protected RpcInvocation decodeReqBody(byte serialize, byte[] buff, DefaultRequest request) {
        try {
            return ThriftCodecSerializer.decodeReqBody(buff, request);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Thrift decode request failed, cause " + e.getMessage(), e);
        }
    }

    protected RpcResult decodeRspBody(byte serialize, byte[] buff, DefaultResponse response) {
        try {
            return ThriftCodecSerializer.decodeRspBody(buff, response);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            } else if (e instanceof TimeoutException) {
                throw (TimeoutException) e;
            }
            throw new ProtocolException("Thrift decode response failed, cause " + e.getMessage(), e);
        }
    }

    protected byte[] encodeReqBody(byte serialize, DefaultRequest request) {
        try {
            return ThriftCodecSerializer.encodeReqBody(request);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Thrift encode request failed, cause " + e.getMessage(), e);
        }
    }

    protected byte[] encodeRspBody(byte serialize, DefaultResponse response) {
        try {
            return ThriftCodecSerializer.encodeRspBody(response);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Thrift encode response failed, cause " + e.getMessage(), e);
        }
    }

    protected Header decodeOctoHeader(byte[] headerBytes) throws TException {
        TDeserializer deserializer = new TDeserializer();
        Header header = new Header();
        deserializer.deserialize(header, headerBytes);
        return header;
    }

    private byte[] encodeOctoHeader(Object message) throws TException {
        TSerializer serializer = new TSerializer();
        Header header;
        if (message instanceof DefaultRequest) {
            DefaultRequest request = (DefaultRequest) message;
            header = MetaUtil.convertRequestToHeader(request);
        } else if (message instanceof DefaultResponse) {
            DefaultResponse response = (DefaultResponse) message;
            header = MetaUtil.convertResponseToHeader(response);
        } else {
            throw new ProtocolException("Serialize header, but object is invalid.");
        }
        return serializer.serialize(header);
    }

    private byte[] getHeaderBodyBuff(byte[] buffer, Map<String, Object> attachInfo) {
        int readerIndex = MAGIC_LENGTH;
        byte version = buffer[readerIndex++]; // no use temporary
        byte protocol = buffer[readerIndex++];

        int totalLength = BytesUtil.bytes2int(buffer, readerIndex);
        readerIndex += TOTAL_LEN_FIELD_LENGTH + HEADER_LEN_FIELD_LENGTH;

        int msgNeedLength = totalLength + TOTAL_LEN_FIELD_LENGTH + PACKAGE_HEAD_INFO_LENGTH;
        if (buffer.length < msgNeedLength) {
            throw new ProtocolException("Message length less than need length");
        }

        boolean needCheck = (protocol & 0x80) == 0x80;
        CompressType compressType = getCompressType(protocol);
        byte serialize = (byte) (protocol & 0x03);

        int headerBodyLength;
        if (needCheck) {
            headerBodyLength = totalLength - HEADER_LEN_FIELD_LENGTH - CHECKSUM_FIELD_LENGTH;
            boolean isCheckOk = compareCheckSum(buffer, readerIndex, headerBodyLength);
            if (!isCheckOk) {
                throw new ProtocolException("Message checksum different");
            }
        } else {
            headerBodyLength = totalLength - HEADER_LEN_FIELD_LENGTH;
        }

        byte[] headerBodyBytes = new byte[headerBodyLength];
        System.arraycopy(buffer, readerIndex, headerBodyBytes, 0, headerBodyLength);
        try {
            headerBodyBytes = compressType.uncompress(headerBodyBytes);
        } catch (IOException e) {
            throw new ProtocolException("Message uncompress by " + compressType + " failed.", e);
        }
        attachInfo.put(ATTACH_INFO_IS_DO_CHECK, needCheck);
        attachInfo.put(ATTACH_INFO_COMPRESS_TYPE, compressType);
        attachInfo.put(ATTACH_INFO_SERIALIZE_CODE, serialize);
        return headerBodyBytes;
    }

    private boolean isOctoProtocol(byte[] buffer) {
        short magic = BytesUtil.bytes2short(buffer, 0);
        return magic == MAGIC;
    }

    public byte[] generateSendMessageBuff(Object obj, byte[] headerBytes, byte[] bodyBytes) {
        boolean needCheck = false;
        CompressType compressType = CompressType.NO;
        byte serialize = DoradoSerializerFactory.SerializeType.THRIFT_CODEC.getCode();
        byte version = 0x00;
        if (obj instanceof DefaultRequest) {
            needCheck = ((DefaultRequest) obj).getDoChecksum();
            compressType = ((DefaultRequest) obj).getCompressType();
            version = ((DefaultRequest) obj).getVersion();
            serialize = ((DefaultRequest) obj).getSerialize();
        } else if (obj instanceof DefaultResponse) {
            needCheck = ((DefaultResponse) obj).getDoChecksum();
            compressType = ((DefaultResponse) obj).getCompressType();
            version = ((DefaultResponse) obj).getVersion();
            serialize = ((DefaultResponse) obj).getSerialize();
        }

        int headerLength = headerBytes.length;
        byte[] headerBodyBytes = new byte[headerLength + bodyBytes.length];
        System.arraycopy(headerBytes, 0, headerBodyBytes, 0, headerLength);
        System.arraycopy(bodyBytes, 0, headerBodyBytes, headerLength, bodyBytes.length);

        byte[] packageHead = new byte[PACKAGE_HEAD_LENGTH];
        int writerIndex = 0;
        BytesUtil.short2bytes(MAGIC, packageHead, writerIndex);
        writerIndex += 2;
        packageHead[writerIndex++] = version;

        byte protocol = (byte) (version | (needCheck ? 0x80 : 0x00) | getCompressCode(compressType) | serialize);
        packageHead[writerIndex++] = protocol;

        try {
            headerBodyBytes = compressType.compress(headerBodyBytes);
        } catch (IOException e) {
            throw new ProtocolException("Message uncompress by " + compressType + " failed.", e);
        }
        int totalLenFieldLength = HEADER_LEN_FIELD_LENGTH + headerBodyBytes.length;
        BytesUtil.int2bytes(needCheck ? totalLenFieldLength + CHECKSUM_FIELD_LENGTH : totalLenFieldLength,
                packageHead, writerIndex);
        writerIndex += 4;
        BytesUtil.short2bytes((short) headerLength, packageHead, writerIndex);

        byte[] wholeMsgBuff = new byte[PACKAGE_HEAD_INFO_LENGTH + TOTAL_LEN_FIELD_LENGTH + totalLenFieldLength];
        System.arraycopy(packageHead, 0, wholeMsgBuff, 0, PACKAGE_HEAD_LENGTH);
        System.arraycopy(headerBodyBytes, 0, wholeMsgBuff, PACKAGE_HEAD_LENGTH, headerBodyBytes.length);

        if (needCheck) {
            byte[] checkSum = ChecksumUtil.genAdler32ChecksumBytes(wholeMsgBuff);
            byte[] msgBuffWithCheckSum = new byte[wholeMsgBuff.length + CHECKSUM_FIELD_LENGTH];
            System.arraycopy(wholeMsgBuff, 0, msgBuffWithCheckSum, 0, wholeMsgBuff.length);
            System.arraycopy(checkSum, 0, msgBuffWithCheckSum, wholeMsgBuff.length, checkSum.length);
            return msgBuffWithCheckSum;
        }
        return wholeMsgBuff;
    }

    private boolean compareCheckSum(byte[] buffer, int readerIndex, int headerBodyLength) {
        byte[] msgCheckBuff = new byte[buffer.length - CHECKSUM_FIELD_LENGTH];
        System.arraycopy(buffer, 0, msgCheckBuff, 0, buffer.length - CHECKSUM_FIELD_LENGTH);

        byte[] checksumField = new byte[CHECKSUM_FIELD_LENGTH];
        System.arraycopy(buffer, readerIndex + headerBodyLength, checksumField, 0, CHECKSUM_FIELD_LENGTH);

        return BytesUtil.bytesEquals(ChecksumUtil.genAdler32ChecksumBytes(msgCheckBuff), checksumField);
    }

    private Object decodeThrift(byte[] buffer, Map<String, Object> attachments) {
        int bodyLength = BytesUtil.bytes2int(buffer, 0);
        if (bodyLength + 4 > buffer.length) {
            throw new ProtocolException("Message length less than need length");
        }
        try {
            byte[] bodyBytes = new byte[bodyLength];
            System.arraycopy(buffer, TOTAL_LEN_FIELD_LENGTH, bodyBytes, 0, bodyLength);
            return ThriftCodecSerializer.decodeThrift(bodyBytes, attachments);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            } else if (e instanceof TimeoutException) {
                throw (TimeoutException) e;
            }
            throw new ProtocolException("Origin thrift decode failed: " + e.getMessage(), e);
        }
    }

    private byte[] encodeThrift(Object obj) {
        try {
            return ThriftCodecSerializer.encodeThrift(obj);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Origin thrift encode failed: " + e.getMessage(), e);
        }
    }

    private CompressType getCompressType(byte protocol) {
        if ((protocol & 0x20) == 0x20) {
            return CompressType.SNAPPY;
        } else if ((protocol & 0x40) == 0x40) {
            return CompressType.GZIP;
        } else {
            return CompressType.NO;
        }
    }

    private byte getCompressCode(CompressType compressType) {
        if (CompressType.SNAPPY.equals(compressType)) {
            return 0x20;
        } else if (CompressType.GZIP.equals(compressType)) {
            return 0x40;
        } else {
            return 0x00;
        }
    }
}
