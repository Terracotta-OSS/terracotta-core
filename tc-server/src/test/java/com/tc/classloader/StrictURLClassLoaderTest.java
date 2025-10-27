/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.utilities.test.runtime.Os;

/**
 *
 */
public class StrictURLClassLoaderTest {

  public StrictURLClassLoaderTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of loadClass method, of class StrictURLClassLoader.
   */
  @Test
  public void testLoadClassWithAnnotations() throws Exception {
    String cp = System.getProperty("java.class.path");
    String sc = Os.isWindows() ? ";" : ":";
    String[] split = cp.split(sc);
    StrictURLClassLoader instance = new StrictURLClassLoader(Arrays.stream(split).map(StrictURLClassLoaderTest::convert).toArray(URL[]::new),null,new AnnotationBasedCommonComponentChecker());
    try {
      instance.loadClass("org.terracotta.config.Config");
    } catch (ClassNotFoundException cla) {
      // expected
    } catch (TypeNotPresentException type) {
      Assert.fail(type.getMessage() + " not expected");
    }
  }

  private static URL convert(String url) {
    try {
      return Path.of(url).toUri().toURL();
    } catch (MalformedURLException l) {
      return null;
    }
  }
}
