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
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseLogger;
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

  private String displayName;
  private ReadyStripe cluster;
  private TestStateManager stateManager;

  public BasicExternalCluster(File clusterDirectory, int stripeSize) {
    this(clusterDirectory, stripeSize, emptyList(), "", "");
  }

  public BasicExternalCluster(File clusterDirectory, int stripeSize, List<File> serverJars, String namespaceFragment, String serviceFragment) {
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
    this.serverJars = serverJars;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    this.displayName = description.getDisplayName();
    return super.apply(base, description);
  }

  @Override
  protected void before() throws Throwable {
    ContextualLogger stripeLogger = new ContextualLogger(new VerboseLogger(System.out, null), "[" + displayName + "]");
    ContextualLogger fileLogger = new ContextualLogger(new VerboseLogger(null, null), "[" + displayName + "]");
    File serverInstallDirectory = new File(System.getProperty("kitInstallationPath"));
    File testParentDirectory = File.createTempFile(displayName, "", clusterDirectory);
    testParentDirectory.delete();
    testParentDirectory.mkdir();
    List<String> serverJarPaths = convertToStringPaths(serverJars);
    int serverPort = new PortChooser().chooseRandomPort();

    stateManager = new TestStateManager();
    cluster = ReadyStripe.configureAndStartStripe(stateManager, stripeLogger, fileLogger,
            serverInstallDirectory.getAbsolutePath(),
            testParentDirectory.getAbsolutePath(),
            stripeSize, serverPort, 0, false,
            serverJarPaths, namespaceFragment, serviceFragment);

    stateManager.addComponentToShutDown(new IComponentManager() {
      @Override
      public void forceTerminateComponent() {
        cluster.stripeControl.shutDown();
      }
    });
  }

  @Override
  protected void after() {
    stateManager.testDidPass();
    if (!stateManager.waitForFinish()) {
      throw new AssertionError("Test tear down failure");
    }
  }

  @Override
  public URI getConnectionURI() {
    return URI.create(cluster.stripeUri);
  }

  @Override
  public Connection newConnection() throws ConnectionException {
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
}
