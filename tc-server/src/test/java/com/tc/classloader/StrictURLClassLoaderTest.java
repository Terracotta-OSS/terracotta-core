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

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
@Ignore("doesn't work after JDK 8")
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
    System.out.println("loadClass");
    String name = "";
    boolean resolve = false;
    URLClassLoader loader = (URLClassLoader)this.getClass().getClassLoader();
    URL[] urls = loader.getURLs();
    StrictURLClassLoader instance = new StrictURLClassLoader(urls,null,new AnnotationBasedCommonComponentChecker());
    try {
      instance.loadClass("org.terracotta.config.Config");
    } catch (ClassNotFoundException cla) {
      // expected
    } catch (TypeNotPresentException type) {
      Assert.fail(type.getMessage() + " not expected");
    }
  }
}
