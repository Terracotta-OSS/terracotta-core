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

import com.tc.classloader.ServiceLocator;
import com.tc.util.ZipBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

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
     zip.putEntry("META-INF/services/org.terracotta.config.service.ServiceConfigParser", "com.tc.server.TestServiceConfigParser".getBytes());
     zip.putEntry("com/tc/server/TestService.class", resourceToBytes("com/tc/server/TestService.class"));
     zip.putEntry("com/tc/server/TestServiceConfigParser.class", resourceToBytes("com/tc/server/TestServiceConfigParser.class"));
     zip.putEntry("com/tc/server/TestServiceProvider.class", resourceToBytes("com/tc/server/TestServiceProvider.class"));
     zip.putEntry("com/tc/server/TestServiceProviderConfiguration.class", resourceToBytes("com/tc/server/TestServiceProviderConfiguration.class"));
//  put it in the zip so null parent loader can find it
     zip.finish();
     List<Class<? extends ServiceConfigParser>> list = ServiceLocator.getImplementations(ServiceConfigParser.class, new URLClassLoader(new URL[] {test.toURI().toURL()}));
     // XXX: depending on the other resources are on the classpath, it is possible that we will see other parsers.  We
     //  should figure out a better way to restrict this since the index otherwise needs to be manually updated when new
     //  resources change the order of the ServiceConfigParser instances in the list.
     int listIndexToTest = 0;
     ClassLoader baseLoader = new ServiceClassLoader(list);
     Class<? extends ServiceConfigParser> check = baseLoader.loadClass("com.tc.server.TestServiceConfigParser").asSubclass(ServiceConfigParser.class);
     ServiceConfigParser parser = check.newInstance();
     Assert.assertTrue(parser.getClass().getClassLoader() != ClassLoader.getSystemClassLoader());
     Assert.assertTrue(parser.getClass().getClassLoader() == list.get(listIndexToTest).getClassLoader());
     ServiceProviderConfiguration config = parser.parse(null, null);
     Assert.assertTrue(config.getClass().getClassLoader() != ClassLoader.getSystemClassLoader());
     Assert.assertTrue(config.getClass().getClassLoader() == list.get(listIndexToTest).getClassLoader());
     Class<? extends ServiceProvider> provider = config.getServiceProviderType();
     Assert.assertTrue(provider.getClassLoader() != ClassLoader.getSystemClassLoader());
     Assert.assertTrue(provider.getClassLoader() == list.get(listIndexToTest).getClassLoader());
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
