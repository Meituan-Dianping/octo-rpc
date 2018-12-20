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

package com.meituan.dorado.registry.meta;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class RegistryInfoTest {

    @Test
    public void testHashCode() {
        RegistryInfo info1 = new RegistryInfo();
        RegistryInfo info2 = new RegistryInfo();

        Assert.assertEquals(info1, info2);
        HashSet set = new HashSet();
        set.add(info1);
        set.add(info2);
        Assert.assertEquals(1, set.size());
        Assert.assertTrue(set.contains(info1));

        info2.setEnv("prod");
        set.add(info2);
        Assert.assertEquals(info1, info2);
        Assert.assertTrue(set.contains(info1));
    }
}