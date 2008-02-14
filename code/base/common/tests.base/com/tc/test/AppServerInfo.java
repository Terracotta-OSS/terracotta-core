/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public class AppServerInfo {
  public static final int WEBLOGIC  = 0;
  public static final int JBOSS     = 1;
  public static final int TOMCAT    = 2;
  public static final int WASCE     = 3;
  public static final int GLASSFISH = 4;
  public static final int JETTY     = 5;
  public static final int WEBSPHERE = 6;
  public static final int RESIN     = 7;

  private int             id        = -1;
  private String          name;
  private String          major;
  private String          minor;

  public int getId() {
    if (id >= 0) return id;
    if (name == null) throw new RuntimeException("No appserver name has been set");
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
    } else if (name.equals("websphere")) {
      id = WEBSPHERE;
    } else if (name.equals("resin")) {
      id = RESIN;
    } else {
      throw new RuntimeException("App server [" + name + "] is not yet defined!");
    }
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public String getMinor() {
    return minor;
  }

  public void setMinor(String minor) {
    this.minor = minor;
  }

  public String toString() {
    return name + "-" + major + "." + minor;
  }
}
