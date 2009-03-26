/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VersionRange {
  // A version range:  
  //   ^         - match start at beginning
  //   [([]      - the begin range character
  //   <version> - see above
  //   ,         - just a comma
  //   <version> - see above
  //   [\])]     - the end range character, does not have to match begin range character
  //   $         - match must hit end of string
  // A version:
  //   \d+ - 1 or more digits
  //   \.  - a dot
  //   \d+ - 1 or more digits
  //   \.  - a dot
  //   \d+ - 1 or more digits
  //   -   - just a dash
  //   \w+ - letters and numbers
  static final String VERSION_PATTERN_STR = "(\\d+(?:\\.\\d+(?:\\.\\d+(?:-\\w+)?)?)?)";

  //
  // Examples:
  //    1
  //    1.2
  //    1.2.3
  //    1.2.2-SNAPSHOT
  //    [1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)
  // 
  // Match groups:
  //    0 - whole input
  //    1 - [ or ( begin range
  //    2 - min version 
  //    3 - max version
  //    4 - ] or ) end range
  private static final Pattern VERSION = Pattern.compile("^([(\\[])?" + VERSION_PATTERN_STR + "(?:," + VERSION_PATTERN_STR + "([\\])])?)?$");

  private final String minVersion;
  private final String maxVersion;
  private final boolean minIsInclusive;
  private final boolean maxIsInclusive;

  public VersionRange(String versionString) {
    Matcher m = VERSION.matcher(versionString);
    if(m.matches()) { 
      if(m.groupCount() == 4) {
        minIsInclusive = m.group(1) != null ? m.group(1).equals("[") : true;
        minVersion = m.group(2);
        maxVersion = m.group(3) != null ? m.group(3) : minVersion;
        maxIsInclusive = m.group(4) != null ? m.group(4).equals("]") : true;
      } else {
        throw new IllegalArgumentException("Unknown version string: " + versionString);
      }
    } else {
      throw new IllegalArgumentException("Unknown version string: " + versionString);
    }
  }

  public VersionRange(String minVersion, String maxVersion) {
    this(minVersion, maxVersion, true, false);
  }

  public VersionRange(String minVersion, String maxVersion, boolean minIsInclusive, boolean maxIsInclusive) {
    this.minVersion = minVersion;
    this.maxVersion = maxVersion;
    this.minIsInclusive = minIsInclusive;
    this.maxIsInclusive = maxIsInclusive;
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
    Version otherVersion = new Version(otherVersionStr);
    Version min = new Version(minVersion);
    Version max = new Version(maxVersion);
    
    int compareMin = otherVersion.compareTo(min);
    int compareMax = otherVersion.compareTo(max);

    boolean greaterThanMin = compareMin > 0 || (isMinInclusive() && compareMin == 0);
    boolean lessThanMax = compareMax < 0 || (isMaxInclusive() && compareMax == 0);
    
    return greaterThanMin && lessThanMax;
  }
}
