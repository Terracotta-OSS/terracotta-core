/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.rules;

import com.tc.util.Assert;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.testing.config.StartupCommandBuilder;
import org.terracotta.testing.config.StripeConfiguration;
import org.terracotta.testing.config.TcConfigBuilder;
import org.terracotta.testing.logging.VerboseLogger;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.testing.master.GalvanFailureException;
import org.terracotta.testing.master.ReadyStripe;
import org.terracotta.testing.master.TestStateManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import org.terracotta.server.Server;

import org.terracotta.testing.common.MultiplexedEventingStream;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.master.FileHelpers;
import org.terracotta.testing.master.InlineStateInterlock;
import org.terracotta.testing.master.InlineStripeInstaller;
import org.terracotta.testing.support.PortTool;
import org.terracotta.utilities.test.net.PortManager;

/**
 * @author cdennis
 */
class BasicInlineCluster extends Cluster {

  private final Path clusterDirectory;
  private final int stripeSize;
  private final Set<Path> serverJars;
  private final String namespaceFragment;
  private final String serviceFragment;
  private final int clientReconnectWindow;
  private final int voterCount;
  private final boolean consistentStart;
  private final Properties tcProperties = new Properties();
  private final Properties systemProperties = new Properties();
  private final String logConfigExt;
  private final int serverHeapSize;
  private final Supplier<StartupCommandBuilder> startupBuilder;
  private final PortManager allocator = PortManager.getInstance();

  private String displayName;
  private ReadyStripe cluster;
  private InlineStateInterlock interlock;
  private TestStateManager stateManager;
  // Note that the clientThread is actually the main thread of the JUnit runner.
  private final Thread clientThread;
  // We keep a flag to describe whether or not we are currently trying to interrupt the clientThread during what is
  // probably its join on shepherdingThread (as that can be ignored).
  private volatile boolean isInterruptingClient;
  private Thread shepherdingThread;
  private boolean isSafe;

  BasicInlineCluster(Path clusterDirectory, int stripeSize, Set<Path> serverJars, String namespaceFragment,
                       String serviceFragment, int clientReconnectWindow, int voterCount, boolean consistentStart, Properties tcProperties,
                       Properties systemProperties, String logConfigExt, int serverHeapSize, Supplier<StartupCommandBuilder> startupBuilder) {
    boolean didCreateDirectories = clusterDirectory.toFile().mkdirs();
    if (Files.exists(clusterDirectory)) {
      if (Files.isRegularFile(clusterDirectory)) {
        throw new IllegalArgumentException("Cluster directory is a file: " + clusterDirectory);
      }
    } else if (!didCreateDirectories) {
      throw new IllegalArgumentException("Cluster directory could not be created: " + clusterDirectory);
    }

    this.clusterDirectory = clusterDirectory;
    this.stripeSize = stripeSize;
    this.namespaceFragment = namespaceFragment;
    this.serviceFragment = serviceFragment;
    this.serverJars = serverJars;
    this.clientReconnectWindow = clientReconnectWindow;
    this.voterCount = voterCount;
    this.consistentStart = consistentStart;
    this.tcProperties.putAll(tcProperties);
    this.systemProperties.putAll(systemProperties);
    this.logConfigExt = logConfigExt;
    this.serverHeapSize = serverHeapSize;
    this.startupBuilder = startupBuilder;
    this.clientThread = Thread.currentThread();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    String methodName = description.getMethodName();
    Class testClass = description.getTestClass();
    if (methodName == null) {
      if (testClass == null) {
        this.displayName = description.getDisplayName();
      } else {
        this.displayName = testClass.getSimpleName();
      }
    } else if (testClass == null) {
      this.displayName = description.getDisplayName();
    } else {
      this.displayName = testClass.getSimpleName() + "-" + methodName;
    }
    return super.apply(base, description);
  }

  public CompletionStage<Void> manualStart(String displayName) {
    this.displayName = displayName;
    CompletableFuture<Void> f = new CompletableFuture<>();
    try {
      internalStart(f);
    } catch (Throwable t) {
      f.completeExceptionally(t);
    }
    return f;
  }

  @Override
  protected void before() throws Throwable {
    internalStart(new CompletableFuture<>());
  }

  @Override
  public TestManager getTestManager() {
    return new TestManager() {
      @Override
      public void testFinished() {
        stateManager.setTestDidPassIfNotFailed();
      }

      @Override
      public void testDidFail(GalvanFailureException failure) {
        stateManager.testDidFail(failure);
      }

      @Override
      public boolean isComplete() throws GalvanFailureException {
        return stateManager.checkDidPass();
      }
    };
  }

  private void internalStart(CompletableFuture<Void> checker) throws Throwable {
    VerboseLogger harnessLogger = new VerboseLogger(System.out, null);
    VerboseLogger fileHelpersLogger = new VerboseLogger(null, null);
    VerboseLogger clientLogger = null;
    VerboseLogger serverLogger = new VerboseLogger(System.out, System.err);
    VerboseManager verboseManager = new VerboseManager("", harnessLogger, fileHelpersLogger, clientLogger, serverLogger);
    VerboseManager displayVerboseManager = verboseManager.createComponentManager("[" + displayName + "]");

    String kitInstallationPath = System.getProperty("kitInstallationPath");
    harnessLogger.output("Using kitInstallationPath: \"" + kitInstallationPath + "\"");
    System.setProperty("tc.install-root", kitInstallationPath + File.separator + "server");
    System.setProperty("restart.inline", Boolean.TRUE.toString());

    MultiplexedEventingStream stdout = new MultiplexedEventingStream(System.out);
    System.setOut(new PrintStream(stdout));
    
    Path kitDir = Paths.get(kitInstallationPath);
    File testParentDir = File.createTempFile(displayName, "", clusterDirectory.toFile());
    testParentDir.delete();
    testParentDir.mkdir();
    String debugPortString = System.getProperty("serverDebugPortStart");
    int serverDebugStartPort = debugPortString != null ? Integer.parseInt(debugPortString) : 0;

    stateManager = new TestStateManager();
    interlock = new InlineStateInterlock(verboseManager.createComponentManager("[Interlock]").createHarnessLogger(), stateManager);

    /*
     * Debug ports, if requested, are reserved from a specified base port, first.  This
     * provides the best chance of allocating a consecutive list of ports without interference
     * from the server and group port reservations.
     */
    PortManager portManager = PortManager.getInstance();
    List<PortManager.PortRef> debugPortRefs = new ArrayList<>();
    List<Integer> serverDebugPorts = new ArrayList<>();
    PortTool.assignDebugPorts(portManager, serverDebugStartPort, stripeSize, debugPortRefs, serverDebugPorts);

    List<PortManager.PortRef> serverPortRefs = portManager.reservePorts(stripeSize);
    List<PortManager.PortRef> groupPortRefs = portManager.reservePorts(stripeSize);

    List<Integer> serverPorts = serverPortRefs.stream().map(PortManager.PortRef::port).collect(toList());
    List<Integer> serverGroupPorts = groupPortRefs.stream().map(PortManager.PortRef::port).collect(toList());
    List<String> serverNames = IntStream.range(0, stripeSize).mapToObj(i -> "testServer" + i).collect(toList());

    String stripeName = "stripe1";
    Path stripeInstallationDir = testParentDir.toPath().resolve(stripeName);
    Files.createDirectory(stripeInstallationDir);

    VerboseManager stripeVerboseManager = displayVerboseManager.createComponentManager("[" + stripeName + "]");

    Path tcConfig = createTcConfig(serverNames, serverPorts, serverGroupPorts, stripeInstallationDir);
    Path kitLocation = installKit(stripeVerboseManager, kitDir, serverJars, stripeInstallationDir);

    StripeConfiguration stripeConfig = new StripeConfiguration(serverDebugPorts, serverPorts, serverGroupPorts, serverNames,
        stripeName, serverHeapSize, logConfigExt, systemProperties);
    InlineStripeInstaller stripeInstaller = new InlineStripeInstaller(interlock, stateManager, stripeVerboseManager, stripeConfig);
    // Configure and install each server in the stripe.
    for (int i = 0; i < stripeSize; ++i) {
      String serverName = serverNames.get(i);
      Path serverWorkingDir = stripeInstallationDir.resolve(serverName);
      Path tcConfigRelative = relativize(serverWorkingDir, tcConfig);
      Path kitLocationRelative = relativize(serverWorkingDir, kitLocation);

      StartupCommandBuilder builder = startupBuilder.get()
          .tcConfig(serverWorkingDir.resolve(tcConfigRelative))
          .serverName(serverName)
          .stripeName(stripeName)
          .serverWorkingDir(serverWorkingDir)
          .kitDir(kitLocationRelative)
          .logConfigExtension(logConfigExt)
          .consistentStartup(consistentStart);      

      String[] cmd = builder.build();
      stripeInstaller.installNewServer(serverName, serverWorkingDir, stdout, ()->startIsolatedServer(serverWorkingDir, cmd));
    }

    cluster = ReadyStripe.configureAndStartStripe(interlock, stripeVerboseManager, stripeConfig, stripeInstaller);
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
        boolean didPass = false;
        try {
          stateManager.waitForFinish();
          didPass = true;
        } catch (GalvanFailureException e) {
          e.printStackTrace();
          checker.completeExceptionally(e);
          didPass = false;
        } finally {
          // Whether we passed or failed, bring everything down.
          try {
            interlock.forceShutdown();
          } catch (Exception e) {
            e.printStackTrace();
            didPass = false;
          } finally {
            setSafeForRun(false);

            serverPortRefs.forEach(PortManager.PortRef::close);
            groupPortRefs.forEach(PortManager.PortRef::close);
            debugPortRefs.stream().filter(Objects::nonNull).forEach(PortManager.PortRef::close);

            if (!didPass) {
              // Typically, we want to interrupt the thread running as the "client" as it might be stuck in a connection
              // attempt, etc.  When Galvan is run in the purely multi-process mode, this is typically where all
              // sub-processes would be terminated.  Since we are running the client as another thread, in-process, the
              // best we can do is interrupt it from a lower-level blocking call.
              // NOTE:  the "client" is also the thread which created us and will join on our termination, before
              // returning back to the user code so it is possible that this interruption could be experienced in its
              // join() call (in which case, we can safely ignore it).
              isInterruptingClient = true;
              clientThread.interrupt();
            }
          }
        }
      }
    };
    this.shepherdingThread.setName("Shepherding Thread");
    this.shepherdingThread.start();
    waitForSafe();
  }

  private Server startIsolatedServer(Path serverWorking, String[] cmd) {
    Path tc = Paths.get(System.getProperty("tc.install-root"), "lib", "tc.jar");
    try {
      URL url = tc.toUri().toURL();
      URL resource = serverWorking.toUri().toURL();
      ClassLoader loader = new IsolatedClassLoader(new URL[] {resource, url}, getClass().getClassLoader());
      Method m = Class.forName("com.tc.server.TCServerMain", true, loader).getMethod("createServer", List.class);
      return (Server)m.invoke(null, Arrays.asList(cmd));
    } catch (RuntimeException mal) {
      throw mal;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Path relativize(Path root, Path other) {
    return root.toAbsolutePath().relativize(other.toAbsolutePath());
  }

  private Path createTcConfig(List<String> serverNames, List<Integer> serverPorts, List<Integer> serverGroupPorts,
                              Path stripeInstallationDir) {
    TcConfigBuilder configBuilder = new TcConfigBuilder(stripeInstallationDir, serverNames, serverPorts, serverGroupPorts, tcProperties,
        namespaceFragment, serviceFragment, clientReconnectWindow, voterCount);
    String tcConfig = configBuilder.build();
    try {
      Path tcConfigPath = Files.createFile(stripeInstallationDir.resolve("tc-config.xml"));
      Files.write(tcConfigPath, tcConfig.getBytes(UTF_8));
      return tcConfigPath;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path installKit(VerboseManager logger, Path srcKit, Set<Path> extraJars, Path stripeInstall) throws IOException {
    if (extraJars.isEmpty()) {
      return srcKit;
    } else {
      ContextualLogger clogger = logger.createFileHelpersLogger();
      Path stripeKit = FileHelpers.createTempCopyOfDirectory(clogger, stripeInstall, "installedKit", srcKit);
      FileHelpers.copyJarsToServer(clogger, stripeKit, extraJars);
      return stripeKit;
    }
  }

  public void manualStop() {
    internalStop();
  }

  @Override
  protected void after() {
    internalStop();
  }

  private void internalStop() {
    stateManager.setTestDidPassIfNotFailed();
    // NOTE:  The waitForFinish is called by the shepherding thread so we just join on it having done that.
    try {
      this.shepherdingThread.join();
    } catch (InterruptedException ignorable) {
      // Note that we both need to join on the shepherding thread (since we created it) but it also tries to interrupt
      // us in the case where we are stuck somewhere else so this exception is possible.
      // This confusion is part of the double-duty being done by the thread from the test harness:  running Galvan
      // _and_ the test.  We split off the Galvan duty to the shepherding thread, so that the test thread can run the
      // test, but we still need to re-join, at the end.
      Assert.assertTrue(this.isInterruptingClient);
      // Clear this flag.
      this.isInterruptingClient = false;
      try {
        this.shepherdingThread.join();
      } catch (InterruptedException unexpected) {
        // Interrupts are unexpected at this point - fail.
        Assert.fail(unexpected.getLocalizedMessage());
      }
    }
    this.shepherdingThread = null;
  }

  @Override
  public URI getConnectionURI() {
    return URI.create(cluster.getStripeUri());
  }

  @Override
  public String[] getClusterHostPorts() {
    return cluster.getStripeUri().substring("terracotta://".length()).split(",");
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
      public void waitForActive() throws Exception {
        cluster.getStripeControl().waitForActive();
      }

      @Override
      public void waitForRunningPassivesInStandby() throws Exception {
        cluster.getStripeControl().waitForRunningPassivesInStandby();
      }

      @Override
      public void startOneServer() throws Exception {
        cluster.getStripeControl().startOneServer();
      }

      @Override
      public void startAllServers() throws Exception {
        cluster.getStripeControl().startAllServers();
      }

      @Override
      public void terminateActive() throws Exception {
        cluster.getStripeControl().terminateActive();
      }

      @Override
      public void terminateOnePassive() throws Exception {
        cluster.getStripeControl().terminateOnePassive();
      }

      @Override
      public void terminateAllServers() throws Exception {
        cluster.getStripeControl().terminateAllServers();
      }
    };
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

  @Override
  public void expectCrashes(boolean yes) {
    interlock.ignoreServerCrashes(yes);
  }
}
