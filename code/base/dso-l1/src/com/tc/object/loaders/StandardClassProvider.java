/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class StandardClassProvider implements ClassProvider {

  private static final String BOOT         = Namespace.getStandardBootstrapLoaderName();
  private static final String EXT          = Namespace.getStandardExtensionsLoaderName();
  private static final String SYSTEM       = Namespace.getStandardSystemLoaderName();

  private final ClassLoader   systemLoader = ClassLoader.getSystemClassLoader();
  private final Map           loaders      = new HashMap();

  public StandardClassProvider() {
    //
  }

  public ClassLoader getClassLoader(String desc) {
    if (isStandardLoader(desc)) { return systemLoader; }

    ClassLoader rv = lookupLoader(desc);
    if (rv == null) { throw new AssertionError("No registered loader for description: " + desc); }
    return rv;
  }

  public Class getClassFor(String className, String desc) throws ClassNotFoundException {
    final ClassLoader loader;

    if (isStandardLoader(desc)) {
      loader = systemLoader;
    } else {
      loader = lookupLoader(desc);
      if (loader == null) { throw new ClassNotFoundException("No registered loader for description: " + desc
                                                             + ", trying to load " + className); }
    }

    return Class.forName(className, false, loader);
  }

  public void registerNamedLoader(NamedClassLoader loader) {
    final String name = getName(loader);
    synchronized (loaders) {
      loaders.put(name, new WeakReference(loader));
    }
  }

  private static String getName(NamedClassLoader loader) {
    String name = loader.__tc_getClassLoaderName();
    if (name == null || name.length() == 0) {
      throw new AssertionError("Invalid name [" + name + "] from loader " + loader);
    }
    return name;
  }

  public String getLoaderDescriptionFor(Class clazz) {
    return getLoaderDescriptionFor(clazz.getClassLoader());
  }

  public String getLoaderDescriptionFor(ClassLoader loader) {
    if (loader == null) { return BOOT; }
    if (loader instanceof NamedClassLoader) { return getName((NamedClassLoader) loader); }
    throw new RuntimeException("No loader description for " + loader);
  }

  private boolean isStandardLoader(String desc) {
    if (BOOT.equals(desc) || EXT.equals(desc) || SYSTEM.equals(desc)) { return true; }
    return false;
  }

  private ClassLoader lookupLoader(String desc) {
    final ClassLoader rv;
    synchronized (loaders) {
      WeakReference ref = (WeakReference) loaders.get(desc);
      if (ref != null) {
        rv = (ClassLoader) ref.get();
        if (rv == null) {
          loaders.remove(desc);
        }
      } else {
        rv = null;
      }
    }
    return rv;
  }
}
