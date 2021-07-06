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
package com.meituan.dorado.test.thrift.annotationTwitter;

import com.facebook.swift.codec.ThriftConstructor;
import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct
public class Tweet {

    private int userId;
    private String userName;
    private String text;

    private Location loc;
    private TweetType tweetType;
    private int age;

    @ThriftConstructor
    public Tweet(int userId, String userName, String text) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;

        this.tweetType = TweetType.TWEET;
        this.age = Constants.DEFAULT_AGE;
    }

    public Tweet() {

    }

    @ThriftField(1)
    public int getUserId() {
        return userId;
    }

    @ThriftField
    public void setUserId(int userId) {
        this.userId = userId;
    }

    @ThriftField(2)
    public String getUserName() {
        return userName;
    }

    @ThriftField
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @ThriftField(3)
    public String getText() {
        return text;
    }

    @ThriftField
    public void setText(String text) {
        this.text = text;
    }

    @ThriftField(4)
    public Location getLoc() {
        return loc;
    }

    @ThriftField
    public void setLoc(Location loc) {
        this.loc = loc;
    }

    @ThriftField(5)
    public TweetType getTweetType() {
        return tweetType;
    }

    @ThriftField
    public void setTweetType(TweetType tweetType) {
        this.tweetType = tweetType;
    }

    @ThriftField(16)
    public int getAge() {
        return age;
    }

    @ThriftField
    public void setAge(int age) {
        this.age = age;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else {
            if (obj instanceof Tweet) {
                Tweet tweet = (Tweet) obj;
                if (tweet.getUserId() == this.getUserId() && tweet.getUserName().equals(this.getUserName())
                        && tweet.getText().equals(this.getText())) {
                    return true;
                }
            }
        }
        return false;
    }
}