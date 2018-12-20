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
package com.meituan.dorado.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CommonUtilTest {

    @Test
    public void testParseUrlParams() {

        String emptyUrl = "";
        Map<String, String> params = CommonUtil.parseUrlParams(emptyUrl);
        Assert.assertEquals(params, new HashMap<String, String>());

        String noParamsUrl = "123456";
        params = CommonUtil.parseUrlParams(noParamsUrl);
        Assert.assertEquals(params, new HashMap<String, String>());

        String url = "abc&a=1&b=3";
        params = CommonUtil.parseUrlParams(url);
        Map<String, String> result = new HashMap<>();
        result.put("a", "1");
        result.put("b", "3");
        Assert.assertEquals(params, result);
    }
}
