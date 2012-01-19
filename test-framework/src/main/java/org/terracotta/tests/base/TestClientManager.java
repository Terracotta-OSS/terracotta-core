package org.terracotta.tests.base;

import org.apache.commons.lang.StringUtils;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.config.model.TestConfig;
import com.tc.text.Banner;
import com.tc.timapi.Version;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.management.remote.jmxmp.JMXMPConnector;

public class TestClientManager {
  private static final boolean   DEBUG_CLIENTS = Boolean.getBoolean("standalone.client.debug");
  public static String           CLIENT_ARGS   = "client.args";

  private volatile int           clientIndex   = 1;
  private final File             tempDir;
  private final AbstractTestBase testBase;
  private final TestConfig       testConfig;

  public TestClientManager(final File tempDir, final AbstractTestBase testBase, final TestConfig testConfig) {
    this.testConfig = testConfig;
    this.tempDir = tempDir;
    this.testBase = testBase;
  }

  protected void runClient(Class<? extends Runnable> client, boolean withStandaloneJar, String clientName,
                           List<String> extraClientArgs) throws Throwable {

    ArrayList<String> jvmArgs = new ArrayList<String>();
    if (DEBUG_CLIENTS) {
      int debugPort = 9000 + (clientIndex++);
      jvmArgs.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
      Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
    }
    String clientArgs = System.getProperty(CLIENT_ARGS);
    if (clientArgs != null) {
      extraClientArgs.add(clientArgs);
    }

    File licenseKey = new File("test-classes/terracotta-license.key");
    jvmArgs.add("-Dcom.tc.productkey.path=" + licenseKey.getAbsolutePath());

    List<String> arguments = new ArrayList<String>();
    arguments.add(client.getName());
    arguments.add(Integer.toString(testBase.getTestControlMbeanPort()));
    arguments.addAll(extraClientArgs);

    // do this last
    configureClientExtraJVMArgs(jvmArgs);

    // removed duplicate args and use the one added in the last in case of multiple entries
    TestBaseUtil.removeDuplicateJvmArgs(jvmArgs);

    String workDirPath = tempDir + File.separator + clientName;
    File workDir;
    synchronized (TestClientManager.class) {
      workDir = new File(workDirPath);
      if (workDir.exists()) {
        int index = 0;
        do {
          String newWorkDirPath = workDirPath + "-" + index;
          System.err.println("Work directory already exists, trying: " + newWorkDirPath);
          workDir = new File(newWorkDirPath);
          index++;
        } while (workDir.exists());
      }
      workDir.mkdirs();
    }
    File output = new File(workDir, clientName + ".log");
    System.out.println("XXX client output file: " + output.getAbsolutePath());
    System.out.println("XXX working directory: " + workDir.getAbsolutePath());

    File verboseGcOutputFile = new File(workDir, "verboseGC.log");
    setupVerboseGC(jvmArgs, verboseGcOutputFile);

    LinkedJavaProcess clientProcess = new LinkedJavaProcess(TestClientLauncher.class.getName(), arguments, jvmArgs);
    String classPath = testBase.createClassPath(client, withStandaloneJar);
    classPath = testBase.makeClasspath(classPath, testBase.getTestDependencies());
    classPath = addRequiredJarsToClasspath(client, classPath);
    classPath = addExtraJarsToClassPath(classPath);
    clientProcess.setClasspath(classPath);

    System.err.println("Starting client with jvmArgs: " + jvmArgs);
    System.err.println("LinkedJavaProcess arguments: " + arguments);
    System.err.println("LinkedJavaProcess classpath: " + classPath);

    clientProcess.setDirectory(workDir);

    testBase.preStart(workDir);

    clientProcess.start();
    Result result = Exec.execute(clientProcess, clientProcess.getCommand(), output.getAbsolutePath(), null, workDir);

    testBase.evaluateClientOutput(client.getName(), result.getExitCode(), output);
  }

  private String addExtraJarsToClassPath(String classPath) {
    for (String extraJar : testBase.getExtraJars()) {
      classPath = testBase.addToClasspath(extraJar, classPath);
    }

    return classPath;
  }

  private String addRequiredJarsToClasspath(Class client, String classPath) {
    String mbsp = TestBaseUtil.jarFor(MBeanServerInvocationProxy.class);
    String test = TestBaseUtil.jarFor(client);
    String junit = TestBaseUtil.jarFor(org.junit.Assert.class);
    String linkedChild = TestBaseUtil.jarFor(LinkedJavaProcess.class);
    String abstractClientBase = TestBaseUtil.jarFor(AbstractClientBase.class);
    String jmxp = TestBaseUtil.jarFor(JMXMPConnector.class);
    String log4j = TestBaseUtil.jarFor(org.apache.log4j.LogManager.class);
    String stringUtils = TestBaseUtil.jarFor(StringUtils.class);
    String timApi = TestBaseUtil.jarFor(Version.class);
    classPath = testBase.makeClasspath(classPath, mbsp, test, junit, linkedChild, abstractClientBase, jmxp, log4j,
                                       stringUtils, timApi);
    return classPath;
  }

  private void configureClientExtraJVMArgs(List<String> jvmArgs) {
    jvmArgs.addAll(testConfig.getClientConfig().getExtraClientJvmArgs());
  }

  private void setupVerboseGC(List<String> jvmArgs, File verboseGcOutputFile) {
    if (Vm.isJRockit()) {
      jvmArgs.add("-Xverbose:gcpause,gcreport");
      jvmArgs.add("-Xverboselog:" + verboseGcOutputFile.getAbsolutePath());
    } else {
      jvmArgs.add("-Xloggc:" + verboseGcOutputFile.getAbsolutePath());
      jvmArgs.add("-XX:+PrintGCTimeStamps");
      jvmArgs.add("-XX:+PrintGCDetails");
    }
  }

}
