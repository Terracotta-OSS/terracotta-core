/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class JarResourceLoader {

  private JarResourceLoader() {
    // uninstantiateable
  }
  
  public static InputStream getJarResource(final URL location, final String resource) throws IOException {
    final JarInputStream jis = new JarInputStream(location.openStream());
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      if (entry.getName().equals(resource)) { return jis; }
    }
    return null;
  }

}
