/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.express.loader.Util;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class ClusteredStateLoader extends SecureClassLoader {
  private static final boolean        DEBUG_TOOLKIT_CLASS_LOADING = false;
  protected final Map<String, byte[]> extraClasses = new ConcurrentHashMap<String, byte[]>();

  public ClusteredStateLoader(ClassLoader parent) {
    super(parent);
  }

  protected void addExtraClass(String name, byte[] classBytes) {
    extraClasses.put(name, classBytes);
  }

  protected Class<?> loadClassFromUrl(String name, URL url, CodeSource codeSource) {
    String packageName = name.substring(0, name.lastIndexOf('.'));
    if (getPackage(packageName) == null) {
      definePackage(packageName, null, null, null, null, null, null, null);
    }

    try {
      byte[] bytes = Util.extract(url.openStream());
      Class<?> clazz = defineClass(name, bytes, 0, bytes.length, codeSource);
      return clazz;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // useful for debugging
  protected Class<?> returnAndLog(Class<?> c, String source) {
    if (DEBUG_TOOLKIT_CLASS_LOADING) {
      System.out.println("XXX loaded " + c + " from " + source);
    }
    return c;
  }
}
