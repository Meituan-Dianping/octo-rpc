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
package com.meituan.dorado.common.extension;

import com.meituan.dorado.common.RpcRole;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ExtensionLoaderTest {

    @Test
    public void testGetExtension() {
        Extension extension = ExtensionLoader.getExtension(Extension.class);
        Assert.assertNotNull(extension);
    }

    @Test
    public void testGetExtensionList() {
        List<Extension> extensionList = ExtensionLoader.getExtensionList(Extension.class);
        for (Extension extension : extensionList) {
            Assert.assertTrue(extension instanceof FirstExtension || extension instanceof SecondExtension);
        }
    }

    @Test
    public void testGetExtensionWithName() {
        Extension extension = ExtensionLoader.getExtensionWithName(Extension.class, "First");
        Assert.assertTrue(extension instanceof FirstExtension);
    }

    @Test
    public void testGetExtensionByRole() {
        Extension extension = ExtensionLoader.getExtensionByRole(Extension.class, RpcRole.INVOKER);
        Assert.assertTrue(extension instanceof SecondExtension);
    }

    @Test
    public void testGetExtensionListByRole() {
        List<Extension> extensionList = ExtensionLoader.getExtensionListByRole(Extension.class, RpcRole.INVOKER);
        Assert.assertTrue(extensionList.get(0) instanceof SecondExtension);
    }
}
