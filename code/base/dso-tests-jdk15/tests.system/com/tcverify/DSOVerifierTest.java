/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcverify;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.process.LinkedJavaProcess;
import com.tc.process.StreamCollector;
import com.tc.server.TCServerImpl;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.util.PortChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

/**
 * A quick/shallow test that uses the DSOVerifier client program
 */
public class DSOVerifierTest extends TCTestCase {

  TCServerImpl server;

  public final static void main(String[] args) throws Exception {
    DSOVerifierTest t = new DSOVerifierTest();
    t.setUp();
    t.test();
    t.tearDown();
  }

  public DSOVerifierTest() {
    disableAllUntil("2007-09-11");    
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    File configFile = writeConfigFile();

    StandardTVSConfigurationSetupManagerFactory config;
    config = new StandardTVSConfigurationSetupManagerFactory(new String[] {
        StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, configFile.getAbsolutePath() }, true,
                                                             new FatalIllegalConfigurationChangeHandler());

    server = new TCServerImpl(config.createL2TVSConfigurationSetupManager(null));
    server.start();
  }

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
    String bootclasspath = "-Xbootclasspath/p:" + TestConfigObject.getInstance().normalBootJar();

    System.out.println("Bootclasspath:" + bootclasspath);

    String[] jvmArgs = new String[] {
        bootclasspath,
        "-D" + TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME + "=localhost:"
            + server.getDSOListenPort() };
    System.out.println("JVM args: " + ArrayUtils.toString(jvmArgs));

    LinkedJavaProcess p1 = new LinkedJavaProcess(DSOVerifier.class.getName(), new String[] { "1", "2" });
    p1.setJavaArguments(jvmArgs);
    p1.setDSOTarget(true);

    LinkedJavaProcess p2 = new LinkedJavaProcess(DSOVerifier.class.getName(), new String[] { "2", "1" });
    p2.setJavaArguments(jvmArgs);
    p2.setDSOTarget(true);

    p1.start();
    p2.start();

    p1.getOutputStream().close();
    p2.getOutputStream().close();

    StreamCollector p1StdOut = new StreamCollector(p1.getInputStream());
    StreamCollector p2StdOut = new StreamCollector(p2.getInputStream());
    StreamCollector p1StdErr = new StreamCollector(p1.getErrorStream());
    StreamCollector p2StdErr = new StreamCollector(p2.getErrorStream());
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

    String p1Out = p1StdOut.toString();
    String p2Out = p2StdOut.toString();
    String p1Err = p1StdErr.toString();
    String p2Err = p2StdErr.toString();

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

  private File writeConfigFile() throws IOException {
    TerracottaConfigBuilder config = TerracottaConfigBuilder.newMinimalInstance();

    PortChooser portChooser = new PortChooser();
    L2ConfigBuilder l2Builder = L2ConfigBuilder.newMinimalInstance();
    l2Builder.setDSOPort(portChooser.chooseRandomPort());
    l2Builder.setJMXPort(portChooser.chooseRandomPort());
    l2Builder.setName("localhost");

    config.getServers().setL2s(new L2ConfigBuilder[] { l2Builder });

    InstrumentedClassConfigBuilder instrumentedClass = new InstrumentedClassConfigBuilderImpl();
    instrumentedClass.setClassExpression("com.tcverify..*");
    config.getApplication().getDSO().setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumentedClass });

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_NAMED_LOCK);
    lock1.setMethodExpression("java.lang.Object com.tcverify.DSOVerifier.getValue(int)");
    lock1.setLockName("verifierMap");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_NAMED_LOCK);
    lock2.setMethodExpression("void com.tcverify.DSOVerifier.setValue(int)");
    lock2.setLockName("verifierMap");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

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
