/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;


public class MockClassProvider implements ClassProvider {

  public MockClassProvider() {
    super();
  }

  public Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException {
    return getClass().getClassLoader().loadClass(className);

  }

  public String getLoaderDescriptionFor(Class clazz) {
    return "";
  }

  public ClassLoader getClassLoader(String loaderDesc) {
    return getClass().getClassLoader();
  }

  public String getLoaderDescriptionFor(ClassLoader loader) {
    return "";
  }

  public void registerNamedLoader(NamedClassLoader loader) {
    // do nothing
  }

}
