/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassProvider;

public class MockClassProvider implements ClassProvider {

  public MockClassProvider() {
    super();
  }

  public Class getClassFor(String className) throws ClassNotFoundException {
    return getClass().getClassLoader().loadClass(className);
  }

}
