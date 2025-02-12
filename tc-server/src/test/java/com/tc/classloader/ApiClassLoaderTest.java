/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.classloader;

import com.tc.util.Assert;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author vmad
 */
public class ApiClassLoaderTest {

    private static final URL apiJar = ApiClassLoaderTest.class.getClassLoader().getResource("classloading-test-jars/test-api.jar");
    private static final URL implJar = ApiClassLoaderTest.class.getClassLoader().getResource("classloading-test-jars/test-impl.jar");

    @Test
    public void testLoadClass() throws Exception {
        ApiClassLoader apiClassLoader = new ApiClassLoader(new URL[] { apiJar }, Thread.currentThread().getContextClassLoader());
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { implJar }, apiClassLoader);
        Class<?> apiClass = Class.forName("com.terracotta.test.api.MyService", true, urlClassLoader);
        Assert.assertEquals(apiClass.getClassLoader(), apiClassLoader);
        Class<?> nonApiClass = Class.forName("com.terracotta.test.impl.SomeNonApiClass", true, urlClassLoader);
        Assert.assertEquals(nonApiClass.getClassLoader(), urlClassLoader);
    }

}