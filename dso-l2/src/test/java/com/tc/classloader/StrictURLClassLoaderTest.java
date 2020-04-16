/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
    System.out.println("loadClass");
    String name = "";
    boolean resolve = false;
    URLClassLoader loader = (URLClassLoader)this.getClass().getClassLoader();
    URL[] urls = loader.getURLs();
    StrictURLClassLoader instance = new StrictURLClassLoader(urls,null,new AnnotationBasedCommonComponentChecker(),true);
    try {
      instance.loadClass("org.terracotta.config.Config");
    } catch (ClassNotFoundException cla) {
      // expected
    } catch (TypeNotPresentException type) {
      Assert.fail(type.getMessage() + " not expected");
    }
  }
}
