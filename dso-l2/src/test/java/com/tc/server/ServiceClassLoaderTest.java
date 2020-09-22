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
package com.tc.server;

import com.tc.util.ZipBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mscott
 */
public class ServiceClassLoaderTest {
  
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  public ServiceClassLoaderTest() {
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

   @Test
   public void testServiceLoading() throws Exception {
     File base = folder.newFolder();
     File test = new File(base, "test.jar");
     ZipBuilder zip = new ZipBuilder(test, false);
     zip.putEntry("META-INF/services/com.tc.server.TestInterface", "com.tc.server.TestInterfaceImpl".getBytes());
     zip.putEntry("com/tc/server/TestInterfaceImpl.class", resourceToBytes("com/tc/server/TestInterfaceImpl.class"));
     zip.putEntry("com/tc/server/TestChild.class", resourceToBytes("com/tc/server/TestChild.class"));
//  put it in the zip so null parent loader can find it
     zip.finish();
     // copy to remove dynamic nature of class building in the loader
     System.out.println(TestInterface.class.getClassLoader());
     // XXX: depending on the other resources are on the classpath, it is possible that we will see other parsers.  We
     //  should figure out a better way to restrict this since the index otherwise needs to be manually updated when new
     //  resources change the order of the ServiceConfigParser instances in the list.
     int listIndexToTest = 0;
     URLClassLoader special = new URLClassLoader(new URL[] {test.toURI().toURL()}, ClassLoader.getSystemClassLoader()) {
       @Override
       protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
         if (name.startsWith("com/tc/server/Test")) {
           return findClass(name);
         } else {
           return super.loadClass(name, resolve);
         }
       }
     };
     ClassLoader baseLoader = new ServiceClassLoader(special, TestInterface.class);
     ServiceLoader<TestInterface>  serviceList = ServiceLoader.load(TestInterface.class, baseLoader);
     Class<? extends TestInterface> check = baseLoader.loadClass("com.tc.server.TestInterfaceImpl").asSubclass(TestInterface.class);
     TestInterface parser = check.newInstance();
     Iterator<TestInterface> i = serviceList.iterator();
     TestInterface ref = i.next();
     
     Assert.assertTrue(parser.getClass().getClassLoader() != this.getClass().getClassLoader());
     Assert.assertTrue(parser.getClass().getClassLoader() == ref.getClass().getClassLoader());
     Object config = parser.child();
     Assert.assertTrue(config.getClass().getClassLoader() != this.getClass().getClassLoader());
     Assert.assertTrue(config.getClass().getClassLoader() == ref.getClass().getClassLoader());
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
