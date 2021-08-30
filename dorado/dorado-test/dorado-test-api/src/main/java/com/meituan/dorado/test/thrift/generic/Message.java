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
package com.meituan.dorado.test.thrift.generic;

import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Message implements org.apache.thrift.TBase<Message, Message._Fields>, java.io.Serializable, Cloneable {
    public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Message");
    private static final org.apache.thrift.protocol.TField ID_FIELD_DESC = new org.apache.thrift.protocol.TField("id", org.apache.thrift.protocol.TType.I64, (short)1);
    private static final org.apache.thrift.protocol.TField VALUE_FIELD_DESC = new org.apache.thrift.protocol.TField("value", org.apache.thrift.protocol.TType.STRING, (short)2);
    private static final org.apache.thrift.protocol.TField SUB_MESSAGES_FIELD_DESC = new org.apache.thrift.protocol.TField("subMessages", org.apache.thrift.protocol.TType.LIST, (short)3);
    private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
    // isset id assignments
    private static final int __ID_ISSET_ID = 0;

    static {
        schemes.put(StandardScheme.class, new MessageStandardSchemeFactory());
        schemes.put(TupleScheme.class, new MessageTupleSchemeFactory());
    }

    static {
        Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
        tmpMap.put(_Fields.ID, new org.apache.thrift.meta_data.FieldMetaData("id", org.apache.thrift.TFieldRequirementType.OPTIONAL,
                new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
        tmpMap.put(_Fields.VALUE, new org.apache.thrift.meta_data.FieldMetaData("value", org.apache.thrift.TFieldRequirementType.OPTIONAL,
                new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
        tmpMap.put(_Fields.SUB_MESSAGES, new org.apache.thrift.meta_data.FieldMetaData("subMessages", org.apache.thrift.TFieldRequirementType.OPTIONAL,
                new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST,
                        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, SubMessage.class))));
        metaDataMap = Collections.unmodifiableMap(tmpMap);
        org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Message.class, metaDataMap);
    }

    public long id; // optional
    public String value; // optional
    public List<SubMessage> subMessages; // optional
    private BitSet __isset_bit_vector = new BitSet(1);
    private _Fields optionals[] = {_Fields.ID,_Fields.VALUE,_Fields.SUB_MESSAGES};
    public Message() {
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public Message(Message other) {
        __isset_bit_vector.clear();
        __isset_bit_vector.or(other.__isset_bit_vector);
        this.id = other.id;
        if (other.isSetValue()) {
            this.value = other.value;
        }
        if (other.isSetSubMessages()) {
            List<SubMessage> __this__subMessages = new ArrayList<SubMessage>();
            for (SubMessage other_element : other.subMessages) {
                __this__subMessages.add(new SubMessage(other_element));
            }
            this.subMessages = __this__subMessages;
        }
    }

    public Message deepCopy() {
        return new Message(this);
    }

    @Override
    public void clear() {
        setIdIsSet(false);
        this.id = 0;
        this.value = null;
        this.subMessages = null;
    }

    public long getId() {
        return this.id;
    }

    public Message setId(long id) {
        this.id = id;
        setIdIsSet(true);
        return this;
    }

    public void unsetId() {
        __isset_bit_vector.clear(__ID_ISSET_ID);
    }

    /** Returns true if field id is set (has been assigned a value) and false otherwise */
    public boolean isSetId() {
        return __isset_bit_vector.get(__ID_ISSET_ID);
    }

    public void setIdIsSet(boolean value) {
        __isset_bit_vector.set(__ID_ISSET_ID, value);
    }

    public String getValue() {
        return this.value;
    }

    public Message setValue(String value) {
        this.value = value;
        return this;
    }

    public void unsetValue() {
        this.value = null;
    }

    /** Returns true if field value is set (has been assigned a value) and false otherwise */
    public boolean isSetValue() {
        return this.value != null;
    }

    public void setValueIsSet(boolean value) {
        if (!value) {
            this.value = null;
        }
    }

    public int getSubMessagesSize() {
        return (this.subMessages == null) ? 0 : this.subMessages.size();
    }

    public java.util.Iterator<SubMessage> getSubMessagesIterator() {
        return (this.subMessages == null) ? null : this.subMessages.iterator();
    }

    public void addToSubMessages(SubMessage elem) {
        if (this.subMessages == null) {
            this.subMessages = new ArrayList<SubMessage>();
        }
        this.subMessages.add(elem);
    }

    public List<SubMessage> getSubMessages() {
        return this.subMessages;
    }

    public Message setSubMessages(List<SubMessage> subMessages) {
        this.subMessages = subMessages;
        return this;
    }

    public void unsetSubMessages() {
        this.subMessages = null;
    }

    /** Returns true if field subMessages is set (has been assigned a value) and false otherwise */
    public boolean isSetSubMessages() {
        return this.subMessages != null;
    }

    public void setSubMessagesIsSet(boolean value) {
        if (!value) {
            this.subMessages = null;
        }
    }

    public void setFieldValue(_Fields field, Object value) {
        switch (field) {
            case ID:
                if (value == null) {
                    unsetId();
                } else {
                    setId((Long)value);
                }
                break;

            case VALUE:
                if (value == null) {
                    unsetValue();
                } else {
                    setValue((String)value);
                }
                break;

            case SUB_MESSAGES:
                if (value == null) {
                    unsetSubMessages();
                } else {
                    setSubMessages((List<SubMessage>)value);
                }
                break;

        }
    }

    public Object getFieldValue(_Fields field) {
        switch (field) {
            case ID:
                return Long.valueOf(getId());

            case VALUE:
                return getValue();

            case SUB_MESSAGES:
                return getSubMessages();

        }
        throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
        if (field == null) {
            throw new IllegalArgumentException();
        }

        switch (field) {
            case ID:
                return isSetId();
            case VALUE:
                return isSetValue();
            case SUB_MESSAGES:
                return isSetSubMessages();
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
        if (that == null)
            return false;
        if (that instanceof Message)
            return this.equals((Message)that);
        return false;
    }

    public boolean equals(Message that) {
        if (that == null)
            return false;

        boolean this_present_id = true && this.isSetId();
        boolean that_present_id = true && that.isSetId();
        if (this_present_id || that_present_id) {
            if (!(this_present_id && that_present_id))
                return false;
            if (this.id != that.id)
                return false;
        }

        boolean this_present_value = true && this.isSetValue();
        boolean that_present_value = true && that.isSetValue();
        if (this_present_value || that_present_value) {
            if (!(this_present_value && that_present_value))
                return false;
            if (!this.value.equals(that.value))
                return false;
        }

        boolean this_present_subMessages = true && this.isSetSubMessages();
        boolean that_present_subMessages = true && that.isSetSubMessages();
        if (this_present_subMessages || that_present_subMessages) {
            if (!(this_present_subMessages && that_present_subMessages))
                return false;
            if (!this.subMessages.equals(that.subMessages))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public int compareTo(Message other) {
        if (!getClass().equals(other.getClass())) {
            return getClass().getName().compareTo(other.getClass().getName());
        }

        int lastComparison = 0;
        Message typedOther = (Message)other;

        lastComparison = Boolean.valueOf(isSetId()).compareTo(typedOther.isSetId());
        if (lastComparison != 0) {
            return lastComparison;
        }
        if (isSetId()) {
            lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.id, typedOther.id);
            if (lastComparison != 0) {
                return lastComparison;
            }
        }
        lastComparison = Boolean.valueOf(isSetValue()).compareTo(typedOther.isSetValue());
        if (lastComparison != 0) {
            return lastComparison;
        }
        if (isSetValue()) {
            lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.value, typedOther.value);
            if (lastComparison != 0) {
                return lastComparison;
            }
        }
        lastComparison = Boolean.valueOf(isSetSubMessages()).compareTo(typedOther.isSetSubMessages());
        if (lastComparison != 0) {
            return lastComparison;
        }
        if (isSetSubMessages()) {
            lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.subMessages, typedOther.subMessages);
            if (lastComparison != 0) {
                return lastComparison;
            }
        }
        return 0;
    }

    public _Fields fieldForId(int fieldId) {
        return _Fields.findByThriftId(fieldId);
    }

    public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
        schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
        schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Message(");
        boolean first = true;

        if (isSetId()) {
            sb.append("id:");
            sb.append(this.id);
            first = false;
        }
        if (isSetValue()) {
            if (!first) sb.append(", ");
            sb.append("value:");
            if (this.value == null) {
                sb.append("null");
            } else {
                sb.append(this.value);
            }
            first = false;
        }
        if (isSetSubMessages()) {
            if (!first) sb.append(", ");
            sb.append("subMessages:");
            if (this.subMessages == null) {
                sb.append("null");
            } else {
                sb.append(this.subMessages);
            }
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    public void validate() throws org.apache.thrift.TException {
        // check for required fields
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        try {
            write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
        } catch (org.apache.thrift.TException te) {
            throw new java.io.IOException(te);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        try {
            // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
            __isset_bit_vector = new BitSet(1);
            read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
        } catch (org.apache.thrift.TException te) {
            throw new java.io.IOException(te);
        }
    }

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
        ID((short)1, "id"),
        VALUE((short)2, "value"),
        SUB_MESSAGES((short)3, "subMessages");

        private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

        static {
            for (_Fields field : EnumSet.allOf(_Fields.class)) {
                byName.put(field.getFieldName(), field);
            }
        }

        private final short _thriftId;
        private final String _fieldName;

        _Fields(short thriftId, String fieldName) {
            _thriftId = thriftId;
            _fieldName = fieldName;
        }

        /**
         * Find the _Fields constant that matches fieldId, or null if its not found.
         */
        public static _Fields findByThriftId(int fieldId) {
            switch(fieldId) {
                case 1: // ID
                    return ID;
                case 2: // VALUE
                    return VALUE;
                case 3: // SUB_MESSAGES
                    return SUB_MESSAGES;
                default:
                    return null;
            }
        }

        /**
         * Find the _Fields constant that matches fieldId, throwing an exception
         * if it is not found.
         */
        public static _Fields findByThriftIdOrThrow(int fieldId) {
            _Fields fields = findByThriftId(fieldId);
            if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            return fields;
        }

        /**
         * Find the _Fields constant that matches name, or null if its not found.
         */
        public static _Fields findByName(String name) {
            return byName.get(name);
        }

        public short getThriftFieldId() {
            return _thriftId;
        }

        public String getFieldName() {
            return _fieldName;
        }
    }

    private static class MessageStandardSchemeFactory implements SchemeFactory {
        public MessageStandardScheme getScheme() {
            return new MessageStandardScheme();
        }
    }

    private static class MessageStandardScheme extends StandardScheme<Message> {

        public void read(org.apache.thrift.protocol.TProtocol iprot, Message struct) throws org.apache.thrift.TException {
            org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();
            while (true)
            {
                schemeField = iprot.readFieldBegin();
                if (schemeField.type == org.apache.thrift.protocol.TType.STOP) {
                    break;
                }
                switch (schemeField.id) {
                    case 1: // ID
                        if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
                            struct.id = iprot.readI64();
                            struct.setIdIsSet(true);
                        } else {
                            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                    case 2: // VALUE
                        if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                            struct.value = iprot.readString();
                            struct.setValueIsSet(true);
                        } else {
                            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                    case 3: // SUB_MESSAGES
                        if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
                            {
                                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                                struct.subMessages = new ArrayList<SubMessage>(_list0.size);
                                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                                {
                                    SubMessage _elem2; // required
                                    _elem2 = new SubMessage();
                                    _elem2.read(iprot);
                                    struct.subMessages.add(_elem2);
                                }
                                iprot.readListEnd();
                            }
                            struct.setSubMessagesIsSet(true);
                        } else {
                            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                        }
                        break;
                    default:
                        org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                iprot.readFieldEnd();
            }
            iprot.readStructEnd();

            // check for required fields of primitive type, which can't be checked in the validate method
            struct.validate();
        }

        public void write(org.apache.thrift.protocol.TProtocol oprot, Message struct) throws org.apache.thrift.TException {
            struct.validate();

            oprot.writeStructBegin(STRUCT_DESC);
            if (struct.isSetId()) {
                oprot.writeFieldBegin(ID_FIELD_DESC);
                oprot.writeI64(struct.id);
                oprot.writeFieldEnd();
            }
            if (struct.value != null) {
                if (struct.isSetValue()) {
                    oprot.writeFieldBegin(VALUE_FIELD_DESC);
                    oprot.writeString(struct.value);
                    oprot.writeFieldEnd();
                }
            }
            if (struct.subMessages != null) {
                if (struct.isSetSubMessages()) {
                    oprot.writeFieldBegin(SUB_MESSAGES_FIELD_DESC);
                    {
                        oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.subMessages.size()));
                        for (SubMessage _iter3 : struct.subMessages)
                        {
                            _iter3.write(oprot);
                        }
                        oprot.writeListEnd();
                    }
                    oprot.writeFieldEnd();
                }
            }
            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }

    }

    private static class MessageTupleSchemeFactory implements SchemeFactory {
        public MessageTupleScheme getScheme() {
            return new MessageTupleScheme();
        }
    }

    private static class MessageTupleScheme extends TupleScheme<Message> {

        @Override
        public void write(org.apache.thrift.protocol.TProtocol prot, Message struct) throws org.apache.thrift.TException {
            TTupleProtocol oprot = (TTupleProtocol) prot;
            BitSet optionals = new BitSet();
            if (struct.isSetId()) {
                optionals.set(0);
            }
            if (struct.isSetValue()) {
                optionals.set(1);
            }
            if (struct.isSetSubMessages()) {
                optionals.set(2);
            }
            oprot.writeBitSet(optionals, 3);
            if (struct.isSetId()) {
                oprot.writeI64(struct.id);
            }
            if (struct.isSetValue()) {
                oprot.writeString(struct.value);
            }
            if (struct.isSetSubMessages()) {
                {
                    oprot.writeI32(struct.subMessages.size());
                    for (SubMessage _iter4 : struct.subMessages)
                    {
                        _iter4.write(oprot);
                    }
                }
            }
        }

        @Override
        public void read(org.apache.thrift.protocol.TProtocol prot, Message struct) throws org.apache.thrift.TException {
            TTupleProtocol iprot = (TTupleProtocol) prot;
            BitSet incoming = iprot.readBitSet(3);
            if (incoming.get(0)) {
                struct.id = iprot.readI64();
                struct.setIdIsSet(true);
            }
            if (incoming.get(1)) {
                struct.value = iprot.readString();
                struct.setValueIsSet(true);
            }
            if (incoming.get(2)) {
                {
                    org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
                    struct.subMessages = new ArrayList<SubMessage>(_list5.size);
                    for (int _i6 = 0; _i6 < _list5.size; ++_i6)
                    {
                        SubMessage _elem7; // required
                        _elem7 = new SubMessage();
                        _elem7.read(iprot);
                        struct.subMessages.add(_elem7);
                    }
                }
                struct.setSubMessagesIsSet(true);
            }
        }
    }

}
