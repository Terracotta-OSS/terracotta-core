/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import static com.tc.config.Directories.TC_INSTALL_ROOT_PROPERTY_NAME;

import com.tc.config.Directories;
import com.tc.util.Assert;
import com.tc.util.ZipBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mscott
 */
public class ServiceLocatorTest {
  
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  public ServiceLocatorTest() {
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
   public void test() throws Exception {
     File f = folder.newFolder();
     File impl = new File(f, Directories.SERVER_PLUGIN_LIB_DIR);
     File meta = new File(impl, "META-INF/services");
     meta.mkdirs();
     File api = new File(f, Directories.SERVER_PLUGIN_API_DIR);
     api.mkdirs();
     writeClass(impl, "com.tc.classloader.TestInterfaceImpl");
     writeClass(impl, "com.tc.classloader.TestInterfaceHandle");
     File testApi = writeZip(new File(api, "test.jar"), "com.tc.classloader.TestInterface");
     System.setProperty(TC_INSTALL_ROOT_PROPERTY_NAME, f.getAbsolutePath());
     
     ClassLoader testloader = new URLClassLoader(new URL[] {impl.toURI().toURL()}, this.getClass().getClassLoader());
     ClassLoader apiLoader = new ApiClassLoader(new URL[] {testApi.toURI().toURL()}, testloader);
     
     FileOutputStream services1 = new FileOutputStream(new File(meta, "com.tc.classloader.TestInterface"));
     services1.write("com.tc.classloader.TestInterfaceImpl".getBytes());
     services1.close();
     FileOutputStream services2 = new FileOutputStream(new File(meta, "java.lang.Runnable"));
     services2.write("com.tc.classloader.TestInterfaceHandle".getBytes());
     services2.close();
     ComponentURLClassLoader component = new ComponentURLClassLoader(new URL[] {impl.toURI().toURL()}, 
         apiLoader, 
         new AnnotationOrDirectoryStrategyChecker());
     Class<?> interf = component.loadClass("com.tc.classloader.TestInterface");
     Class<?> interi = component.loadClass("com.tc.classloader.TestInterfaceImpl");
     Assert.assertTrue(interf.getClassLoader() instanceof ApiClassLoader);
     Assert.assertEquals(interf.getClassLoader(), apiLoader);
     Assert.assertEquals(interi.getClassLoader(), component);
     
     Thread.currentThread().setContextClassLoader(apiLoader);
     List<Class<? extends Runnable>> list = ServiceLocator.getImplementations(Runnable.class);
     for (Class<? extends Runnable> r : list) {
       r.newInstance().run();
     }
   }
   
   @Test
   public void testURLsFromZip() throws Exception {
     File base = folder.newFolder();
     File test = new File(base, "test.jar");
     ZipBuilder zip = new ZipBuilder(test, false);
     zip.putEntry("META-INF/services/com.tc.classloader.TestInterface", "com.tc.classloader.TestInterfaceImpl".getBytes());
     zip.putEntry("com/tc/classloader/TestInterfaceImpl.class", resourceToBytes("com/tc/classloader/TestInterfaceImpl.class"));
//  put it in the zip so null parent loader can find it
     zip.putEntry("com/tc/classloader/TestInterface.class", resourceToBytes("com/tc/classloader/TestInterface.class"));
     zip.finish();
     Map<String, String> map = ServiceLocator.discoverImplementations(new URLClassLoader(new URL[] {test.toURI().toURL()}), "com.tc.classloader.TestInterface");
     Assert.assertTrue(map.size() == 1);
     try {
       ClassLoader loader = new URLClassLoader(new URL[] {new URL(map.get("com.tc.classloader.TestInterfaceImpl").toString())}, null);
       Class<?> c = loader.loadClass("com.tc.classloader.TestInterfaceImpl");
       Assert.assertNotNull(c);
     } catch (MalformedURLException m) {
//  not expected
       throw m;
     } catch (ClassNotFoundException c) {
       throw c;
     } 
   }
   
   @Test
   public void testURLsFromDir() throws Exception {
     File base = folder.newFolder();
     new File(base, "META-INF/services/").mkdirs();
     FileOutputStream meta = new FileOutputStream(new File(base, "META-INF/services/com.tc.classloader.TestInterface"));
     meta.write("com.tc.classloader.TestInterfaceImpl".getBytes());
     meta.close();
     new File(base, "com/tc/classloader/").mkdirs();
     FileOutputStream clazz = new FileOutputStream(new File(base, "com/tc/classloader/TestInterfaceImpl.class"));
     clazz.write(resourceToBytes("com/tc/classloader/TestInterfaceImpl.class"));
     clazz.close();
     
     FileOutputStream impl = new FileOutputStream(new File(base, "com/tc/classloader/TestInterface.class"));
     impl.write(resourceToBytes("com/tc/classloader/TestInterface.class"));
     impl.close();
     
     Map<String, String> map = ServiceLocator.discoverImplementations(new URLClassLoader(new URL[] {base.toURI().toURL()}), "com.tc.classloader.TestInterface");
     Assert.assertTrue(map.size() == 1);
     try {
       ClassLoader loader = new URLClassLoader(new URL[] {new URL(map.get("com.tc.classloader.TestInterfaceImpl").toString())}, null);
       Class<?> c = loader.loadClass("com.tc.classloader.TestInterfaceImpl");
       Assert.assertNotNull(c);
     } catch (MalformedURLException m) {
//  not expected
       throw m;
     } catch (ClassNotFoundException c) {
       throw c;
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
