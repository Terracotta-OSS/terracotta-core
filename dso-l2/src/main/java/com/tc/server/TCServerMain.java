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
import com.tc.spi.Guardian;
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
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.StopAction;

public class TCServerMain {
  private static final Logger CONSOLE = TCLogging.getConsoleLogger();

  public static TCServer server;
  public static ServerConfigurationManager setup;

  public static void main(String[] args) {
    TCLogbackLogging.bootstrapLogging();
    
    writeVersion();
    writePID();

    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ClassLoader systemLoader = ServiceLocator.getPlatformLoader();

      ConfigurationProvider configurationProvider = getConfigurationProvider(systemLoader);

      setServerEnvironment(configurationProvider);

      CommandLineParser commandLineParser = new CommandLineParser(args, configurationProvider);

      configurationProvider.initialize(commandLineParser.getProviderArgs());

      setup = new ServerConfigurationManager(
          configurationProvider,
          commandLineParser.consistentStartup(),
          new ServiceLocator(systemLoader),
          args
      );

      TCLogbackLogging.redirectLogging(setup.getServerConfiguration().getLogsLocation());

      writeSystemProperties();

      server = ServerFactory.createServer(setup, threadGroup);
      server.start();

      server.waitUntilShutdown();
    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }

  private static void setServerEnvironment(ConfigurationProvider config) {
    ServerEnv.setDefaultServer(new Server() {
      @Override
      public int getServerCount() {
        return config.getConfiguration().getServerConfigurations().size();
      }

      @Override
      public String[] processArguments() {
        return setup.getProcessArguments();
      }

      @Override
      public void stop(StopAction... modes) {
        getServer().stop(modes);
      }

      @Override
      public boolean stopIfPassive(StopAction... modes) {
        try {
          getServer().stopIfPassive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean stopIfActive(StopAction... modes) {
        try {
          getServer().stopIfActive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean isActive() {
        return getServer().isActive();
      }

      @Override
      public boolean isStopped() {
        return getServer().isStopped();
      }

      @Override
      public boolean isPassiveUnitialized() {
        return getServer().isPassiveUnitialized();
      }

      @Override
      public boolean isPassiveStandby() {
        return getServer().isPassiveStandby();
      }

      @Override
      public boolean isReconnectWindow() {
        return getServer().isReconnectWindow();
      }

      @Override
      public String getState() {
        return getServer().getState().toString();
      }

      @Override
      public long getStartTime() {
        return getServer().getStartTime();
      }

      @Override
      public long getActivateTime() {
        return getServer().getActivateTime();
      }

      @Override
      public String getIdentifier() {
        return getServer().getL2Identifier();
      }

      @Override
      public int getClientPort() {
        try {
          return config.getConfiguration().getServerConfiguration().getTsaPort().getPort();
        } catch (ConfigurationException ce) {
          throw new RuntimeException(ce);
        }
      }

      @Override
      public int getServerPort() {
        try {
          return config.getConfiguration().getServerConfiguration().getGroupPort().getPort();
        } catch (ConfigurationException ce) {
          throw new RuntimeException(ce);
        }
      }

      @Override
      public String getServerHostName() {
        try {
          return config.getConfiguration().getServerConfiguration().getHost();
        } catch (ConfigurationException ce) {
          throw new RuntimeException(ce);
        }
      }

      @Override
      public int getReconnectWindowTimeout() {
        try {
          return config.getConfiguration().getServerConfiguration().getClientReconnectWindow();
        } catch (ConfigurationException ce) {
          throw new RuntimeException(ce);
        }
      }

      @Override
      public void waitUntilShutdown() {
        getServer().waitUntilShutdown();
      }

      @Override
      public void dump() {
        getServer().dump();
      }

      @Override
      public String getClusterState() {
        return getServer().getClusterState(null);
      }

      @Override
      public String getConfiguration() {
        return config.getConfiguration().getRawConfiguration();
      }

      @Override
      public ClassLoader getServiceClassLoader(ClassLoader parent, Class<?>... serviceClasses) {
        return new ServiceClassLoader(config.getClass().getClassLoader(), serviceClasses);
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
        CONSOLE.warn(warning, event);
      }

      @Override
      public void console(String message, Object... sub) {
        CONSOLE.info(message, sub);
      }

      @Override
      public void audit(String msg, Properties additional) {
        GuardianContext.validate(Guardian.Op.AUDIT_OP, msg, additional);

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
    CONSOLE.info(longProductString);

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      CONSOLE.info(longPatchString);
    }

    String versionMessage = info.versionMessage();
    if (!versionMessage.isEmpty()) {
      CONSOLE.info(versionMessage);
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