/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.express.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

public class Util {

  public static byte[] extract(final InputStream in) throws IOException {
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

  public static void closeQuietly(final InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(final Reader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  public static void closeQuietly(final OutputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }
}
