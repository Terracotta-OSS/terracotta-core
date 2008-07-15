/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import org.apache.commons.io.IOUtils;

import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This concrete implementation allows it's creator to set values while the appserver itself interacts with the
 * immutable {@link AppServerParameters} interface.
 */
public class StandardAppServerParameters implements AppServerParameters {

  private final Map        wars      = new HashMap();
  private final Collection sars      = new ArrayList();
  private final String     instanceName;
  private final Properties props;
  private String           jvmArgs   = "";
  private String           classpath = "";

  public StandardAppServerParameters(String instanceName, Properties props) {
    this.instanceName = instanceName;
    this.props = props;
  }

  public void addWar(String context, File file) {
    wars.put(context, file);
  }

  public final void addWar(File war) {
    String name = war.getName();
    addWar(name.substring(0, name.length() - 4), war);
  }

  public final void addSar(File sar) {
    sars.add(sar);
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

  public String writeTerracottaClassPathFile() {
    FileOutputStream fos = null;

    try {
      File tempFile = File.createTempFile("tc-classpath", instanceName);
      tempFile.deleteOnExit();
      fos = new FileOutputStream(tempFile);
      fos.write(System.getProperty("java.class.path").getBytes());

      String rv = tempFile.getAbsolutePath();
      if (Os.isWindows()) {
        rv = "/" + rv;
      }

      return rv;
    } catch (IOException ioe) {
      throw new AssertionError(ioe);
    } finally {
      IOUtils.closeQuietly(fos);
    }

  }

  public void appendSysProp(String name, int value) {
    appendSysProp(name, Integer.toString(value));
  }

  public void appendSysProp(String name, String value) {
    if (!name.startsWith("-")) {
      name = "-D" + name;
    }
    if (value == null) appendJvmArgs(name);
    else appendJvmArgs(name + "=" + value);
  }

  public void appendSysProp(String name) {
    appendSysProp(name, null);
  }

  public void appendSysProp(String name, boolean b) {
    appendSysProp(name, Boolean.toString(b));
  }

  public Collection sars() {
    return sars;
  }

}