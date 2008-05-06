/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Helper methods to load resources from a jar
 */
public final class JarResourceLoader {

  private JarResourceLoader() {
    // uninstantiateable
  }
  
  /**
   * Load a resource file from a jar file at location
   * @param location The URL to the JAR file
   * @param resource The resource string
   * @return Input stream to the resource - close it when you're done
   * @throws IOException If some bad IO happens
   */
  public static InputStream getJarResource(final URL location, final String resource) throws IOException {
    final JarInputStream jis = new JarInputStream(location.openStream());
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      if (entry.getName().equals(resource)) { return jis; }
    }
    return null;
  }

}
