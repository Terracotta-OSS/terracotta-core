/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import org.apache.commons.io.output.TeeOutputStream;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.StreamCopier;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServerImpl;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.util.PortChooser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A quick/shallow test that uses the DSOVerifier client program
 */
public class DSOVerifierTest extends TCTestCase {

  TCServerImpl server;

  public static void main(String[] args) throws Exception {
    DSOVerifierTest t = new DSOVerifierTest();
    t.setUp();
    t.test();
    t.tearDown();
  }

  public DSOVerifierTest() {
    timebombTestForRewrite();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    File configFile = writeConfigFile();

    StandardConfigurationSetupManagerFactory config;
    config = new StandardConfigurationSetupManagerFactory(new String[] {
        StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, configFile.getAbsolutePath() },
                                                          StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                          new FatalIllegalConfigurationChangeHandler());

    server = new TCServerImpl(config.createL2TVSConfigurationSetupManager(null));
    server.start();
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
    super.tearDown();
  }

  private boolean verifyOutput(String output, String desiredPrefix) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(output));

    String line;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(desiredPrefix)) { return true; }
    }

    return false;
  }

  public void test() throws Exception {
    LinkedJavaProcess p1 = new LinkedJavaProcess(getMainClass(), Arrays.asList("1", "2"), getJvmArgs());

    LinkedJavaProcess p2 = new LinkedJavaProcess(getMainClass(), Arrays.asList("2", "1"), getJvmArgs());

    p1.start();
    p2.start();

    p1.getOutputStream().close();
    p2.getOutputStream().close();

    FileOutputStream p1o = new FileOutputStream(getTempFile("p1out.log"));
    FileOutputStream p1e = new FileOutputStream(getTempFile("p1err.log"));
    FileOutputStream p2o = new FileOutputStream(getTempFile("p2out.log"));
    FileOutputStream p2e = new FileOutputStream(getTempFile("p2err.log"));

    ByteArrayOutputStream p1oBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream p1eBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream p2oBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream p2eBytes = new ByteArrayOutputStream();

    StreamCopier p1StdOut = new StreamCopier(p1.getInputStream(), new TeeOutputStream(p1oBytes, p1o));
    StreamCopier p1StdErr = new StreamCopier(p1.getErrorStream(), new TeeOutputStream(p1eBytes, p1e));
    StreamCopier p2StdOut = new StreamCopier(p2.getInputStream(), new TeeOutputStream(p2oBytes, p2o));
    StreamCopier p2StdErr = new StreamCopier(p2.getErrorStream(), new TeeOutputStream(p2eBytes, p2e));
    p1StdOut.start();
    p1StdErr.start();
    p2StdOut.start();
    p2StdErr.start();

    p1.waitFor();
    p2.waitFor();

    p1StdOut.join();
    p1StdErr.join();
    p2StdOut.join();
    p2StdErr.join();

    String p1Out = new String(p1oBytes.toByteArray());
    String p1Err = new String(p1eBytes.toByteArray());
    String p2Out = new String(p2oBytes.toByteArray());
    String p2Err = new String(p2eBytes.toByteArray());

    String compound = "Process 1 output:\n\n" + p1Out + "\n\nProcess 1 error:\n\n" + p1Err
                      + "\n\nProcess 2 output:\n\n" + p2Out + "\n\nProcess 2 error:\n\n" + p2Err + "\n\n";

    String output = "L2-DSO-OK:";
    if (!verifyOutput(p1Out, output)) {
      fail(compound);
    }

    if (!verifyOutput(p2Out, output)) {
      fail(compound);
    }
  }

  protected String getMainClass() {
    return DSOVerifier.class.getName();
  }

  protected List<String> getJvmArgs() {
    String bootclasspath = "-Xbootclasspath/p:" + TestConfigObject.getInstance().normalBootJar();

    List<String> args = new ArrayList<String>();
    args.add(bootclasspath);
    args.add("-D" + ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME + "=localhost:"
             + server.getDSOListenPort());
    args.add("-D" + TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_BUFFER_RANDOM_SUFFIX_ENABLED) + "=true");
    args.add("-D" + TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_STORE_RANDOM_SUFFIX_ENABLED) + "=true");

    args.addAll(getExtraJvmArgs());

    System.out.println("JVM args: " + args);
    return args;
  }

  protected Collection<String> getExtraJvmArgs() {
    return Collections.emptyList();
  }

  protected boolean isSynchronousWrite() {
    return false;
  }

  private File writeConfigFile() throws IOException {
    TerracottaConfigBuilder config = TerracottaConfigBuilder.newMinimalInstance();

    PortChooser portChooser = new PortChooser();
    L2ConfigBuilder l2Builder = L2ConfigBuilder.newMinimalInstance();
    l2Builder.setDSOPort(portChooser.chooseRandomPort());
    l2Builder.setJMXPort(portChooser.chooseRandomPort());
    l2Builder.setL2GroupPort(portChooser.chooseRandomPort());

    config.getServers().setL2s(new L2ConfigBuilder[] { l2Builder });

    InstrumentedClassConfigBuilder instrumentedClass = new InstrumentedClassConfigBuilderImpl();
    instrumentedClass.setClassExpression("com.tcverify..*");
    config.getApplication().getDSO().setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumentedClass });

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_NAMED_LOCK);
    lock1.setMethodExpression("java.lang.Object com.tcverify.DSOVerifier.getValue(int)");
    lock1.setLockName("verifierMap");
    if (isSynchronousWrite()) {
      lock1.setLockLevel(LockConfigBuilder.LEVEL_SYNCHRONOUS_WRITE);
    } else {
      lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    }

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_NAMED_LOCK);
    lock2.setMethodExpression("void com.tcverify.DSOVerifier.setValue(int)");
    lock2.setLockName("verifierMap");
    if (isSynchronousWrite()) {
      lock2.setLockLevel(LockConfigBuilder.LEVEL_SYNCHRONOUS_WRITE);
    } else {
      lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    }

    config.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();

    root.setFieldName("com.tcverify.DSOVerifier.verifierMap");
    root.setRootName("verifierMap");

    config.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root });

    File configDir = this.getTempDirectory();
    File configFile = new File(configDir, "dso-verifier-config.xml");
    FileWriter cfgOut = new FileWriter(configFile);
    cfgOut.write(config.toString());
    cfgOut.flush();
    cfgOut.close();
    return configFile;
  }
}
