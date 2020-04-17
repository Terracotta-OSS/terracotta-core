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

import com.tc.util.ZipBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author mscott
 */
public class ComponentURLClassLoaderTest {
  
  @Rule
  public TemporaryFolder folder= new TemporaryFolder();
  
  public ComponentURLClassLoaderTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() throws Exception {

  }
  
  @After
  public void tearDown() {
  }
  
  @Test
  public void testCommonComponent() throws Exception {
    CommonComponentChecker checker = mock(CommonComponentChecker.class);
    when(checker.check(any(Class.class))).then(a->{
      return ((Class)a.getArgument(0)).getName().endsWith("CommonComponentClass");
    });
    StrictURLClassLoader strict = new StrictURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, null, checker, true);
    ClassLoader loader = new ComponentURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, strict, checker);
    Class<?> commonClass = loader.loadClass("com.tc.classloader.CommonComponentClass");
    assertEquals(commonClass.getClassLoader(), strict);
    Class<?> specificClass = loader.loadClass("com.tc.classloader.SpecificComponentClass");
    assertEquals(specificClass.getClassLoader(), loader);
  }
  
  @Test @Ignore("test is no longer relevant")
  public void testClassCaching() throws Exception {
    CommonComponentChecker checker = mock(CommonComponentChecker.class);
    when(checker.check(any(Class.class))).then(a->{
      return ((Class)a.getArgument(0)).getName().endsWith("CommonComponentClass");
    });
    StrictURLClassLoader strict = new StrictURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, null, checker, true);
    ClassLoader loader = new ComponentURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, strict, checker);
    Class<?> commonClass = loader.loadClass("com.tc.classloader.CommonComponentClass");
    assertTrue(commonClass == loader.loadClass("com.tc.classloader.CommonComponentClass"));

    assertEquals(commonClass.getClassLoader(), strict);
    Class<?> specificClass = loader.loadClass("com.tc.classloader.SpecificComponentClass");
    assertTrue(specificClass == loader.loadClass("com.tc.classloader.SpecificComponentClass"));

    assertEquals(specificClass.getClassLoader(), loader);
  }
  
  private static class ExposedClassLoader extends URLClassLoader {

    public ExposedClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }
    
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      return super.loadClass(name, resolve);
    }
  }


   private File writeZip(File api, String...classes) throws IOException {
     ZipBuilder builder = new ZipBuilder(api, true);
     for (String className : classes) {
       className = className.replace('.', '/');
       builder.putEntry(className + ".class", resourceToBytes(className + ".class"));
     }
     builder.finish();
     return api;
   }

   private byte[] resourceToBytes(String loc) throws IOException {
     ByteArrayOutputStream fos = new ByteArrayOutputStream();
     InputStream implb = getClass().getClassLoader().getResourceAsStream(loc);
     int check = implb.read();
     while (check >= 0) {
       fos.write(check);
       check = implb.read();
     }
     fos.close();
     return fos.toByteArray();
   }
}
