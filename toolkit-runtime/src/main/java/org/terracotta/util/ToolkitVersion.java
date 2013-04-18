/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ToolkitVersion {

  public static void main(String[] args) {
    InputStream in = ToolkitVersion.class.getResourceAsStream("/Version.info");
    Properties props = new Properties();

    try {
      props.load(in);
    } catch (IOException e) {
      System.err.println("Unable to load Version.info");
    }
    System.out.println("Supported Toolkit api version" + props.getProperty("toolkit-api-version"));
  }
}
