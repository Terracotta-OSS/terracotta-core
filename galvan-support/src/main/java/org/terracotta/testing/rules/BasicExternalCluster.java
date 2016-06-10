/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.rules;

import com.tc.util.PortChooser;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.passthrough.Assert;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.testing.logging.VerboseLogger;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.testing.master.IComponentManager;
import org.terracotta.testing.master.ReadyStripe;
import org.terracotta.testing.master.TestStateManager;

import static java.util.Collections.emptyList;

/**
 *
 * @author cdennis
 */
public class BasicExternalCluster extends Cluster {

  private final File clusterDirectory;
  private final int stripeSize;
  private final List<File> serverJars;
  private final String namespaceFragment;
  private final String serviceFragment;
  private final String entityFragment;

  private String displayName;
  private ReadyStripe cluster;
  private TestStateManager stateManager;
  // Note that the clientThread is actually the main thread of the JUnit runner.
  private final Thread clientThread;
  private Thread shepherdingThread;
  private boolean isSafe;

  public BasicExternalCluster(File clusterDirectory, int stripeSize) {
    this(clusterDirectory, stripeSize, emptyList(), "", "", "");
  }

  public BasicExternalCluster(File clusterDirectory, int stripeSize, List<File> serverJars, String namespaceFragment, String serviceFragment, String entityFragment) {
    if (clusterDirectory == null) {
      throw new NullPointerException("Cluster directory must be non-null");
    }
    if (stripeSize < 1) {
      throw new IllegalArgumentException("Must be at least one server in the cluster");
    }
    if (namespaceFragment == null) {
      throw new NullPointerException("Namespace fragment must be non-null");
    }
    if (serviceFragment == null) {
      throw new NullPointerException("Service fragment must be non-null");
    }
    clusterDirectory.mkdirs();
    this.clusterDirectory = clusterDirectory;
    this.stripeSize = stripeSize;
    this.namespaceFragment = namespaceFragment;
    this.serviceFragment = serviceFragment;
    this.entityFragment = entityFragment;
    this.serverJars = serverJars;
    
    this.clientThread = Thread.currentThread();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    this.displayName = description.getDisplayName();
    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    VerboseLogger harnessLogger = new VerboseLogger(System.out, null);
    VerboseLogger fileHelpersLogger = new VerboseLogger(null, null);
    VerboseLogger clientLogger = null;
    VerboseLogger serverLogger = new VerboseLogger(System.out, System.err);
    VerboseManager verboseManager = new VerboseManager("", harnessLogger, fileHelpersLogger, clientLogger, serverLogger);
    VerboseManager displayVerboseManager = verboseManager.createComponentManager("[" + displayName + "]");
    
    File serverInstallDirectory = new File(System.getProperty("kitInstallationPath"));
    File testParentDirectory = File.createTempFile(displayName, "", clusterDirectory);
    testParentDirectory.delete();
    testParentDirectory.mkdir();
    List<String> serverJarPaths = convertToStringPaths(serverJars);
    int serverPort = new PortChooser().chooseRandomPort();
    String debugPortString = System.getProperty("serverDebugStartPort");
    int serverDebugStartPort = (null != debugPortString)
        ? Integer.parseInt(debugPortString)
        : 0;

    stateManager = new TestStateManager();
    stateManager.addComponentToShutDown(new IComponentManager() {
      @Override
      public void forceTerminateComponent() {
        cluster.stripeControl.shutDown();
      }
    });
    cluster = ReadyStripe.configureAndStartStripe(stateManager, displayVerboseManager,
        serverInstallDirectory.getAbsolutePath(),
        testParentDirectory.getAbsolutePath(),
        stripeSize, serverPort, serverDebugStartPort, 0, false,
        serverJarPaths, namespaceFragment, serviceFragment, entityFragment);
    // Spin up an extra thread to call waitForFinish on the stateManager.
    // This is required since galvan expects that the client is running in a different thread (different process, usually)
    // than the framework, and the framework waits for the finish so that it can terminate the clients/servers if any of
    // them trigger an unexpected failure.
    // Without this, the client will hang in the case when the server crashes since nobody is running the logic to detect
    // that.
    Assert.assertTrue(null == this.shepherdingThread);
    this.shepherdingThread = new Thread(){
      @Override
      public void run() {
        setSafeForRun(true);
        boolean didPass = stateManager.waitForFinish();
        setSafeForRun(false);
        if (!didPass) {
          clientThread.interrupt();
        }
      }
    };
    this.shepherdingThread.setName("Shepherding Thread");
    this.shepherdingThread.start();
    waitForSafe();
  }

  @Override
  protected void after() {
    stateManager.testDidPass();
    // NOTE:  The waitForFinish is called by the shepherding thread so we just join on it having done that.
    try {
      this.shepherdingThread.join();
    } catch (InterruptedException e) {
      // We don't expect interruption in these tests.
      Assert.unexpected(e);
    }
    this.shepherdingThread = null;
  }

  @Override
  public URI getConnectionURI() {
    return URI.create(cluster.stripeUri);
  }

  @Override
  public Connection newConnection() throws ConnectionException {
    if (!checkSafe()) {
      throw new ConnectionException(null);
    }
    return ConnectionFactory.connect(getConnectionURI(), new Properties());
  }

  @Override
  public IClusterControl getClusterControl() {
    return new IClusterControl() {
      @Override
      public void restartActive() throws Exception {
        cluster.stripeControl.restartActive();
      }

      @Override
      public void waitForActive() throws Exception {
        cluster.stripeControl.waitForActive();
      }

      @Override
      public void waitForPassive() throws Exception {
        cluster.stripeControl.waitForPassive();
      }

      @Override
      public Connection createConnectionToActive() {
        try {
          return newConnection();
        } catch (ConnectionException ex) {
          throw new RuntimeException(ex);
        }
      }

      @Override
      public void tearDown() {
        cluster.stripeControl.shutDown();
      }
    };
  }

  private static List<String> convertToStringPaths(List<File> serverJars) {
    List<String> l = new ArrayList<>();
    for (File f : serverJars) {
      l.add(f.getAbsolutePath());
    }
    return l;
  }

  private synchronized void setSafeForRun(boolean isSafe) {
    // Note that this is called in 2 cases:
    // 1) To state that the shepherding thread is running and we can proceed.
    // 2) To state that there was a problem and we can't proceed.
    this.isSafe = isSafe;
    this.notifyAll();
  }

  private synchronized void waitForSafe() {
    boolean interrupted = false;
    while (!interrupted && !this.isSafe) {
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private synchronized boolean checkSafe() {
    return this.isSafe;
  }
}
