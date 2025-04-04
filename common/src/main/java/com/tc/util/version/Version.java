/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
  // \. - a dot
  // \d+ - 1 or more digits
  // \. - a dot
  // \d+ - 1 or more digits
  // _ - an underscore
  // \w+ - letters and/or numbers
  // - - just a dash
  // \w+ - letters and numbers
  private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)?)?(?:\\.(\\d+)?)?(?:\\.(\\d+)?)?)?(?:_(\\w+))?(?:-(\\w+))?$");

  private final int            major;
  private final int            minor;
  private final int            micro;
  private final int            patch;
  private final int            build;
  private final String         specifier;
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
        String patchStr = m.group(4);
        if (patchStr != null) {
          patch = Integer.parseInt(patchStr);
          String buildStr = m.group(5);
          if (buildStr != null) {
            build = Integer.parseInt(buildStr);
          } else {
            build = 0;
          }
        } else {
          patch = 0;
          build = 0;
        }
      } else {
        micro = 0;
        patch = 0;
        build = 0;
      }
    } else {
      minor = 0;
      micro = 0;
      patch = 0;
      build = 0;
    }
    specifier = m.group(6);
    qualifier = m.group(7);
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

  private int patch() {
    return patch;
  }

  private int build() {
    return build;
  }

  /**
   * Tests if this Version is newer than Version 'v' up to a maximum 'depth' of digits.
   * @param v Version to compare against
   * @param depth Maximum number of digits in version string to compare
   * @return TRUE if this Version is newer than Version 'v' up to 'depth' digits comparison, FALSE otherwise
   */
  public boolean isNewer(Version v, int depth) {
    if (v == null) { throw new NullPointerException(); }
    if (depth < 1 || depth > 5) {throw new IndexOutOfBoundsException(); }
    int[] v1 = {major, minor, micro, patch, build};
    int[] v2 = {v.major, v.minor, v.micro, v.patch, v.build};
    int i = 0;
    while (i < depth) {
      if (v1[i] > v2[i]) {
        return true;
      }
      if (v1[i] != v2[i]) {
        return false;
      }
      i++;
    }
    return false;
  }



  /**
   * May be null
   */
  public String qualifier() {
    return this.qualifier;
  }

  public String specifier() {
    return specifier;
  }

  @Override
  public int compareTo(Version otherVersion) {
    int majorDiff = major - otherVersion.major();
    if (majorDiff != 0) { return majorDiff; }

    int minorDiff = minor - otherVersion.minor();
    if (minorDiff != 0) { return minorDiff; }

    int microDiff = micro - otherVersion.micro();
    if (microDiff != 0) { return microDiff; }

    int patchDiff = patch - otherVersion.patch();
    if (patchDiff != 0) { return patchDiff; }

    int buildDiff = build - otherVersion.build();
    if (buildDiff != 0) { return buildDiff; }

    if (specifier == null) {
      if (otherVersion.specifier != null) { return 1; }
    } else if (otherVersion.specifier == null) {
      return -1;
    } else if (specifier.compareTo(otherVersion.specifier) != 0) {
      return specifier.compareTo(otherVersion.specifier);
    }

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
    StringBuilder versionString = new StringBuilder().append(major);
    versionString.append(".").append(minor).append(".").append(micro)
                  .append(".").append(patch).append(".").append(build);
    if (specifier != null) {
      versionString.append("_" + specifier);
    }
    if (qualifier != null) {
      versionString.append("-" + qualifier);
    }
    return versionString.toString();
  }
}
