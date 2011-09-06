/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerInfo {
  public static final int WEBLOGIC  = 0;
  public static final int JBOSS     = 1;
  public static final int TOMCAT    = 2;
  public static final int WASCE     = 3;
  public static final int GLASSFISH = 4;
  public static final int JETTY     = 5;
  public static final int RESIN     = 6;
  public static final int WEBSPHERE = 7;

  private final int       id;
  private final String    name;
  private final String    major;
  private final String    minor;

  private static final Pattern nameAndVersionPattern =
      Pattern.compile("^(\\w+)-(\\w+)\\.([\\w\\-\\.]+)$");

  /**
   * Creates a new {@link AppServerInfo} object whose properties are parsed from
   * the given <code>nameAndVersion> string, which must be of the form
   * name-major-version.minor-version.
   *
   * @throws IllegalArgumentException if the <code>nameAndVersion</code> does
   *         not parse properly.
   */
  public static AppServerInfo parse(final String nameAndVersion) {
    Matcher matcher = nameAndVersionPattern.matcher(nameAndVersion);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Cannot parse appserver specification: " + nameAndVersion);
    }
    return new AppServerInfo(matcher.group(1), matcher.group(2), matcher.group(3));
  }

  public AppServerInfo(String name, String majorVersion, String minorVersion) {
    this.name = name;
    this.major = majorVersion;
    this.minor = minorVersion;

    if (name == null)
      throw new RuntimeException("No appserver name has been set");

    if (name.equals("weblogic")) {
      id = WEBLOGIC;
    } else if (name.equals("jboss")) {
      id = JBOSS;
    } else if (name.equals("tomcat")) {
      id = TOMCAT;
    } else if (name.equals("wasce")) {
      id = WASCE;
    } else if (name.equals("glassfish")) {
      id = GLASSFISH;
    } else if (name.equals("jetty")) {
      id = JETTY;
    } else if (name.equals("resin")) {
      id = RESIN;
    } else if (name.equals("websphere")) {
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

  public String toString() {
    return name + "-" + major + "." + minor;
  }
}
