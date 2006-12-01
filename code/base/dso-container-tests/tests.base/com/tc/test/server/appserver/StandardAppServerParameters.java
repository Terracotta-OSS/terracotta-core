/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.test.server.tcconfig.TerracottaServerConfigGenerator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This concrete implementation allows it's creator to set values while the appserver itself interacts with the
 * immutable {@link AppServerParameters} interface.
 */
public class StandardAppServerParameters implements AppServerParameters {

  private final Map        wars      = new HashMap();
  private final String     instanceName;
  private final Properties props;
  private final String     tcSessionClasspath;
  private String           jvmArgs   = "";
  private String           classpath = "";

  public StandardAppServerParameters(String instanceName, Properties props, String tcSessionClasspath) {
    this.instanceName = instanceName;
    this.props = props;
    this.tcSessionClasspath = tcSessionClasspath;
  }

  public void addWar(String context, File file) {
    wars.put(context, file);
  }

  public final void addWar(File war) {
    String name = war.getName();
    addWar(name.substring(0, name.length() - 4), war);
  }

  public final String jvmArgs() {
    return jvmArgs;
  }

  public final void appendJvmArgs(String jvmArgsVar) {
    this.jvmArgs += jvmArgsVar + " ";
  }

  public final String classpath() {
    return classpath;
  }

  protected final void appendClasspath(String classpathVar) {
    this.classpath += classpathVar + " ";
  }

  public final Map wars() {
    return wars;
  }

  public final String instanceName() {
    return instanceName;
  }

  public final Properties properties() {
    return props;
  }

  public final void enableDSO(TerracottaServerConfigGenerator dsoConfig, File bootJar) {
    StringBuffer sb = new StringBuffer();
    sb.append("-Dtc.config='" + dsoConfig.configPath() + "'");
    sb.append(' ');
    sb.append("-Xbootclasspath/p:'" + bootJar + "'");
    sb.append(' ');
    sb.append("-Dtc.classpath='" + System.getProperty("java.class.path") + "'");
    sb.append(' ');
    sb.append("-Dtc.session.classpath='" + tcSessionClasspath + "'");
    appendJvmArgs(sb.toString());
  }

  public void appendSysProp(String name, int value) {
    appendSysProp(name, Integer.toString(value));
  }

  private void appendSysProp(String name, String value) {
    if (value == null) appendJvmArgs("-D" + name);
    else appendJvmArgs("-D" + name + "=" + value);
  }

  public void appendSysProp(String name) {
    appendSysProp(name, null);
  }

  public void appendSysProp(String name, boolean b) {
    appendSysProp(name, Boolean.toString(b));
  }

}