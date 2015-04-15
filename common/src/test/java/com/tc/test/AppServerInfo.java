/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerInfo {
  public static final int      WEBLOGIC              = 0;
  public static final int      JBOSS                 = 1;
  public static final int      TOMCAT                = 2;
  public static final int      WASCE                 = 3;
  public static final int      GLASSFISH             = 4;
  public static final int      JETTY                 = 5;
  public static final int      RESIN                 = 6;
  public static final int      WEBSPHERE             = 7;

  private final int            id;
  private final String         name;
  private final String         major;
  private final String         minor;

  private static final Pattern nameAndVersionPattern = Pattern.compile("^(.+)-(\\d+)\\.(.+)$");

  /**
   * Creates a new {@link AppServerInfo} object whose properties are parsed from the given
   * <code>nameAndVersion> string, which must be of the form
   * name-major-version.minor-version.
   * 
   * @throws IllegalArgumentException if the <code>nameAndVersion</code> does not parse properly.
   */
  public static AppServerInfo parse(final String nameAndVersion) {
    Matcher matcher = nameAndVersionPattern.matcher(nameAndVersion);
    if (!matcher.matches()) { throw new IllegalArgumentException("Cannot parse appserver specification: "
                                                                 + nameAndVersion); }
    return new AppServerInfo(matcher.group(1), matcher.group(2), matcher.group(3));
  }

  public AppServerInfo(String name, String majorVersion, String minorVersion) {
    this.name = name;
    this.major = majorVersion;
    this.minor = minorVersion;

    if (name == null) throw new RuntimeException("No appserver name has been set");

    if (name.startsWith("weblogic")) {
      id = WEBLOGIC;
    } else if (name.startsWith("jboss")) {
      id = JBOSS;
    } else if (name.startsWith("tomcat")) {
      id = TOMCAT;
    } else if (name.startsWith("wasce")) {
      id = WASCE;
    } else if (name.startsWith("glassfish")) {
      id = GLASSFISH;
    } else if (name.startsWith("jetty")) {
      id = JETTY;
    } else if (name.startsWith("resin")) {
      id = RESIN;
    } else if (name.startsWith("websphere")) {
      id = WEBSPHERE;
    } else {
      id = -1;
    }
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getMajor() {
    return major;
  }

  public String getMinor() {
    return minor;
  }

  @Override
  public String toString() {
    return name + "-" + major + "." + minor;
  }
}
