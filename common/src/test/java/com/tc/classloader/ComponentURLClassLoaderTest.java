/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    ClassLoader loader = new ComponentURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, this.getClass().getClassLoader());
    Class<?> commonClass = loader.loadClass("com.tc.classloader.CommonComponentClass");
    assertEquals(commonClass.getClassLoader(), this.getClass().getClassLoader());
    Class<?> specificClass = loader.loadClass("com.tc.classloader.SpecificComponentClass");
    assertEquals(specificClass.getClassLoader(), loader);
  }
}
