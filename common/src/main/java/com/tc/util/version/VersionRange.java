/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionRange {
  // A version range:
  // ^ - match start at beginning
  // [([] - the begin range character
  // <version> - see below
  // , - just a comma
  // <version> - see below
  // [\])] - the end range character, does not have to match begin range character
  // $ - match must hit end of string
  //
  // A version:
  // \d+ - 1 or more digits
  // \. - a dot
  // \d+ - 1 or more digits
  // \. - a dot
  // \d+ - 1 or more digits
  // - - just a dash
  // \w+ - letters and numbers
  private static final String  VERSION_PATTERN_STR = "^(\\d+(?:\\.\\d+(?:\\.\\d+(?:-\\w+)?)?)?)$";

  private static final Pattern SINGLE_COMMA        = Pattern.compile("^([^,]+)?,([^,]+)?$");
  private static final Pattern VERSION             = Pattern.compile(VERSION_PATTERN_STR);

  private final String         minVersion;
  private final String         maxVersion;
  private final boolean        minIsInclusive;
  private final boolean        maxIsInclusive;

  public VersionRange(String versionString) {
    if (versionString == null) { throw new NullPointerException(); }
    versionString = versionString.trim();

    if (isRange(versionString)) {
      minIsInclusive = versionString.startsWith("[");
      maxIsInclusive = versionString.endsWith("]");

      versionString = versionString.replaceFirst("^[\\[(]", "");
      versionString = versionString.replaceFirst("[\\])]$", "");
      versionString = versionString.trim();

      if (versionString.endsWith(",")) {
        minVersion = verifyVersionFormat(versionString.replaceFirst(",$", ""));
        maxVersion = "";
      } else if (versionString.startsWith(",")) {
        maxVersion = verifyVersionFormat(versionString.replaceFirst("^,", ""));
        minVersion = "";
      } else {
        String[] pair = versionString.split(",");
        if (pair.length != 2) { throw new AssertionError("Unexpected number of elements (" + pair.length + "): "
                                                         + versionString); }
        for (int i = 0; i < pair.length; i++) {
          pair[i] = pair[i].trim();
        }

        minVersion = verifyVersionFormat(pair[0]);
        maxVersion = verifyVersionFormat(pair[1]);
      }
    } else {
      minVersion = verifyVersionFormat(versionString);
      maxVersion = verifyVersionFormat(versionString);
      minIsInclusive = true;
      maxIsInclusive = true;
    }
  }

  private String verifyVersionFormat(String ver) {
    Matcher matcher = VERSION.matcher(ver);
    if (matcher.matches()) { return ver; }

    throw new IllegalArgumentException("Unexpected version string format: " + ver);
  }

  private boolean isRange(String ver) {
    if (ver.startsWith("[") || ver.startsWith("(")) {
      if (ver.endsWith("]") || ver.endsWith(")")) {
        Matcher matcher = SINGLE_COMMA.matcher(ver);
        if (matcher.matches()) {
          return true;
        } else {
          throw new IllegalArgumentException("Apparent version range missing single comma: " + ver);
        }
      } else {
        throw new IllegalArgumentException("Version string missing proper trailing character: " + ver);
      }
    }
    return false;
  }

  public String getMinVersion() {
    return minVersion;
  }

  public String getMaxVersion() {
    return maxVersion;
  }

  public boolean isMinInclusive() {
    return minIsInclusive;
  }

  public boolean isMaxInclusive() {
    return maxIsInclusive;
  }

  public boolean contains(String otherVersionStr) {
    if (!Version.isValidVersionString(otherVersionStr)) {
      return false;
    }
    
    Version otherVersion = new Version(otherVersionStr);

    int compareMin = isMinUnbounded() ? 1 : otherVersion.compareTo(new Version(minVersion));
    int compareMax = isMaxUnbounded() ? -1 : otherVersion.compareTo(new Version(maxVersion));

    boolean greaterThanMin = compareMin > 0 || (isMinInclusive() && compareMin == 0);
    boolean lessThanMax = compareMax < 0 || (isMaxInclusive() && compareMax == 0);

    return greaterThanMin && lessThanMax;
  }

  public boolean isMaxUnbounded() {
    return maxVersion.length() == 0;
  }

  public boolean isMinUnbounded() {
    return minVersion.length() == 0;
  }
}
