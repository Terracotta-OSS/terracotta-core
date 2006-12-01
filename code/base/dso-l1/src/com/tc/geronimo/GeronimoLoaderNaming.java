/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.geronimo;

public class GeronimoLoaderNaming {

  public static String adjustName(String name) {
    if (name != null && name.endsWith("war")) {
      String[] parts = name.split("/", -1);
      if (parts.length != 4) { throw new RuntimeException("unknown format: " + name + ", # parts = " + parts.length); }

      if ("war".equals(parts[3]) && parts[2].matches("^\\d+$")) {
        name = name.replaceAll(parts[2], "");
      }
    }

    return name;
  }

  public static void main(String args[]) {
    String name = "Geronimo.default/simplesession/1164587457359/war";
    System.err.println(adjustName(name));
  }

}
