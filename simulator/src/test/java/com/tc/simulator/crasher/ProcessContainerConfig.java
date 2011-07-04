/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.crasher;

import java.io.File;
import java.text.DateFormat;
import java.util.Collection;

public class ProcessContainerConfig {
  private final String     id;
  private final Collection serverArgs;
  private final String     classname;
  private final Collection mainClassArgs;
  private final File       outputDirectory;
  private final String     outputPrefix;
  private final DateFormat dateFormat;

  public ProcessContainerConfig(String id, DateFormat dateFormat, Collection serverArgs, String classname, Collection mainClassArgs,
                                File outputDirectory, String outputPrefix) {
    this.id = id;
    this.dateFormat = dateFormat;
    this.serverArgs = serverArgs;
    this.classname = classname;
    this.mainClassArgs = mainClassArgs;
    this.outputDirectory = outputDirectory;
    this.outputPrefix = outputPrefix;
  }

  public Collection getServerArgs() {
    return serverArgs;
  }

  public String getClassname() {
    return classname;
  }

  public Collection getMainClassArgs() {
    return mainClassArgs;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public String getOutputPrefix() {
    return outputPrefix;
  }

  public String getID() {
    return id;
  }

  public DateFormat getDateFormat() {
    return dateFormat;
  }
}