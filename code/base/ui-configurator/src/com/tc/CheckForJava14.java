/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc;

import org.apache.commons.lang.StringUtils;

public class CheckForJava14 {
  public static void main(String[] args) {
    String version = System.getProperty("java.version");
    System.exit(StringUtils.contains(version, "1.4") ? 0 : -1);
  }
}
