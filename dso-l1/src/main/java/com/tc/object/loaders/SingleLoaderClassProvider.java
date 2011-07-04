/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.loaders;

public class SingleLoaderClassProvider implements ClassProvider {

  private final LoaderDescription loaderDesc;
  private final ClassLoader       loader;

  public SingleLoaderClassProvider(String appGroup, String desc, ClassLoader loader) {
    this.loader = loader;
    this.loaderDesc = new LoaderDescription(appGroup, desc);
  }

  public Class getClassFor(String className, LoaderDescription desc) throws ClassNotFoundException {
    return Class.forName(className, false, loader);
  }

  public ClassLoader getClassLoader(LoaderDescription desc) {
    return loader;
  }

  public LoaderDescription getLoaderDescriptionFor(Class clazz) {
    return loaderDesc;
  }

  public LoaderDescription getLoaderDescriptionFor(ClassLoader cl) {
    return loaderDesc;
  }

  public void registerNamedLoader(NamedClassLoader cl, String appGroup) {
    throw new UnsupportedOperationException();
  }

}
