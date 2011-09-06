/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.test.TCTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CommandLineContainerBuilderConfigTest extends TCTestCase {

  CommandLineContainerBuilderConfig cfg;

  public void testMaster() throws Exception {
    newConfig(withRequiredArgs("--master"));
    cfg.parse();
    assertTrue(cfg.master());
  }

  public void testStartServer() throws Exception {
    newConfig(withRequiredArgs("--start-server"));
    cfg.parse();
    assertTrue(cfg.startServer());
  }

  public void testOutputToConsole() throws Exception {
    newConfig(withRequiredArgs("--output=console"));
    cfg.parse();
    assertTrue(cfg.outputToConsole());
  }

  public void testOutputToFile() throws Exception {
    String filename = "/tmp";
    File file = new File(filename);
    newConfig(withRequiredArgs("--output=" + filename));
    cfg.parse();
    assertTrue(cfg.outputToFile());
    assertEquals(file, cfg.outputFile());
  }

  public void testAppConfigBuilder() throws Exception {
    String classname = getClass().getName();
    newConfig(withRequiredArgs("--app-config-builder=" + classname));
    cfg.parse();
    assertEquals(classname, cfg.appConfigBuilder());
  }

  public void testGlobalParticipantCount() throws Exception {
    int count = 2;
    String[] args = withRequiredArgs("--global-participant-count=" + count);
    newConfig(args);
    cfg.parse();
    assertEquals(count, cfg.globalParticipantCount());
  }

  public void testApplicationClassname() throws Exception {
    String classname = "foobar";
    String[] args = withRequiredArgs("--application-classname="+classname);
    newConfig(args);
    cfg.parse();
    assertEquals(classname, cfg.applicationClassname());
  }
  
  public void testIntensity() throws Exception {
    int count = 5;
    String[] args = withRequiredArgs("--intensity="+count);
    newConfig(args);
    cfg.parse();
    assertEquals(count, cfg.intensity());
  }
  
  public void testGlobalContainerCount() throws Exception {
    int count = 2;
    String [] args = withRequiredArgs("--global-container-count=" + count);
    newConfig(args);
    cfg.parse();
    assertEquals(count, cfg.globalContainerCount());
  }
  
  public void testGetApplicationExecCount() throws Exception {
    int count = 2;
    newConfig(withRequiredArgs("--app-exec-count=" + count));
    cfg.parse();
    assertEquals(count, cfg.getApplicationExecutionCount());

    newConfig(withRequiredArgs());
    cfg.parse();
    assertEquals(1, cfg.getApplicationExecutionCount());
  }
  
  public void testGetContainerStartTimeout() throws Exception {
    long timeout = 2;
    newConfig(withRequiredArgs("--container-start-timeout=" + timeout));
    cfg.parse();
    assertEquals(2, cfg.getContainerStartTimeout());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertEquals(5 * 60 * 1000, cfg.getContainerStartTimeout());
  }
  
  public void testGetApplicationStartTimeout() throws Exception {
    long timeout = 2;
    newConfig(withRequiredArgs("--app-start-timeout=" + timeout));
    cfg.parse();
    assertEquals(timeout, cfg.getApplicationStartTimeout());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertEquals(5 * 60 * 1000, cfg.getApplicationStartTimeout());
  }
  
  public void testGetApplicationExecutionTimeout() throws Exception {
    long timeout = 2;
    newConfig(withRequiredArgs("--app-exec-timeout=" + timeout));
    cfg.parse();
    assertEquals(2, cfg.getApplicationExecutionTimeout());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertEquals(30 * 60 * 1000, cfg.getApplicationExecutionTimeout());
  }
  
  public void testDumpErrors() throws Exception {
    newConfig(withRequiredArgs("--dump-errors"));
    cfg.parse();
    assertTrue(cfg.dumpErrors());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertFalse(cfg.dumpErrors());
  }
  
  public void testDumpOutput() throws Exception {
    newConfig(withRequiredArgs("--dump-output"));
    cfg.parse();
    assertTrue(cfg.dumpOutput());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertFalse(cfg.dumpOutput());
  }
  
  public void testAggregate() throws Exception {
    newConfig(withRequiredArgs("--aggregate"));
    cfg.parse();
    assertTrue(cfg.aggregate());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertFalse(cfg.aggregate());
  }
  
  public void testStream() throws Exception {
    newConfig(withRequiredArgs("--stream"));
    cfg.parse();
    assertTrue(cfg.stream());
    
    newConfig(withRequiredArgs());
    cfg.parse();
    assertFalse(cfg.stream());
  }
  
  private void newConfig(String[] args) {
    this.cfg = new CommandLineContainerBuilderConfig(args);
  }
  
  private String[] withRequiredArgs() {
    return (String[]) requiredArgs.toArray(new String[requiredArgs.size()]);
  }

  private String[] withRequiredArgs(String arg) {
    List argList = new ArrayList();
    argList.add(arg);

    String argname = (arg.indexOf('=') >= 0) ? arg.substring(0, arg.indexOf('=')) : arg;
    for (Iterator i = requiredArgs.iterator(); i.hasNext();) {
      String required = (String) i.next();
      if (!required.startsWith(argname)) argList.add(required);
    }

    String[] rv = new String[argList.size()];
    argList.toArray(rv);
    return rv;
  }

  private static final Set requiredArgs = new HashSet();
  static {
    requiredArgs.add("--app-config-builder=alkdfj");
    requiredArgs.add("--global-participant-count=123");
    requiredArgs.add("--global-container-count=323");
  }

}