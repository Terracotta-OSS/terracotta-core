/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.tests.base;

import org.hamcrest.Matcher;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.test.util.TestProcessUtil;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.TestConfigObject;
import com.tc.test.config.model.TestConfig;
import com.tc.text.Banner;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.remote.jmxmp.JMXMPConnector;

import junit.framework.Assert;

public class TestClientManager {
  private static final String           STANDALONE_CLIENT_DEBUG_PROPERTY = "standalone.client.debug";

  /**
   * If set to true allows debugging of java applications
   */
  private static final boolean          DEBUG_CLIENTS                    = Boolean
                                                                             .getBoolean(STANDALONE_CLIENT_DEBUG_PROPERTY);

  /**
   * arguments to be passed to the clients. e.g mvn -Psystem-tests integration-test -Dtest=MyTest
   * -DclientJVMArgs="-DsomeProp=value1 -DsomeProp2=value2" In the spawned clients, these will be passed as JVMArgs
   * System.getProperty("someProp"); => will return value1 System.getProperty("someProp2"); => will return value2
   */
  public static final String            CLIENT_ARGS                      = "clientJVMArgs";

  private final AtomicInteger           clientIndex                      = new AtomicInteger(0);
  private final File                    tempDir;
  private final AbstractTestBase        testBase;
  private final TestConfig              testConfig;
  private final SetOnceFlag             stopped                          = new SetOnceFlag();
  private final List<LinkedJavaProcess> runningClients                   = new ArrayList<LinkedJavaProcess>();
  private final HashMap<Integer, LinkedJavaProcess> allClients                       = new HashMap<Integer, LinkedJavaProcess>();
  private volatile Throwable            exceptionFromClient;
  private final PauseManager                        pauseManager;

  public TestClientManager(final File tempDir, final AbstractTestBase testBase, final TestConfig testConfig,
                           final PauseManager pauseManager) {
    this.testConfig = testConfig;
    this.tempDir = tempDir;
    this.testBase = testBase;
    this.pauseManager = pauseManager;
  }

  /**
   * Starts a new client
   * 
   * @param client : the class which is to be started as client
   * @param clientName name of : the client to be started
   * @param extraClientMainArgs : List of arguments with which the client will start
   */
  public void runClient(Class<? extends Runnable> client, String clientName, List<String> extraClientMainArgs)
      throws Throwable {
    synchronized (TestClientManager.class) {
      if (stopped.isSet()) { return; }
    }
    ArrayList<String> jvmArgs = new ArrayList<String>();
    int debugPortOffset = clientIndex.getAndIncrement();
    int localClientIndex = debugPortOffset;
    if (shouldDebugClient(debugPortOffset)) {
      int debugPort = 9000 + debugPortOffset;
      jvmArgs.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
      Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
    }

    if (testConfig.getClientConfig().shouldResolveLicense()) {
      File licenseKey = new File("test-classes" + File.separator + "terracotta-license.key");
      jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.PRODUCTKEY_PATH + "=" + licenseKey.getAbsolutePath());
    }

    // do this last
    configureClientExtraJVMArgs(jvmArgs);

    // removed duplicate args and use the one added in the last in case of multiple entries
    TestBaseUtil.removeDuplicateJvmArgs(jvmArgs);
    TestBaseUtil.setHeapSizeArgs(jvmArgs, testConfig.getClientConfig().getMinHeap(), testConfig.getClientConfig()
        .getMaxHeap(), testConfig.getClientConfig().getDirectMemorySize(), false);
    testConfig.getClientConfig().getBytemanConfig().addTo(jvmArgs, tempDir);

    List<String> clientMainArgs = new ArrayList<String>();
    clientMainArgs.add(client.getName());
    clientMainArgs.add(Integer.toString(localClientIndex));
    clientMainArgs.add(Integer.toString(testBase.getTestControlMbeanPort()));
    clientMainArgs.addAll(extraClientMainArgs);

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
    TestBaseUtil.setupVerboseGC(jvmArgs, verboseGcOutputFile);

    LinkedJavaProcess clientProcess = new LinkedJavaProcess(TestClientLauncher.class.getName(), clientMainArgs, jvmArgs);
    clientProcess.setMaxRuntime(TestConfigObject.getInstance().getJunitTimeoutInSeconds());
    String classPath = testBase.createClassPath(client);
    classPath = testBase.makeClasspath(classPath, testBase.getTestDependencies());
    classPath = addRequiredJarsToClasspath(client, classPath);
    classPath = addExtraJarsToClassPath(classPath);
    clientProcess.setClasspath(classPath);

    System.err.println("\nStarting client with jvmArgs: " + jvmArgs);
    System.err.println("\nLinkedJavaProcess main method arguments: " + clientMainArgs);
    System.err.println("\nLinkedJavaProcess classpath: " + scrubClassPath(classPath) + "\n");

    clientProcess.setDirectory(workDir);

    testBase.preStart(workDir);
    synchronized (TestClientManager.class) {
      if (stopped.isSet()) { return; }
      runningClients.add(clientProcess);
      allClients.put(localClientIndex, clientProcess);
      clientProcess.start();
      pauseManager.startPauseConfig(localClientIndex);
    }

    Result result = Exec.execute(clientProcess, clientProcess.getCommand(), output.getAbsolutePath(), null, workDir);
    synchronized (TestClientManager.class) {
      if (stopped.isSet()) { return; }
      runningClients.remove(clientProcess);
      allClients.remove(localClientIndex);
      try {
        testBase.evaluateClientOutput(client.getName(), result.getExitCode(), output);
      } catch (Throwable t) {
        System.out.println("*************Got exception in One of the Clients Killing other clients");
        System.out.println("**** For Details Refer to client Logs at " + output.getAbsolutePath());
        stopAllClients();
        if (exceptionFromClient != null) {
          throw new AssertionError(exceptionFromClient);
        } else {
          throw new AssertionError(t);
        }
      }
      pauseManager.stopPauseConfig(localClientIndex);
    }
  }

  private String scrubClassPath(String classPath) {
    Set<String> cp = new LinkedHashSet<String>();
    for (String entry : classPath.split(File.pathSeparator)) {
      entry = entry.trim();
      if (entry.length() > 0) {
        if (entry.contains(".m2") && entry.endsWith(".jar")) {
          cp.add(new File(entry).getName());
        } else {
          cp.add(entry);
        }
      }
    }
    return cp.toString();
  }

  private boolean shouldDebugClient(int debugPortOffset) {
    return DEBUG_CLIENTS || Boolean.getBoolean(STANDALONE_CLIENT_DEBUG_PROPERTY + "." + debugPortOffset);
  }

  private String addExtraJarsToClassPath(String classPath) {
    for (String extraJar : testBase.getExtraJars()) {
      classPath = testBase.addToClasspath(classPath, extraJar);
    }
    return classPath;
  }

  private String addRequiredJarsToClasspath(Class client, String classPath) {
    String test = TestBaseUtil.jarFor(client);
    String junit = TestBaseUtil.jarFor(org.junit.Assert.class);
    String linkedChild = TestBaseUtil.jarFor(LinkedJavaProcess.class);
    String abstractClientBase = TestBaseUtil.jarFor(AbstractClientBase.class);
    String jmxp = TestBaseUtil.jarFor(JMXMPConnector.class);
    String log4j = TestBaseUtil.jarFor(org.apache.log4j.LogManager.class);
    String hamcrest = TestBaseUtil.jarFor(Matcher.class);
    classPath = testBase.makeClasspath(classPath, test, junit, linkedChild, abstractClientBase, jmxp, log4j, hamcrest);
    return classPath;
  }

  private void configureClientExtraJVMArgs(List<String> jvmArgs) {
    jvmArgs.addAll(testConfig.getClientConfig().getExtraClientJvmArgs());
    // JVM Args Specified through command line using -DclientJVMArgs="args"
    String clientArgs = System.getProperty(CLIENT_ARGS);
    if (clientArgs != null) {
      jvmArgs.add(clientArgs);
    }
  }

  synchronized void stopAllClients() {
    if (stopped.attemptSet()) {
      for (LinkedJavaProcess client : runningClients) {
        try {
          client.destroy();
        } catch (IllegalStateException e) {
          System.out.println("Got an IllegalStateException stopping a client. Msg - " + e.getMessage());
        }
      }
    }
  }

  synchronized void stopClient(final int index) {
    Assert.assertTrue("index: " + index + " no of running clients: " + this.runningClients.size(),
                      index < this.runningClients.size());
    this.runningClients.get(index).destroy();
  }

  public void clientExitedWithException(Throwable t) {
    exceptionFromClient = t;
  }

  public int getClientCount() {
    return clientIndex.get();
  }

  public void pauseProcess(int index, long pauseTimeMillis) throws InterruptedException {
    LinkedJavaProcess process = getClientProcess(index);
    if (process == null) { return; }
    TestProcessUtil.pauseProcess(process, pauseTimeMillis);
  }
  
  public void pauseClient(int index) throws InterruptedException {
    LinkedJavaProcess process = getClientProcess(index);
    if (process == null) { return; }
    TestProcessUtil.pauseProcess(process);
  }

  public void unpauseClient(int index) throws InterruptedException {
    LinkedJavaProcess process = getClientProcess(index);
    if (process == null) { return; }
    TestProcessUtil.unpauseProcess(process);
  }

  private synchronized LinkedJavaProcess getClientProcess(int index) {
    LinkedJavaProcess javaProcess = allClients.get(index);
    return javaProcess;
  }

  public File getTempDir() {
    return tempDir;
  }

}
