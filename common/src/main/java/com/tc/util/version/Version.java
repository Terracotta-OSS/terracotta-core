/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
  // A version:
  // \d+ - 1 or more digits
  // \. - a dot
  // \d+ - 1 or more digits
  // \. - a dot
  // \d+ - 1 or more digits
  // - - just a dash
  // \w+ - letters and numbers
  private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:-(\\w+))?)?)?$");

  private final int            major;
  private final int            minor;
  private final int            micro;
  private final String         qualifier;

  /**
   * Throws IllegalArgumentException on bad version string
   */
  public Version(String version) {
    Matcher m = VERSION_PATTERN.matcher(version);
    if (!m.matches()) throw invalidVersion(version);

    major = Integer.parseInt(m.group(1));
    String minorStr = m.group(2);
    if (minorStr != null) {
      minor = Integer.parseInt(minorStr);
      String microStr = m.group(3);
      if (microStr != null) {
        micro = Integer.parseInt(microStr);
        qualifier = m.group(4);
      } else {
        micro = 0;
        qualifier = null;
      }
    } else {
      minor = 0;
      micro = 0;
      qualifier = null;
    }
  }

  public static boolean isValidVersionString(String version) {
    Matcher m = VERSION_PATTERN.matcher(version);
    return m.matches();
  }

  private IllegalArgumentException invalidVersion(String input) {
    return new IllegalArgumentException("Invalid version, unable to parse: " + input);
  }

  public int major() {
    return this.major;
  }

  public int minor() {
    return this.minor;
  }

  public int micro() {
    return this.micro;
  }

  /**
   * May be null
   */
  public String qualifier() {
    return this.qualifier;
  }

  public int compareTo(Version otherVersion) {
    int majorDiff = major - otherVersion.major();
    if (majorDiff != 0) { return majorDiff; }

    int minorDiff = minor - otherVersion.minor();
    if (minorDiff != 0) { return minorDiff; }

    int microDiff = micro - otherVersion.micro();
    if (microDiff != 0) { return microDiff; }

    // Any version with a qualifier is considered "less than" a version without:
    // 1.0.0-SNAPSHOT < 1.0.0
    if (qualifier == null) {
      if (otherVersion.qualifier != null) { return 1; }
    } else if (otherVersion.qualifier == null) {
      return -1;
    } else {
      return qualifier.compareTo(otherVersion.qualifier);
    }

    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Version) {
      return this.compareTo((Version) obj) == 0;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + micro + (qualifier == null ? "" : "." + qualifier);
  }
}
