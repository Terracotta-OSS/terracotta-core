/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import java.io.File;

public interface ContainerBuilderConfig {

  public String applicationClassname();
  
  public int globalParticipantCount();

  public int globalContainerCount();
  
  public boolean startServer();

  public boolean master();

  public String appConfigBuilder();

  public boolean outputToConsole();

  public boolean outputToFile();

  public File outputFile();

  public int getApplicationExecutionCount();

  public long getContainerStartTimeout();

  public long getApplicationStartTimeout();

  public long getApplicationExecutionTimeout();

  /**
   * Dump errors to a local output resource.
   */
  public boolean dumpErrors();

  /**
   * Dump output to a local output resource.
   */
  public boolean dumpOutput();

  /**
   * Aggregate output for batch processing at the end of the application run.
   */
  public boolean aggregate();

  /**
   * Stream output immediately.
   */
  public boolean stream();
  
  public int intensity();
  
}