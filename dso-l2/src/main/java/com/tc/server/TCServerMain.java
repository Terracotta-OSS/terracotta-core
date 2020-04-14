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
package com.tc.server;

import com.tc.classloader.ServiceLocator;
import com.tc.config.ServerConfigurationManager;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.objectserver.impl.JMXSubsystem;
import com.tc.util.ProductInfo;
import org.terracotta.configuration.ConfigurationProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.StopAction;

public class TCServerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCServerMain.class);
  private static final Logger CONSOLE = TCLogging.getConsoleLogger();

  public static TCServer server;
  public static ServerConfigurationManager setup;

  public static void main(String[] args) {
    writeVersion();
    writePID();

    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ClassLoader systemLoader = ServiceLocator.getPlatformLoader();

      ConfigurationProvider configurationProvider = getConfigurationProvider(systemLoader);

      CommandLineParser commandLineParser = new CommandLineParser(args, configurationProvider);

      configurationProvider.initialize(commandLineParser.getProviderArgs());

      setup = new ServerConfigurationManager(
          configurationProvider,
          commandLineParser.consistentStartup(),
          commandLineParser.upgradeCompatibility(),
          new ServiceLocator(systemLoader),
          args
      );

      TCLogbackLogging.redirectLogging(setup.getServerConfiguration().getLogsLocation().getCanonicalPath());

      writeSystemProperties();

      server = ServerFactory.createServer(setup, threadGroup);
      setServerEnvironment();
      server.start();

      server.waitUntilShutdown();
    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }

  private static void setServerEnvironment() {
    ServerEnv.setDefaultServer(new Server() {
      @Override
      public int getServerCount() {
        return setup.getConfiguration().getServerConfigurations().size();
      }

      @Override
      public String[] processArguments() {
        return setup.getProcessArguments();
      }

      @Override
      public void stop(StopAction... modes) {
        server.stop(modes);
      }

      @Override
      public boolean stopIfPassive(StopAction... modes) {
        try {
          server.stopIfPassive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean stopIfActive(StopAction... modes) {
        try {
          server.stopIfActive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean isActive() {
        return server.isActive();
      }

      @Override
      public boolean isStopped() {
        return server.isStopped();
      }

      @Override
      public boolean isPassiveUnitialized() {
        return server.isPassiveUnitialized();
      }

      @Override
      public boolean isPassiveStandby() {
        return server.isPassiveStandby();
      }

      @Override
      public boolean isReconnectWindow() {
        return server.isReconnectWindow();
      }

      @Override
      public String getState() {
        return server.getState().toString();
      }

      @Override
      public long getStartTime() {
        return server.getStartTime();
      }

      @Override
      public long getActivateTime() {
        return server.getActivateTime();
      }

      @Override
      public String getIdentifier() {
        return server.getL2Identifier();
      }

      @Override
      public int getClientPort() {
        return setup.getServerConfiguration().getTsaPort().getPort();
      }

      @Override
      public int getServerPort() {
        return setup.getServerConfiguration().getGroupPort().getPort();
      }

      @Override
      public int getReconnectWindowTimeout() {
        return setup.getServerConfiguration().getClientReconnectWindow();
      }

      @Override
      public void waitUntilShutdown() {
        server.waitUntilShutdown();
      }

      @Override
      public void dump() {
        server.dump();
      }

      @Override
      public String getClusterState() {
        return server.getClusterState(null);
      }

      @Override
      public String getConfiguration() {
        return setup.getConfiguration().getRawConfiguration();
      }

      @Override
      public ClassLoader getServiceClassLoader(ClassLoader parent, Class<?>... serviceClasses) {
        return new ServiceClassLoader(setup.getConfigurationProvider().getClass().getClassLoader(), serviceClasses);
      }

      @Override
      public <T> List<Class<? extends T>> getImplementations(Class<T> serviceClasses) {
        return setup.getServiceLocator().getImplementations(serviceClasses);
      }

      @Override
      public ServerJMX getManagement() {
        JMXSubsystem system = new JMXSubsystem();
        return new ServerJMX() {
          @Override
          public String get(String target, String attr) {
            return system.get(target, attr);
          }

          @Override
          public String set(String target, String attr, String val) {
            return system.set(target, attr, val);
          }

          @Override
          public String call(String target, String cmd, String arg) {
            return system.call(target, cmd, arg);
          }
        };
      }

      @Override
      public Properties getCurrentChannelProperties() {
        return GuardianContext.getCurrentChannelProperties();
      }

      @Override
      public void warn(String warning, Object...event) {
        LOGGER.warn(warning,event[0]);
      }
    });
  }

  private static ConfigurationProvider getConfigurationProvider(ClassLoader loader) {
    Collection<Class<? extends ConfigurationProvider>> pl = new ServiceLocator(loader).getImplementations(ConfigurationProvider.class);
    if (pl.isEmpty()) {
      throw new RuntimeException("No ConfigurationProvider found");
    } else if (pl.size() == 1) {
      try {
        return pl.iterator().next().newInstance();
      } catch (IllegalAccessException | InstantiationException ii) {
        throw new RuntimeException("unable to load configuration");
      }
    } else {
      throw new RuntimeException("Found multiple implementations of ConfigurationProvider");
    }
  }

  public static TCServer getServer() {
    return server;
  }
  
  public static ServerConfigurationManager getSetupManager() {
    return setup;
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getInstance();

    // Write build info always
    String longProductString = info.toLongString();
    LOGGER.info(longProductString);

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      LOGGER.info(longPatchString);
    }

    String versionMessage = info.versionMessage();
    if (!versionMessage.isEmpty()) {
      LOGGER.info(versionMessage);
    }
  }

  private static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      CONSOLE.info("PID is {}", pid);
    } catch (Throwable t) {
      CONSOLE.warn("Unable to fetch the PID of this process.");
    }
  }

  private static void writeSystemProperties() {
    try {
      Properties properties = System.getProperties();
      int maxKeyLength = 1;

      List<String> keys = new ArrayList<String>();
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        Object objKey = entry.getKey();
        Object objValue = entry.getValue();

        // Filter out any bad non-String keys or values in system properties
        if (objKey instanceof String && objValue instanceof String) {
          String key = (String) objKey;
          keys.add(key);
          maxKeyLength = Math.max(maxKeyLength, key.length());
        }
      }

      String inputArguments = null;
      try {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        inputArguments = mxbean.getInputArguments().toString();
      } catch (SecurityException se) {
        inputArguments = "unknown";
      }
      String nl = System.getProperty("line.separator");
      StringBuffer data = new StringBuffer();
      data.append("All Java System Properties for this Terracotta instance:");
      data.append(nl);
      data.append("========================================================================");
      data.append(nl);
      data.append("JVM arguments: " + inputArguments);
      data.append(nl);

      String[] sortedKeys = keys.toArray(new String[keys.size()]);
      Arrays.sort(sortedKeys);
      for (String key : sortedKeys) {
        data.append(key);
        for (int i = 0; i < maxKeyLength - key.length(); i++) {
          data.append(' ');
        }
        data.append(" : ");
        data.append(properties.get(key));
        data.append(nl);
      }
      data.append("========================================================================");

      LoggerFactory.getLogger(TCLogbackLogging.class).info(data.toString());
    } catch (Throwable t) {
      // don't let exceptions here be fatal
      t.printStackTrace();
    }
  }
}