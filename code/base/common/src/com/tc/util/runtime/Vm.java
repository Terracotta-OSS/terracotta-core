/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

import java.util.HashMap;
import java.util.Map;

public class Vm {

  public static final Version VERSION_1_4 = new Version("1.4");
  public static final Version VERSION_1_5 = new Version("1.5");

  private static final Map    versions    = new HashMap();

  static {
    versions.put(VERSION_1_4.version, VERSION_1_4);
    versions.put(VERSION_1_5.version, VERSION_1_5);
  }

  private Vm() {
    // utility class
  }

  public static Version getMajorVersion() {
    String version = System.getProperty("java.version");
    if (version != null && version.length() >= 3) {
      Version rv = (Version) versions.get(version.substring(0, 3));
      if (rv != null) { return rv; }
    }
    throw new RuntimeException("Cannot determine version based on 'java.version' value of " + version);
  }

  public static boolean isJRockit() {
    return (System.getProperty("java.vm.name", "").toLowerCase().indexOf("jrockit") >= 0)
           || (System.getProperty("jrockit.version") != null);
  }
  
  public static boolean isJDK15() {
    return VERSION_1_5.equals(getMajorVersion());
  }

  public final static class Version {
    private final String version;

    private Version(String version) {
      this.version = version;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Version)) { return false; }

      Version other = (Version) o;
      return this.version.equals(other.version);
    }

    public int hashCode() {
      return this.version.hashCode();
    }

    public String toString() {
      return this.version;
    }
  }

}
