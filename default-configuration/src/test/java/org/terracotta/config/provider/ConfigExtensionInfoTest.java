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
 *
 */
package org.terracotta.config.provider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.ServiceLoader;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ConfigExtensionInfoTest {

  public ConfigExtensionInfoTest() {
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
   * Test of getExtensionInfo method, of class ConfigExtensionInfo.
   */
  @Test
  public void testGetExtensionInfo() throws Exception {
    System.out.println(new ConfigExtensionInfo());
    Enumeration<URL> u = getClass().getClassLoader().getResources("META-INF/services/com.tc.productinfo.Description");
    while (u.hasMoreElements()) {
      URL n = u.nextElement();
      System.out.println(n);
      LineNumberReader r = new LineNumberReader(new InputStreamReader(n.openStream()));
      String line = r.readLine();
      while (line != null) {
        System.out.println(Class.forName(line));
        line = r.readLine();
      }
    }
    ServiceLoader<com.tc.productinfo.Description> items = ServiceLoader.load(com.tc.productinfo.Description.class, getClass().getClassLoader());
    for (com.tc.productinfo.Description d : items) {
      System.out.println(d);
    }
  }
}
