/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.Version;

final class OsgiVersion extends Version {

  OsgiVersion(String arg0) {
    super(MavenToOSGi.projectVersionToBundleVersion(arg0));
  }

  OsgiVersion(int major, int minor, int micro) {
    super(major, minor, micro);
  }

  OsgiVersion(int major, int minor, int micro, String qualifier) {
    super(major, minor, micro, qualifier);
  }

  static OsgiVersion parse(String version) {
    org.osgi.framework.Version v = org.osgi.framework.Version.parseVersion(MavenToOSGi
        .projectVersionToBundleVersion(version));
    return new OsgiVersion(v.getMajor(), v.getMinor(), v.getMicro(), v.getQualifier());
  }

  @Override
  public String toString() {
    return getMajor() + ";" + getMinor() + ";" + getMicro() + ";" + getQualifier();
  }
}
