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

import java.util.List;
import java.util.Objects;

@ThriftStruct
public class TweetSearchResult {

    private List<Tweet> tweets;

    @ThriftConstructor
    public TweetSearchResult(List<Tweet> tweets) {
        this.tweets = tweets;
    }

    @ThriftField(1)
    public List<Tweet> getTweets() {
        return tweets;
    }

    @ThriftField
    public void setTweets(List<Tweet> tweets) {
        this.tweets = tweets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TweetSearchResult that = (TweetSearchResult) o;
        return Objects.equals(tweets, that.tweets);
    }
}