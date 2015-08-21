/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

class Util {

  private Util() {
    //
  }

  public static byte[] extract(InputStream in) throws IOException {
    if (in == null) { throw new NullPointerException(); }

    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] data = new byte[4096];
      int read = 0;
      while ((read = in.read(data, 0, data.length)) > 0) {
        out.write(data, 0, read);
      }
      return out.toByteArray();
    } finally {
      closeQuietly(in);
    }
  }

  public static void closeQuietly(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(Reader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(OutputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }
}