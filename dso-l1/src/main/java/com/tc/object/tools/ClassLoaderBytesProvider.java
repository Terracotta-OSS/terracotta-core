/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassLoaderBytesProvider implements ClassBytesProvider {

  private final ClassLoader source;

  public ClassLoaderBytesProvider(ClassLoader source) {
    this.source = source;
  }

  public byte[] getBytesForClass(String className) throws ClassNotFoundException {
    String resource = BootJar.classNameToFileName(className);

    InputStream is = source.getResourceAsStream(resource);
    if (is == null) { throw new ClassNotFoundException("No resource found for class: " + className); }
    final int size = 4096;
    byte[] buffer = new byte[size];
    ByteArrayOutputStream baos = new ByteArrayOutputStream(size);

    int read;
    try {
      while ((read = is.read(buffer, 0, size)) > 0) {
        baos.write(buffer, 0, read);
      }
    } catch (IOException ioe) {
      throw new ClassNotFoundException("Error reading bytes for " + resource, ioe);
    } finally {
      try {
        is.close();
      } catch (IOException ioe) {
        // ignore
      }
    }

    return baos.toByteArray();
  }

}
