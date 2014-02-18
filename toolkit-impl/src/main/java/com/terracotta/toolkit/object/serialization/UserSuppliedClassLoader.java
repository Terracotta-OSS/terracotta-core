/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

public class UserSuppliedClassLoader extends ClassLoader {

  private final ClassLoader userLoader;
  private final ClassLoader toolkitLoader;

  public UserSuppliedClassLoader(ClassLoader userLoader, ClassLoader toolkitLoader) {
    this.userLoader = userLoader;
    this.toolkitLoader = toolkitLoader;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    try {
      return userLoader.loadClass(name);
    } catch (ClassNotFoundException cnfe) {
      //
    }

    return toolkitLoader.loadClass(name);
  }

}
