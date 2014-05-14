/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.cli.Options;
import org.apache.commons.lang.ArrayUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.security.PwProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.terracotta.management.keychain.KeyChain;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class TCStop {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private final String          host;
  private final int             port;
  private final String          username;
  private final String          password;
  private final boolean         forceStop;
  private final boolean         secured;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static final void main(String[] args) throws Exception {
    Options options = StandardConfigurationSetupManagerFactory
        .createOptions(StandardConfigurationSetupManagerFactory.ConfigMode.L2);
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(TCStop.class.getName(), args);
    commandLineBuilder.setOptions(options);
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.addOption("s", "secured", false, "secured", String.class, false);
    commandLineBuilder.addOption("force", "force", false, "force", String.class, false);
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.addOption("k", "ignoreSSLCert", false, "Ignore untrusted SSL certificate", String.class, false);

    commandLineBuilder.parse();
    String[] arguments = commandLineBuilder.getArguments();

    if (arguments.length > 2) {
      commandLineBuilder.usageAndDie();
    }

    String host = null;
    int port = -1;

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    if (commandLineBuilder.hasOption('k')) {
      System.setProperty("tc.ssl.trustAllCerts", "true");
    }

    String defaultName = StandardConfigurationSetupManagerFactory.DEFAULT_CONFIG_SPEC;
    File configFile = new File(System.getProperty("user.dir"), defaultName);
    boolean configSpecified = commandLineBuilder.hasOption('f');
    boolean nameSpecified = commandLineBuilder.hasOption('n');
    boolean userNameSpecified = commandLineBuilder.hasOption('u');
    boolean passwordSpecified = commandLineBuilder.hasOption('w');
    boolean forceSpecified = commandLineBuilder.hasOption("force");
    boolean securedSpecified = commandLineBuilder.hasOption("s");
    ArrayList tmpArgs = new ArrayList(Arrays.asList(args));
    tmpArgs.remove("-force");
    tmpArgs.remove("--force");

    String userName = null;
    final String password;
    if (userNameSpecified) {
      userName = commandLineBuilder.getOptionValue('u');
      if (passwordSpecified) {
        password = commandLineBuilder.getOptionValue('w');
      } else {
        final Console console = System.console();
        if(console != null) {
          password = new String(console.readPassword("Enter password: ")); // Hu?!
        } else {
          password = CommandLineBuilder.readPassword();
        }
      }
    } else {
      password = null;
    }

    boolean secured = false;
    if (configSpecified || System.getProperty("tc.config") != null || configFile.exists()) {
      if (!configSpecified && System.getProperty("tc.config") == null) {

        tmpArgs.add("-f");
        final String absolutePath = configFile.getAbsolutePath();
        if(securedSpecified && absolutePath.indexOf('@') == -1 && userNameSpecified) {
          tmpArgs.add(userName + "@" + absolutePath);
        } else {
          tmpArgs.add(absolutePath);
        }
      }

      args = (String[]) tmpArgs.toArray(new String[tmpArgs.size()]);

      FatalIllegalConfigurationChangeHandler changeHandler = new FatalIllegalConfigurationChangeHandler();
      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                              args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                                              changeHandler, new PwProvider() {
        @Override
        public char[] getPasswordFor(final URI uri) {
          return getPassword();
        }

        @Override
        @SuppressWarnings("hiding")
        public char[] getPasswordForTC(final String user, final String host, final int port) {
          return getPassword();
        }

        private char[] getPassword() {
          return password != null ? password.toCharArray() : null;
        }
      });

      String name = null;
      if (nameSpecified) {
        name = commandLineBuilder.getOptionValue('n');
      }

      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(name);
      String[] servers = manager.allCurrentlyKnownServers();

      if(manager.isSecure() || securedSpecified) {
        // Create a security manager that will set the default SSL context
        final Class<?> securityManagerClass = Class.forName("com.tc.net.core.security.TCClientSecurityManager");
        securityManagerClass.getConstructor(KeyChain.class, boolean.class).newInstance(null, true);
        secured = true;
      }

      if (nameSpecified && !Arrays.asList(servers).contains(name)) {
        consoleLogger.error("The specified configuration of the Terracotta server instance '" + name
                            + "' does not exist; exiting.");
        System.exit(1);
      }

      if (name == null && servers != null && servers.length == 1) {
        name = servers[0];
        consoleLogger.info("There is only one Terracotta server instance in this configuration file (" + name
                           + "); stopping it.");
      } else if (name == null && servers != null && servers.length > 1) {
        consoleLogger
            .error("There are multiple Terracotta server instances defined in this configuration file; please specify "
                   + "which one you want to stop, using the '-n' command-line option. Available servers are:\n"
                   + "    " + ArrayUtils.toString(servers));
        System.exit(1);
      }

      CommonL2Config serverConfig = manager.commonL2ConfigFor(name);

      host = serverConfig.jmxPort().getBind();
      if (host == null || host.equals("0.0.0.0")) host = serverConfig.host();
      if (host == null) host = name;
      if (host == null) host = DEFAULT_HOST;
      port = computeJMXPort(serverConfig);
      consoleLogger.info("Host: " + host + ", port: " + port);
    } else {
      if (arguments.length == 0) {
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
        consoleLogger.info("No host or port provided. Stopping the Terracotta server instance at '" + host + "', port "
                           + port + " by default.");
      } else if (arguments.length == 1) {
        host = DEFAULT_HOST;
        port = Integer.parseInt(arguments[0]);
      } else {
        host = arguments[0];
        port = Integer.parseInt(arguments[1]);
      }
    }

    try {
      new TCStop(host, port, userName, password, forceSpecified, secured).stop();
    } catch (SecurityException se) {
      consoleLogger.error(se.getMessage());
      commandLineBuilder.usageAndDie();
    } catch (Exception e) {
      Throwable root = getRootCause(e);
      if (root instanceof ConnectException) {
        consoleLogger.error("Unable to connect to host '" + host + "', port " + port
                            + ". Are you sure there is a Terracotta server instance running there?");
      } else {
        consoleLogger.error("Unexpected error while stopping server: " + root.getMessage());
      }
      System.exit(1);
    }
  }

  static int computeJMXPort(CommonL2Config l2Config) {
    if (l2Config.jmxPort() != null) {
      return l2Config.jmxPort().getIntValue() == 0 ? DEFAULT_PORT : l2Config.jmxPort().getIntValue();
    } else {
      return L2DSOConfigObject.computeJMXPortFromTSAPort(l2Config.tsaPort().getIntValue());
    }
  }

  private static Throwable getRootCause(Throwable e) {
    Throwable t = e;
    while (t != null) {
      e = t;
      t = t.getCause();
    }
    return e;
  }

  public TCStop(String host, int port) {
    this(host, port, null, null, false, false);
  }

  public TCStop(String host, int port, String userName, String password, boolean forceStop, boolean secured) {
    this.host = host;
    this.port = port;
    this.username = userName;
    this.password = password;
    this.forceStop = forceStop;
    this.secured = secured;
  }

  public void stop() throws IOException {
    JMXConnector jmxc = null;
    jmxc = CommandLineBuilder.getJMXConnector(username, password, host, port, secured);
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    // To be in sync with Test framework's server shutdown grace time
    long SHUTDOWN_WAIT_TIME = 2 * 60 * 1000;
    long startTime = System.currentTimeMillis();
    long maxWaitTime = (startTime + (long) (0.75 * SHUTDOWN_WAIT_TIME));
    if (mbsc != null) {
      TCServerInfoMBean tcServerInfo = (TCServerInfoMBean) TerracottaManagement
          .findMBean(L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, mbsc);
      if (!forceStop && tcServerInfo.isActive() && tcServerInfo.isLegacyProductionModeEnabled()) {
        ServerGroupInfo currentServerGroup = getCurrentServerGroup(tcServerInfo);
        if (currentServerGroup != null) {
          boolean isPassiveStandByAvailable = false;
          for (L2Info l2Info : currentServerGroup.members()) {
            try {
              if (isPassiveStandBy(l2Info)) {
                isPassiveStandByAvailable = true;
                break;
              }
            } catch (Exception e) {
              continue;
            }
          }

          if (!isPassiveStandByAvailable) {
            consoleLogger.error("No passive server available in Standby mode. Use -force option to stop the server");
            return;
          }
        }

      }

      // wait a bit for server to be ready for shutdown
      while (!tcServerInfo.isShutdownable() && (System.currentTimeMillis() < maxWaitTime)) {
        consoleLogger.warn("Server state: " + tcServerInfo.getState() + ". Waiting for server to be shutdownable... ");
        ThreadUtil.reallySleep(5000);
      }
      try {
        tcServerInfo.shutdown();
      } finally {
        jmxc.close();
      }
    } else {
      consoleLogger.warn("Unable to get mbean connection to Server " + host + ":" + port);
    }
  }

  private ServerGroupInfo getCurrentServerGroup(TCServerInfoMBean tcServerInfo) throws UnknownHostException {
    ServerGroupInfo[] serverGroupInfos = tcServerInfo.getServerGroupInfo();
    for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
      L2Info[] members = serverGroupInfo.members();
      for (L2Info l2Info : members) {
        if (l2Info.name().equals(tcServerInfo.getL2Identifier())) { return serverGroupInfo; }
      }
    }
    return null;
  }

  private boolean isPassiveStandBy(L2Info l2Info) throws Exception {
    TCServerInfoMBean mbean = null;
    boolean isPassiveStandByAvailable = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, l2Info.host(), l2Info.jmxPort(), secured);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isPassiveStandByAvailable = mbean.isPassiveStandby();
    } finally {
      if (jmxConnector != null) {
        jmxConnector.close();
      }
    }

    return isPassiveStandByAvailable;
  }
}
