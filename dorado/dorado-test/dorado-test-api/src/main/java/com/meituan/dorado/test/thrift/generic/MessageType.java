
package com.meituan.dorado.test.thrift.generic;

import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum MessageType implements TEnum {
    TWEET(0),
    RETWEET(2),
    DM(10),
    REPLY(11);

    private final int value;

    private MessageType(int value) {
        this.value = value;
    }

    /**
     * Get the integer value of this enum value, as defined in the Thrift IDL.
     */
    public int getValue() {
        return value;
    }

    /**
     * Find a the enum type by its integer value, as defined in the Thrift IDL.
     * @return null if the value is not found.
     */
    public static MessageType findByValue(int value) {
        switch (value) {
            case 0:
                return TWEET;
            case 2:
                return RETWEET;
            case 10:
                return DM;
            case 11:
                return REPLY;
            default:
                return null;
        }
    }
}