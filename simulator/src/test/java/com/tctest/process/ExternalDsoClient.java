/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.process;

import org.apache.commons.io.IOUtils;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.StreamCopier;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TestConfigObject;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will start a DSO client out of process
 * 
 * @author hhuynh
 */
public class ExternalDsoClient {
  private static final String CLIENT_CONFIG_FILENAME = "client-config.xml";

  private final File          clientLog;
  private final File          configFile;
  private final List          jvmArgs                = new ArrayList();
  private final List          args                   = new ArrayList();
  private final File          workingDir;
  private final Class         clientClass;
  private final String        clientName;
  private FileOutputStream    logOutputStream;
  private LinkedJavaProcess   process;

  public ExternalDsoClient(String clientName, File workingDir, InputStream configInput, Class clientClass) {
    this.workingDir = workingDir;
    this.clientLog = new File(workingDir, "dso-client.log");
    this.clientClass = clientClass;
    this.clientName = clientName;
    try {
      this.configFile = saveToFile(configInput);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    prepareTCJvmArgs();
  }

  protected void addProductKeyIfExists(List vmargs) {
    String propertyKey = TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.PRODUCTKEY_PATH;
    String productKeyPath = System.getProperty(propertyKey);
    if (productKeyPath != null) {
      vmargs.add("-D" + propertyKey + "=" + productKeyPath);
    }
  }
  
  private File saveToFile(InputStream configInput) throws IOException {
    File config = new File(workingDir, CLIENT_CONFIG_FILENAME);
    FileOutputStream out = new FileOutputStream(config);
    IOUtils.copy(configInput, out);
    out.close();
    return config;
  }

  public int startAndWait() throws IOException, InterruptedException {
    start();
    return process.waitFor();
  }

  public void start() throws IOException {
    logOutputStream = new FileOutputStream(clientLog);
    process = createLinkedJavaProcess();
    process.addAllJvmArgs(jvmArgs);
    process.start();
    StreamCopier outCopier = new StreamCopier(process.STDOUT(), logOutputStream);
    StreamCopier errCopier = new StreamCopier(process.STDERR(), logOutputStream);
    outCopier.start();
    errCopier.start();
    System.out.println(toString() + " started");
  }

  private LinkedJavaProcess createLinkedJavaProcess() {
    LinkedJavaProcess p = new LinkedJavaProcess(clientClass.getName(),  args);
    p.setDirectory(workingDir);
    return p;
  }

  public void stop() throws Exception {
    Assert.assertNotNull(process);
    Assert.assertNotNull(logOutputStream);
    if (isRunning()) {
      process.destroy();
    }
    if (isRunning()) {
      System.err.println(" WARNING: Dso client " + toString() + " process is still running after calling destroy()");
    }
    System.err.println("DSO client " + toString() + " stopped");
    IOUtils.closeQuietly(logOutputStream);
  }

  public boolean isRunning() {
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }

  public int exitValue() {
    return process.exitValue();
  }

  public String getClientName() {
    return clientName;
  }

  public File getClientLog() {
    return clientLog;
  }

  public File getConfigFile() {
    return configFile;
  }

  public String toString() {
    return "DSO client " + (clientName != null ? clientName : clientClass.getName());
  }

  public List getJvmArgs() {
    return jvmArgs;
  }

  public List getArgs() {
    return args;
  }

  public void addJvmArg(String jvmarg) {
    jvmArgs.add(jvmarg);
  }

  public void addArg(String arg) {
    args.add(arg);
  }

  private void prepareTCJvmArgs() {
    try {
      String bootclasspath = "-Xbootclasspath/p:" + TestConfigObject.getInstance().normalBootJar();
      this.jvmArgs.add("-Dtc.classpath=" + createTcClassPath());
      this.jvmArgs.add(bootclasspath);
      this.jvmArgs.add("-Dtc.config=" + configFile);
      addProductKeyIfExists(jvmArgs);
    } catch (Exception e) {
      throw Assert.failure("Can't set JVM args", e);
    }
  }

  private String createTcClassPath() {
    File tcClassPathFile = new File(workingDir, "tc.classpath." + this.hashCode() + ".txt");
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(tcClassPathFile);
      IOUtils.write(System.getProperty("java.class.path"), fos);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(fos);
    }
    return tcClassPathFile.toURI().toString();
  }
}
