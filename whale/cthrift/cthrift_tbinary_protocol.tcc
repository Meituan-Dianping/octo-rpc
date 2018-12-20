/*
 * Copyright (c) 2011-2018, Meituan Dianping. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#ifndef CTHRIFT_SRC_CTHRIFT_TBINARY_PROTOCOL_TCC_
#define CTHRIFT_SRC_CTHRIFT_TBINARY_PROTOCOL_TCC_

#include "cthrift_transport.h"
#include "cthrift_tbinary_protocol.h"

namespace meituan_cthrift {
using namespace std;
using namespace muduo;

template<>
int32_t CthriftTBinaryProtocolT<TMemoryBuffer>::GetSeqID(void) {

  int32_t i32_size = 0;
  readI32(i32_size);

  // follow two just offset
  string str_name;
  int8_t i8_type;

  int32_t i32_seq_id = 0;

  if (0 > i32_size && ((i32_size & VERSION_MASK) == VERSION_1)) {
    readString(str_name);
    readI32(i32_seq_id);

    CTHRIFT_LOG_DEBUG("seq_id " << i32_seq_id);
    return i32_seq_id;
  } else if (0 <= i32_size && !(this->strict_read_)) {
    readStringBody(str_name, i32_size);
    readByte(i8_type);
    readI32(i32_seq_id);

    CTHRIFT_LOG_DEBUG("seq_id " << i32_seq_id);
    return i32_seq_id;
  }

  return 0;
}

template<class Transport_>
int32_t
CthriftTBinaryProtocolT<Transport_>::GetSeqID(void) {
  return 0;
}

template<>
uint32_t CthriftTBinaryProtocolT<TMemoryBuffer>::
writeMessageBegin(const std::string &name,
                  const TMessageType messageType,
                  const int32_t seqid) {
  CTHRIFT_LOG_DEBUG("async writeMessageBegin");

  int32_t i32_seq_id = seqid;
  if (0 == seqid) {
    i32_seq_id = g_atomic_i32_seq_id
        .incrementAndGet();

    if (CTHRIFT_UNLIKELY(std::numeric_limits<int32_t>::max() == i32_seq_id)) {
      CTHRIFT_LOG_WARN("i32_seq_id " << i32_seq_id
                                     << " up to int32 max value, reset to 1");
      g_atomic_i32_seq_id.getAndSet(1);
      i32_seq_id = 1;
    }

  }
  CTHRIFT_LOG_DEBUG("seq_id " << i32_seq_id);

  if (this->strict_write_) {
    int32_t version = (VERSION_1) | (static_cast<int32_t>(messageType));
    uint32_t wsize = 0;
    // 协助channel获取seq，从而避免内存copy开销
    wsize += writeI32(version);
    wsize += writeString(name);
    wsize += writeI32(i32_seq_id);

    wsize += writeI32(version);
    wsize += writeString(name);
    wsize += writeI32(i32_seq_id);
    return wsize;
  } else {
    uint32_t wsize = 0;
    // 协助channel获取seq，从而避免内存copy开销
    wsize += writeString(name);
    wsize += writeByte(static_cast<int8_t>(messageType));
    wsize += writeI32(i32_seq_id);

    wsize += writeString(name);
    wsize += writeByte(static_cast<int8_t>(messageType));
    wsize += writeI32(i32_seq_id);
    return wsize;
  }
}

template<>
uint32_t CthriftTBinaryProtocolT<CthriftTransport>::
writeMessageBegin(const std::string &name,
                  const TMessageType messageType,
                  const int32_t seqid) {
  CTHRIFT_LOG_DEBUG("writeMessageBegin");

  int32_t i32_seq_id = seqid;
  if (CTHRIFT_CLIENT == use_type_ && 0 == seqid) {
    i32_seq_id = g_atomic_i32_seq_id
        .incrementAndGet();

    if (CTHRIFT_UNLIKELY(std::numeric_limits<int32_t>::max() == i32_seq_id)) {
      CTHRIFT_LOG_WARN("i32_seq_id " << i32_seq_id <<
                                     " up to int32 max value, reset to 1");
      g_atomic_i32_seq_id.getAndSet(1);
      i32_seq_id = 1;
    }

    CTHRIFT_LOG_DEBUG("seq_id " << i32_seq_id);

    string str_id;
    try {
      str_id = boost::lexical_cast<std::string>(i32_seq_id);
    } catch (boost::bad_lexical_cast &e) {

      CTHRIFT_LOG_ERROR("boost::bad_lexical_cast :" << e.what()
                                                    << "i32_seq_id : "
                                                    << i32_seq_id);
    }

    boost::dynamic_pointer_cast<CthriftTransport>
        (getTransport())->ResetWriteBuf();
    CTHRIFT_LOG_DEBUG("clear write buf done");

    boost::dynamic_pointer_cast<CthriftTransport>
        (getTransport())->SetID2Transport(
        str_id); //Transport to CthriftTransport, CANNOT
    // use dynamic_cast
  }

  if (this->strict_write_) {
    int32_t version = (VERSION_1) | (static_cast<int32_t>(messageType));
    uint32_t wsize = 0;
    wsize += writeI32(version);
    wsize += writeString(name);
    wsize += writeI32(i32_seq_id);
    return wsize;
  } else {
    uint32_t wsize = 0;
    wsize += writeString(name);
    wsize += writeByte(static_cast<int8_t>(messageType));
    wsize += writeI32(i32_seq_id);
    return wsize;
  }
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeMessageBegin(const std::string &name,
                                                       const TMessageType messageType,
                                                       const int32_t seqid) {
  CTHRIFT_LOG_DEBUG("Transport_ writeMessageBegin");
  if (this->strict_write_) {
    int32_t version = (VERSION_1) | (static_cast<int32_t>(messageType));
    uint32_t wsize = 0;
    wsize += writeI32(version);
    wsize += writeString(name);
    wsize += writeI32(seqid);
    return wsize;
  } else {
    uint32_t wsize = 0;
    wsize += writeString(name);
    wsize += writeByte(static_cast<int8_t>(messageType));
    wsize += writeI32(seqid);
    return wsize;
  }
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeMessageEnd() {
  return 0;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeStructBegin(const char *name) {
  (void) name;
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeStructEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeFieldBegin(const char *name,
                                                              const TType fieldType,
                                                              const int16_t fieldId) {
  (void) name;
  uint32_t wsize = 0;
  wsize += writeByte(static_cast<int8_t>(fieldType));
  wsize += writeI16(fieldId);
  return wsize;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeFieldEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeFieldStop() {
  return
      writeByte(static_cast<int8_t>(T_STOP));
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeMapBegin(const TType keyType,
                                                            const TType valType,
                                                            const uint32_t size) {
  uint32_t wsize = 0;
  wsize += writeByte(static_cast<int8_t>(keyType));
  wsize += writeByte(static_cast<int8_t>(valType));
  wsize += writeI32(static_cast<int32_t>(size));
  return wsize;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeMapEnd() {
  return 0;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeListBegin(const TType elemType,
                                                    const uint32_t size) {
  uint32_t wsize = 0;
  wsize += writeByte(static_cast<int8_t>(elemType));
  wsize += writeI32(static_cast<int32_t>(size));
  return wsize;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeListEnd() {
  return 0;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeSetBegin(const TType elemType,
                                                   const uint32_t size) {
  uint32_t wsize = 0;
  wsize += writeByte(static_cast<int8_t>(elemType));
  wsize += writeI32(static_cast<int32_t>(size));
  return wsize;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeSetEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeBool(const bool value) {
  uint8_t tmp = value ? 1 : 0;
  this->trans_->write(&tmp, 1);
  return 1;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeByte(const int8_t byte) {
  uint8_t u_byte = static_cast<uint8_t>(byte);
  this->trans_->write(&u_byte, 1);
  return 1;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeI16(const int16_t i16) {
  int16_t net = static_cast<int16_t>((htons)(i16));
  this->trans_->write(reinterpret_cast<uint8_t * >(&net), 2);
  return 2;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeI32(const int32_t i32) {
  int32_t net = static_cast<int32_t>( htonl(i32));
  this->trans_->write(reinterpret_cast<uint8_t * >(&net), 4);
  return 4;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeI64(const int64_t i64) {
  int64_t net = static_cast<int64_t>( htonll(i64));
  this->trans_->write(reinterpret_cast<uint8_t * >(&net), 8);
  return 8;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::writeDouble(const double dub) {
  BOOST_STATIC_ASSERT(sizeof(double) == sizeof(uint64_t));
  BOOST_STATIC_ASSERT(std::numeric_limits<double>::is_iec559);

  uint64_t bits = bitwise_cast<uint64_t>(dub);
  bits = htonll(bits);
  this->trans_->write(reinterpret_cast<uint8_t * >(&bits), 8);
  return 8;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeString(const std::string &str) {
  uint32_t size = static_cast<uint32_t>(str.size());
  uint32_t result = writeI32(static_cast<int32_t>( size ));
  if (size > 0) {
    this->trans_->write(reinterpret_cast<const uint8_t * >( str.data()), size);
  }
  return result + size;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::writeBinary(const std::string &str) {
  return CthriftTBinaryProtocolT<Transport_>::writeString(str);
}

/**
 * Reading functions
 */

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::readMessageBegin(std::string &name,
                                                      TMessageType &messageType,
                                                      int32_t &seqid) {
  uint32_t result = 0;
  int32_t sz;
  result += readI32(sz);

  if (sz < 0) {
    // Check for correct version number
    int32_t version = sz & VERSION_MASK;
    if (version != VERSION_1) {
      throw TProtocolException(TProtocolException::BAD_VERSION,
                               "Bad version identifier");
    }
    messageType = static_cast<TMessageType>(sz & 0x000000ff);
    result += readString(name);
    result += readI32(seqid);
  } else {
    if (this->strict_read_) {
      throw TProtocolException(TProtocolException::BAD_VERSION,
                               "No version identifier... old protocol client in strict mode?");
    } else {
      // Handle pre-versioned input
      int8_t type;
      result += readStringBody(name, sz);
      result += readByte(type);
      messageType = static_cast<TMessageType>( type );
      result += readI32(seqid);
    }
  }
  return result;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readMessageEnd() {
  return 0;
}

template<class Transport_>
uint32_t
CthriftTBinaryProtocolT<Transport_>::readStructBegin(std::string &name) {
  name = "";
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readStructEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readFieldBegin(std::string &name,
                                                             TType &fieldType,
                                                             int16_t &fieldId) {
  (void) name;
  uint32_t result = 0;
  int8_t type;
  result += readByte(type);
  fieldType = static_cast<TType>( type );
  if (fieldType == T_STOP) {
    fieldId = 0;
    return result;
  }
  result += readI16(fieldId);
  return result;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readFieldEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readMapBegin(TType &keyType,
                                                           TType &valType,
                                                           uint32_t &size) {
  int8_t k, v;
  uint32_t result = 0;
  int32_t sizei;
  result += readByte(k);
  keyType = static_cast<TType>( k );
  result += readByte(v);
  valType = static_cast<TType>( v );
  result += readI32(sizei);
  if (sizei < 0) {
    throw TProtocolException(TProtocolException::NEGATIVE_SIZE);
  } else if (this->container_limit_ && sizei > this->container_limit_) {
    throw TProtocolException(TProtocolException::SIZE_LIMIT);
  }
  size = static_cast<uint32_t>( sizei );
  return result;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readMapEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readListBegin(TType &elemType,
                                                            uint32_t &size) {
  int8_t e;
  uint32_t result = 0;
  int32_t sizei;
  result += readByte(e);
  elemType = static_cast<TType>( e );
  result += readI32(sizei);
  if (sizei < 0) {
    throw TProtocolException(TProtocolException::NEGATIVE_SIZE);
  } else if (this->container_limit_ && sizei > this->container_limit_) {
    throw TProtocolException(TProtocolException::SIZE_LIMIT);
  }
  size = static_cast<uint32_t>( sizei );
  return result;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readListEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readSetBegin(TType &elemType,
                                                           uint32_t &size) {
  int8_t e;
  uint32_t result = 0;
  int32_t sizei;
  result += readByte(e);
  elemType = static_cast<TType>( e );
  result += readI32(sizei);
  if (sizei < 0) {
    throw TProtocolException(TProtocolException::NEGATIVE_SIZE);
  } else if (this->container_limit_ && sizei > this->container_limit_) {
    throw TProtocolException(TProtocolException::SIZE_LIMIT);
  }
  size = static_cast<uint32_t>( sizei );
  return result;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readSetEnd() {
  return 0;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readBool(bool &value) {
  uint8_t b[1];
  this->trans_->readAll(b, 1);
  value = *reinterpret_cast<int8_t *>(b) != 0;
  return 1;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readByte(int8_t &byte) {
  uint8_t b[1];
  this->trans_->readAll(b, 1);
  byte = *reinterpret_cast<int8_t *>(b);
  return 1;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readI16(int16_t &i16) {
  union bytes {
    uint8_t b[2];
    int16_t all;
  } theBytes;
  this->trans_->readAll(theBytes.b, 2);
  i16 = static_cast<int16_t>((ntohs)(theBytes.all));
  return 2;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readI32(int32_t &i32) {
  union bytes {
    uint8_t b[4];
    int32_t all;
  } theBytes;
  this->trans_->readAll(theBytes.b, 4);
  i32 = static_cast<int32_t>(ntohl(theBytes.all));
  return 4;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readI64(int64_t &i64) {
  union bytes {
    uint8_t b[8];
    int64_t all;
  } theBytes;
  this->trans_->readAll(theBytes.b, 8);
  i64 = static_cast<int64_t>(ntohll(theBytes.all));
  return 8;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readDouble(double &dub) {
  BOOST_STATIC_ASSERT(sizeof(double) == sizeof(uint64_t));
  BOOST_STATIC_ASSERT(std::numeric_limits<double>::is_iec559);

  union bytes {
    uint8_t b[8];
    uint64_t all;
  } theBytes;
  this->trans_->readAll(theBytes.b, 8);
  theBytes.all = ntohll(theBytes.all);
  dub = bitwise_cast<double>(theBytes.all);
  return 8;
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readString(std::string &str) {
  uint32_t result;
  int32_t size;
  result = readI32(size);
  return result + readStringBody(str, size);
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readBinary(std::string &str) {
  return CthriftTBinaryProtocolT<Transport_>::readString(str);
}

template<class Transport_>
uint32_t CthriftTBinaryProtocolT<Transport_>::readStringBody(std::string &str,
                                                             int32_t size) {
  uint32_t result = 0;

  // Catch error cases
  if (size < 0) {
    throw TProtocolException(TProtocolException::NEGATIVE_SIZE);
  }
  if (this->string_limit_ > 0 && size > this->string_limit_) {
    CTHRIFT_LOG_ERROR("throw  TProtocolException::SIZE_LIMIT : "
                          << this->string_limit_
                          << " read size:" << size);
    throw TProtocolException(TProtocolException::SIZE_LIMIT);
  }

  // Catch empty string case
  if (size == 0) {
    str = "";
    return result;
  }

  // Try to borrow first
  const uint8_t *borrow_buf;
  uint32_t got = size;
  if ((borrow_buf = this->trans_->borrow(NULL, &got))) {
    str.assign(reinterpret_cast<const char *>(borrow_buf), size);
    this->trans_->consume(size);
    return size;
  }

  // Use the heap here to prevent stack overflow for v. large strings
  if (size > this->string_buf_size_ || this->string_buf_ == NULL) {
    void *new_string_buf =
        std::realloc(this->string_buf_, static_cast<uint32_t>( size ));
    if (new_string_buf == NULL) {
      throw std::bad_alloc();
    }
    this->string_buf_ = static_cast<uint8_t *>( new_string_buf );
    this->string_buf_size_ = size;
  }
  this->trans_->readAll(this->string_buf_, size);
  str = std::string(reinterpret_cast<char *>( this->string_buf_ ), size);
  return static_cast<uint32_t>( size );
}
}  // namespace meituan_cthrift

#endif
