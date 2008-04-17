/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.tools.BootJarTool;

import junit.framework.TestCase;

public class ApplicationHelperTest extends TestCase {

  private static final String TEST_CLASSNAME = ApplicationHelperTest.class.getName(); //doesn't matter what class
  private ApplicationHelper appHelper;
  private Class testClass;
  private MockClassLoader mockLoader;
  
  protected void setUp() throws Exception {
    super.setUp();
    mockLoader = new MockClassLoader();
    testClass = mockLoader.loadClass(TEST_CLASSNAME);
    assertSame(mockLoader, testClass.getClassLoader());
  }
  
  public void testTypicalTomcatContextPath(){
    mockLoader.__tc_setClassLoaderName(ApplicationHelper.TOMCAT_PREFIX + "Catalina:localhost/Foo");
    appHelper = new ApplicationHelper(testClass);
    assertEquals("Foo", appHelper.getAppName());
  }

  public void testTomcatRootContext(){
    mockLoader.__tc_setClassLoaderName(ApplicationHelper.TOMCAT_PREFIX + "Catalina:localhost");
    appHelper = new ApplicationHelper(testClass);
    assertEquals(ApplicationHelper.ROOT_APP_NAME, appHelper.getAppName());
  }

  public void testTypicalWeblogicContextPath(){
    mockLoader.__tc_setClassLoaderName(ApplicationHelper.WEBLOGIC_PREFIX + "blah:localhost@Foo");
    appHelper = new ApplicationHelper(testClass);
    assertEquals("Foo", appHelper.getAppName());
  }

  /* what's expected here?  This test just exercises the code that's already there, not sure if it's correct.  */
  public void testWeblogicRootContext(){
    final String classLoaderName = ApplicationHelper.WEBLOGIC_PREFIX + "blah:localhost";
    mockLoader.__tc_setClassLoaderName(classLoaderName);
    appHelper = new ApplicationHelper(testClass);
    assertEquals(classLoaderName, appHelper.getAppName());
  }
  
  
  private static final class MockClassLoader extends ClassLoader implements NamedClassLoader{

    private String name;
    
    public Class loadClass(String classname) throws ClassNotFoundException {
      if (classname.equals(TEST_CLASSNAME)){
        byte[] bytes = BootJarTool.getBytesForClass(classname, this);
        return defineClass(classname, bytes, 0, bytes.length);
      }
      return super.loadClass(classname);
    }

    public String __tc_getClassLoaderName() {
      return name;
    }

    public void __tc_setClassLoaderName(String loaderName) {
      this.name = loaderName;
    }
  }
}