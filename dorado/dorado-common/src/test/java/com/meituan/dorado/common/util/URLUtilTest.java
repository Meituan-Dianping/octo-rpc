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

public class URLUtilTest {

    @Test
    public void testGetURIPath() {
        String uri = URLUtil.getURIPath("http://abcd.com.cn?a=v");
        Assert.assertEquals(uri, "http://abcd.com.cn");
    }

    @Test
    public void testGetURIParameter() {
        Map<String, String> params = new HashMap<>();
        String path = URLUtil.getURIPathAndParameter("http://abcd.com.cn?name=emma&age=18", params);
        Map<String, String> result = new HashMap<>();
        result.put("name", "emma");
        result.put("age", "18");
        Assert.assertEquals(params, result);

        params.clear();
        path = URLUtil.getURIPathAndParameter("http://abcd.com.cn?serviceName=com.Twitter&method=echo", params);
        result = new HashMap<>();
        result.put("serviceName", "com.Twitter");
        result.put("method", "echo");
        Assert.assertEquals(params, result);
    }
}
