/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.loaders;

public class SingleLoaderClassProvider implements ClassProvider {

  private final ClassLoader loader;

  public SingleLoaderClassProvider(ClassLoader loader) {
    this.loader = loader;
  }

  @Override
  public Class<?> getClassFor(String className) throws ClassNotFoundException {
    return LoadClassUtil.loadClass(className, loader);
  }

}
