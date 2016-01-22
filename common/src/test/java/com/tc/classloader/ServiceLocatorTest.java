/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import static com.tc.config.Directories.TC_INSTALL_ROOT_PROPERTY_NAME;
import com.tc.util.Assert;
import com.tc.util.ZipBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
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
     File impl = new File(f, "plugins/impl/");
     File meta = new File(impl, "META-INF/services");
     meta.mkdirs();
     File api = new File(f, "plugins/api");
     api.mkdirs();
     writeClass(impl, "com.tc.classloader.TestInterfaceImpl");
     File testApi = writeZip(new File(api, "test.jar"), "com.tc.classloader.TestInterface");
     System.setProperty(TC_INSTALL_ROOT_PROPERTY_NAME, f.getAbsolutePath());
     
     ClassLoader testloader = new URLClassLoader(new URL[] {impl.toURI().toURL()}, this.getClass().getClassLoader());
     ClassLoader apiLoader = new ApiClassLoader(new URL[] {testApi.toURI().toURL()}, testloader);
     
     FileOutputStream services = new FileOutputStream(new File(meta, "com.tc.classloader.TestInterface"));
     services.write("com.tc.classloader.TestInterfaceImpl".getBytes());
     services.close();
     ComponentURLClassLoader component = new ComponentURLClassLoader(new URL[] {impl.toURI().toURL()}, 
         apiLoader, 
         new AnnotationOrDirectoryStrategyChecker());
     Class<?> interf = component.loadClass("com.tc.classloader.TestInterface");
     Class<?> interi = component.loadClass("com.tc.classloader.TestInterfaceImpl");
     Assert.assertTrue(interf.getClassLoader() instanceof ApiClassLoader);
     Assert.assertEquals(interf.getClassLoader(), apiLoader);
     Assert.assertEquals(interi.getClassLoader(), component);
     
     List<?> list = ServiceLocator.getImplementations("com.tc.classloader.TestInterface", testloader);
     Assert.assertEquals(list.size(), 1);
     Assert.assertEquals(list.get(0).getClass().getName(), "com.tc.classloader.TestInterfaceImpl");
     Assert.assertTrue(list.get(0).getClass().getClassLoader() instanceof ComponentURLClassLoader);
     System.out.println(list.get(0).getClass().getInterfaces()[0].getClassLoader());
     Assert.assertTrue(list.get(0).getClass().getInterfaces()[0].getClassLoader() instanceof ApiClassLoader);
     Assert.assertEquals(list.get(0).getClass().getInterfaces()[0].getName(), "com.tc.classloader.TestInterface");
   }
   
   private File writeZip(File api, String...classes) throws IOException {
     ZipBuilder builder = new ZipBuilder(api, true);
     for (String className : classes) {
       className = className.replace('.', '/');
       ByteArrayOutputStream fos = new ByteArrayOutputStream();
       InputStream implb = getClass().getClassLoader().getResourceAsStream(className + ".class");
       int check = implb.read();
       while (check >= 0) {
         fos.write(check);
         check = implb.read();
       }
       fos.close();
       builder.putEntry(className + ".class", fos.toByteArray());
     }
     builder.finish();
     return api;
   }
   
   private void writeClass(File base, String className)  throws IOException {
     className = className.replace('.', '/');
     int psplit = className.lastIndexOf('/');
     File impld = new File(base, className.substring(0, psplit));
     impld.mkdirs();
     FileOutputStream fos = new FileOutputStream(new File(impld, className.substring(psplit + 1) + ".class"));
     InputStream implb = getClass().getClassLoader().getResourceAsStream(className + ".class");
     int check = implb.read();
     while (check >= 0) {
       fos.write(check);
       check = implb.read();
     }
     fos.close();
   }
}
