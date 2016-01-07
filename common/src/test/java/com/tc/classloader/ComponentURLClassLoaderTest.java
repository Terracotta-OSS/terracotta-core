/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.classloader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    ClassLoader loader = new ComponentURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, this.getClass().getClassLoader(), new AnnotationBasedCommonComponentChecker());
    Class<?> commonClass = loader.loadClass("com.tc.classloader.CommonComponentClass");
    assertEquals(commonClass.getClassLoader(), this.getClass().getClassLoader());
    Class<?> specificClass = loader.loadClass("com.tc.classloader.SpecificComponentClass");
    assertEquals(specificClass.getClassLoader(), loader);
  }
  
  @Test 
  public void testClassCaching() throws Exception {
    ExposedClassLoader parent = Mockito.spy(new ExposedClassLoader(new URL[0], this.getClass().getClassLoader()));
    ClassLoader loader = new ComponentURLClassLoader(new URL[] {new File(System.getProperty("testClassesDir")).toURI().toURL()}, parent, new AnnotationBasedCommonComponentChecker());
    Class<?> commonClass = loader.loadClass("com.tc.classloader.CommonComponentClass");
    assertTrue(commonClass == loader.loadClass("com.tc.classloader.CommonComponentClass"));
//  should happen twice because the class is common
    verify(parent, times(2)).loadClass(Matchers.eq("com.tc.classloader.CommonComponentClass"), Matchers.anyBoolean());
    Class<?> specificClass = loader.loadClass("com.tc.classloader.SpecificComponentClass");
    assertTrue(specificClass == loader.loadClass("com.tc.classloader.SpecificComponentClass"));
//  should happen once because the class is specific and should be a loaded class and not hit the parent the second time
    verify(parent).loadClass(Matchers.eq("com.tc.classloader.SpecificComponentClass"), Matchers.anyBoolean());
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
}
