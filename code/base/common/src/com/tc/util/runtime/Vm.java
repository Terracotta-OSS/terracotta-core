/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vm {

  public static final Pattern JVM_VERSION_PATTERN = Pattern
                                                      .compile("^(\\p{Digit})\\.(\\p{Digit})\\.(\\p{Digit})(?:_(.+))?$");

  public static final Version VERSION;
  static {
    try {
      VERSION = new Version(System.getProperties());
    } catch (UnknownJvmVersionException mve) {
      throw new RuntimeException(mve);
    }
  }

  private Vm() {
  // Utility class
  }

  public static int getMegaVersion() {
    return VERSION.getMegaVersion();
  }

  public static int getMajorVersion() {
    return VERSION.getMajorVersion();
  }

  public static int getMinorVersion() {
    return VERSION.getMinorVersion();
  }

  public static String getPatchLevel() {
    return VERSION.getPatchLevel();
  }

  public static boolean isJDK14() {
    return VERSION.isJDK14();
  }

  public static boolean isJDK15() {
    return VERSION.isJDK15();
  }

  public static boolean isJDK16() {
    return VERSION.isJDK16();
  }
  
  public static boolean isJDK15Compliant() {
    return VERSION.isJDK15() || VERSION.isJDK16();
  }

  public static boolean isJRockit() {
    return VERSION.isJRockit();
  }

  public final static class UnknownJvmVersionException extends Exception {
    private UnknownJvmVersionException(final String badVersion) {
      super("Unable to parse JVM version '" + badVersion + "'");
    }
  }

  public final static class Version {

    private final String  vendorVersion;
    private final int     mega;
    private final int     major;
    private final int     minor;
    private final String  patch;
    private final boolean isJRockit;

    public Version(final Properties properties) throws UnknownJvmVersionException {
      this(properties.getProperty("java.version", "<error: java.version not specified in properties>"), properties
          .getProperty("jrockit.version") != null
          || properties.getProperty("java.vm.name", "").toLowerCase().indexOf("jrockit") >= 0);
    }

    public Version(final String vendorVersion, final boolean isJRockit) throws UnknownJvmVersionException {
      this.vendorVersion = vendorVersion;
      this.isJRockit = isJRockit;
      final Matcher versionMatcher = JVM_VERSION_PATTERN.matcher(vendorVersion);
      if (versionMatcher.matches()) {
        mega = Integer.parseInt(versionMatcher.group(1));
        major = Integer.parseInt(versionMatcher.group(2));
        minor = Integer.parseInt(versionMatcher.group(3));
        patch = versionMatcher.groupCount() == 4 ? versionMatcher.group(4) : null;
      } else {
        throw new UnknownJvmVersionException(vendorVersion);
      }
    }

    /**
     * Given the history of SunOS and Java version numbering by Sun, this will return '1' for a long time to come.
     */
    public int getMegaVersion() {
      return mega;
    }

    public int getMajorVersion() {
      return major;
    }

    public int getMinorVersion() {
      return minor;
    }

    public String getPatchLevel() {
      return patch;
    }

    public boolean isJDK14() {
      return mega == 1 && major == 4;
    }

    public boolean isJDK15() {
      return mega == 1 && major == 5;
    }

    public boolean isJDK16() {
      return mega == 1 && major == 6;
    }

    public boolean isJRockit() {
      return isJRockit;
    }

    public boolean equals(final Object o) {
      if (!(o instanceof Version)) { return false; }

      final Version other = (Version) o;
      return vendorVersion.equals(other.vendorVersion);
    }

    public int hashCode() {
      return vendorVersion.hashCode();
    }

    public String toString() {
      return vendorVersion;
    }
  }

}
