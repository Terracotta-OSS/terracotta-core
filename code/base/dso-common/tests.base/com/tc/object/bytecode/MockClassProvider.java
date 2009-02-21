/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.loaders.NamedClassLoader;


public class MockClassProvider implements ClassProvider {
  
  public static final LoaderDescription MOCK_LOADER = new LoaderDescription(null, "mock");

  public MockClassProvider() {
    super();
  }

  public LoaderDescription getLoaderDescriptionFor(Class clazz) {
    return MOCK_LOADER;
  }

  public ClassLoader getClassLoader(LoaderDescription desc) {
    return getClass().getClassLoader();
  }

  public Class getClassFor(String className, LoaderDescription desc) throws ClassNotFoundException {
    return getClass().getClassLoader().loadClass(className);
  }
  
  public LoaderDescription getLoaderDescriptionFor(ClassLoader loader) {
    return MOCK_LOADER;
  }

  public void registerNamedLoader(NamedClassLoader loader) {
    // do nothing
  }

  public void registerNamedLoader(NamedClassLoader loader, String appGroup) {
    // do nothing
  }

}
