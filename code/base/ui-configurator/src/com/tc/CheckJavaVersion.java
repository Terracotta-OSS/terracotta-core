/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.commons.lang.StringUtils;

public class CheckJavaVersion {
  public static void main(String[] args) {
    if(args.length != 1) {
      throw new IllegalArgumentException("Requires single argument: 'MajorVersion.MinorVersion'");
    }
    String targetVersion = args[0];
    String vmVersion = System.getProperty("java.version");
    System.exit(StringUtils.contains(vmVersion, targetVersion) ? 0 : -1);
  }
}
