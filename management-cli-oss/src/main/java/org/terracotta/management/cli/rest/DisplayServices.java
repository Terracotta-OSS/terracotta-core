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
package org.terracotta.management.cli.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class DisplayServices {
  private static final Collection<String> BINARY_CONTENT_TYPES = Arrays.asList("application/zip");

  private static boolean performUrlEncoding = false;

  public static void setPerformUrlEncoding(boolean performUrlEncoding) {
    DisplayServices.performUrlEncoding = performUrlEncoding;
  }

  private static String prepare(String s) {
    if (performUrlEncoding && s != null) {
      try {
        s = URLEncoder.encode(s, "UTF-8").replace("+", "%20");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return s;
  }

  public static void println(Object o) throws IOException {
    String s = toString(o);
    s = prepare(s);
    System.out.println(s);
  }

  public static void print(Object o) throws IOException {
    String s = toString(o);
    s = prepare(s);
    System.out.print(s);
  }

  private static String toString(Object o) {
    if (o == null) {
      return  "";
    } else {
      return o.toString();
    }
  }

  public static void println(byte[] bytes, String encoding, String contentType) throws IOException {
    if (BINARY_CONTENT_TYPES.contains(contentType)) {
      System.out.write(bytes);
    } else {
      String s = new String(bytes, encoding);
      println(s);
    }
  }
}
