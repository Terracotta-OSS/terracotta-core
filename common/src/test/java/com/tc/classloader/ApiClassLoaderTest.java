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
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { implJar }, Thread.currentThread().getContextClassLoader());
        ApiClassLoader apiClassLoader = new ApiClassLoader(new URL[] { apiJar }, urlClassLoader);
        Class<?> apiClass = apiClassLoader.loadClass("com.terracotta.test.api.MyService", false);
        Assert.assertEquals(apiClass.getClassLoader(), apiClassLoader);
        Class<?> nonApiClass = apiClassLoader.loadClass("com.terracotta.test.impl.SomeNonApiClass", false);
        Assert.assertEquals(nonApiClass.getClassLoader(), urlClassLoader);
    }

}