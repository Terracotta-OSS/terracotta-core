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
package com.tc.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.spy;

/**
 *
 * @author mscott
 */
public class ManagedServiceLoaderTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  public ManagedServiceLoaderTest() {
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
  public void testServiceLoad() {
    ManagedServiceLoader loader = new ManagedServiceLoader();
    List<Class<? extends TestService>> services = loader.getImplementationsTypes(TestService.class, Thread.currentThread().getContextClassLoader());
    Assert.assertEquals(1, services.size());
    Assert.assertEquals(TestServiceImpl.class, services.get(0));
  }
  
  @Test
  public void testServiceLoaderBootstrap() throws Throwable {
    Collection<? extends TestService> services = TCServiceLoader.loadServices(TestService.class);
    Field f = TCServiceLoader.class.getDeclaredField("IMPL");
    f.setAccessible(true);
    String name = f.get(TCServiceLoader.class).getClass().getName();
    Assert.assertTrue(name.contains("ManagedServiceLoader"));
  }

  @Test
  public void testOverrideAnnotation() throws Throwable {
    File f = folder.newFolder();
    File meta = new File(f, "META-INF/services");
    meta.mkdirs();
    File sfile = new File(meta, "com.tc.util.TestService");
    Writer w = new OutputStreamWriter(new FileOutputStream(sfile), Charset.forName("UTF-8"));
    w.append("com.tc.util.OverrideTestServiceImpl");
    w.close();
    writeClass(f, "com.tc.util.OverrideTestServiceImpl");
    File zip = folder.newFile("test.jar");
    File testApi = writeZip(zip, f);
    ZipFile zf = new java.util.zip.ZipFile(testApi);
    Enumeration<? extends ZipEntry> em = zf.entries();
    while (em.hasMoreElements()) {
      System.out.println(em.nextElement().getName());
    }

    ClassLoader overrides = new URLClassLoader(new URL[] {testApi.toURI().toURL()});
    
    Enumeration<URL> urls = overrides.getResources("META-INF/services/com.tc.util.TestService");
    while(urls.hasMoreElements()) {
      URL next = urls.nextElement();
      System.out.println(next);
      LineNumberReader reader = new LineNumberReader(new InputStreamReader(next.openStream(), Charset.defaultCharset()));
      String line = reader.readLine();
      while (line != null) {
        System.out.println(line);
        line = reader.readLine();
      }
    }
     
    ManagedServiceLoader loader = spy(new ManagedServiceLoader());
    List<Class<? extends TestService>> services = loader.getImplementationsTypes(TestService.class, overrides);
    Assert.assertEquals(1, services.size());
    Assert.assertEquals(OverrideTestServiceImpl.class, services.get(0));
    // test the loadClass got called twice
    try {
      Mockito.verify(loader).loadClass(eq("com.tc.util.TestServiceImpl"), any(String.class), any(ClassLoader.class));
      Mockito.verify(loader).loadClass(eq("com.tc.util.OverrideTestServiceImpl"), any(String.class), any(ClassLoader.class));
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
  }
  
  private File writeZip(File api, File dir) throws IOException {
    ZipBuilder builder = new ZipBuilder(api, true);
    try {
      builder.putTraverseDirectory(dir, "");
    } catch (Throwable t) {
      t.printStackTrace();
      throw new IOException(t);
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
   
   private void writeClass(File base, String className)  throws IOException {
     className = className.replace('.', '/');
     int psplit = className.lastIndexOf('/');
     File impld = new File(base, className.substring(0, psplit));
     impld.mkdirs();
     FileOutputStream fos = new FileOutputStream(new File(impld, className.substring(psplit + 1) + ".class"));
     fos.write(resourceToBytes(className + ".class"));
     fos.close();
   }  
}
