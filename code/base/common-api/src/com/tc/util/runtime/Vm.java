/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vm {

  public static final Pattern JVM_VERSION_PATTERN         = Pattern
                                                              .compile("^(\\p{Digit})\\.(\\p{Digit})\\.(\\p{Digit})(?:[-_](.+))?$");
  public static final Pattern IBM_SERVICE_RELEASE_PATTERN = Pattern
                                                              .compile("^[^-]+-\\p{Digit}{8}[^\\p{Space}]*\\p{Space}*\\(.*(SR\\p{Digit}+).*\\)$");

  public static final Version VERSION;
  static {
    try {
      VERSION = new Version(System.getProperties());
    } catch (UnknownJvmVersionException mve) {
      throw new RuntimeException(mve);
    } catch (UnknownRuntimeVersionException mve) {
      throw new RuntimeException(mve);
    }
  }

  private Vm() {
  // utility class
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

  public static boolean isJDK17() {
    return VERSION.isJDK17();
  }

  public static boolean isJDK15Compliant() {
    return VERSION.isJDK15() || VERSION.isJDK16() || VERSION.isJDK17();
  }
  
  public static boolean isJDK16Compliant() {
    return VERSION.isJDK16() || VERSION.isJDK17();
  }

  public static boolean isIBM() {
    return VERSION.isIBM();
  }

  public static boolean isJRockit() {
    return VERSION.isJRockit();
  }

  public final static class Version {

    private final String  vmVersion;
    private final int     mega;
    private final int     major;
    private final int     minor;
    private final String  patch;
    private final boolean isIBM;
    private final boolean isJRockit;

    public Version(final Properties properties) throws UnknownJvmVersionException, UnknownRuntimeVersionException {
      this(properties.getProperty("java.version", "<error: java.version not specified in properties>"), properties
          .getProperty("java.runtime.version", "<error: java.runtime.version not specified in properties>"), properties
          .getProperty("jrockit.version") != null
          || properties.getProperty("java.vm.name", "").toLowerCase().indexOf("jrockit") >= 0, properties.getProperty(
          "java.vendor", "").toLowerCase().startsWith("ibm "));
    }

    public Version(final String vendorVersion, final String runtimeVersion, final boolean isJRockit, final boolean isIBM)
        throws UnknownJvmVersionException, UnknownRuntimeVersionException {
      this.isIBM = isIBM;
      this.isJRockit = isJRockit;
      final Matcher versionMatcher = JVM_VERSION_PATTERN.matcher(vendorVersion);
      if (versionMatcher.matches()) {
        mega = Integer.parseInt(versionMatcher.group(1));
        major = Integer.parseInt(versionMatcher.group(2));
        minor = Integer.parseInt(versionMatcher.group(3));
        String version_patch = versionMatcher.groupCount() == 4 ? versionMatcher.group(4) : null;
        if (isIBM) {
          final Matcher serviceReleaseMatcher = IBM_SERVICE_RELEASE_PATTERN.matcher(runtimeVersion);
          if (serviceReleaseMatcher.matches()) {
            String serviceRelease = serviceReleaseMatcher.groupCount() == 1 ? serviceReleaseMatcher.group(1)
                .toLowerCase() : null;
            if (null == version_patch && null == serviceRelease) {
              patch = null;
            } else if (null == version_patch) {
              patch = serviceRelease;
            } else if (null == serviceRelease) {
              patch = version_patch;
            } else {
              patch = version_patch + serviceRelease;
            }
          } else {
            throw new UnknownRuntimeVersionException(vendorVersion, runtimeVersion);
          }
        } else {
          patch = version_patch;
        }
      } else {
        throw new UnknownJvmVersionException(vendorVersion);
      }
      this.vmVersion = this.mega + "." + this.major + "." + this.minor + (null == patch ? "" : "_" + patch);
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

    public boolean isJDK17() {
      return mega == 1 && major == 7;
    }

    public boolean isIBM() {
      return isIBM;
    }

    public boolean isJRockit() {
      return isJRockit;
    }

    public boolean equals(final Object o) {
      if (!(o instanceof Version)) { return false; }

      final Version other = (Version) o;
      return vmVersion.equals(other.vmVersion);
    }

    public int hashCode() {
      return vmVersion.hashCode();
    }

    public String toString() {
      return vmVersion;
    }
  }

}
