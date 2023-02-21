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
import com.tc.logging.TCLogging;
import com.tc.management.TerracottaManagement;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.objectserver.impl.JMXSubsystem;
import com.tc.productinfo.ProductInfo;
import com.tc.spi.Pauseable;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.StopAction;

/**
 *
 */
public class Bootstrap implements BootstrapService {
  private static final Logger CONSOLE = LoggerFactory.getLogger(TCLogbackLogging.CONSOLE);
  
  @Override
  public Future<Boolean> createServer(List<String> args, OutputStream out, ClassLoader loader) {
    TCLogbackLogging.bootstrapLogging(out);
    ServiceLocator locator = ServiceLocator.createPlatformServiceLoader(loader);

    ServerConfigurationManager setup = new ServerConfigurationManager(
      getConfigurationProvider(locator),
      locator,
      args
    );

    writeVersion(setup.getProductInfo());
    writePID();

    ThrowableHandler throwableHandler = new BootstrapThrowableHandler(LoggerFactory.getLogger(TCServerImpl.class));
    TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler, Integer.toString(System.identityHashCode(this)), out != null);

    TCServerImpl impl = new TCServerImpl(setup, threadGroup);
    Server server = wrap(setup, args, locator, impl);
    boolean redirected = false;
    try {
      ServerEnv.setDefaultServer(server);

      setup.initialize();

      TCLogbackLogging.setServerName(setup.getServerConfiguration().getName());
      TCLogbackLogging.redirectLogging(setup.getServerConfiguration().getLogsLocation());
      redirected = true;

      writeSystemProperties();

      impl.start();
    } catch (ConfigurationException config) {
      throwableHandler.handleThrowable(Thread.currentThread(), config);
      if (config.getMessage().equals("print usage information")) {
        // unfortunate but this is how we swallow logging for now.
        redirected = true;
      }
    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
      throw t;
    } finally {
      if (!redirected) {
        TCLogbackLogging.redirectLogging(null);
      }
    }
    return new Future<Boolean>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
       if (server.isStopped()) {
         return false;
       } else {
         server.stop();
         return true;
       }
      }

      @Override
      public boolean isCancelled() {
        return !server.isStopped();
      }

      @Override
      public boolean isDone() {
        return server.isStopped();
      }

      @Override
      public Boolean get() throws InterruptedException, ExecutionException {
        return server.waitUntilShutdown();
      }

      @Override
      public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //  for completeness , do not use
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < end) {
          if (!server.isStopped()) {
            Thread.sleep(500);
          } else {
            return server.waitUntilShutdown();
          }
        }
        throw new TimeoutException();
      }
      // for galvan compatiblity
      public boolean waitUntilShutdown() {
        try {
          return get();
        } catch (ExecutionException | InterruptedException e) {
          return false;
        }
      }
      
      public Object getManagement() {
        return server.getManagement();
      }
    };
  }

  private static void writeVersion(ProductInfo info) {
    // Write build info always
    String longProductString = info.toLongString();
    CONSOLE.info(longProductString);

    CONSOLE.info("Extensions:");
    for (String ext : info.getExtensions()) {
      CONSOLE.info(ext);
    }

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

  private static interface PauseableServer extends Server, Pauseable {

  }

  private Server wrap(ServerConfigurationManager config, List<String> args, ServiceLocator loader, TCServerImpl impl) {
    return new PauseableServer() {

      @Override
      public Map<String, ?> getStateMap() {
        return impl.getStateMap();
      }

      @Override
      public void pause(String path) {
        impl.pause(path);
      }

      @Override
      public void unpause(String path) {
        impl.unpause(path);
      }

      @Override
      public int getServerCount() {
        return config.getConfiguration().getServerConfigurations().size();
      }

      @Override
      public String[] processArguments() {
        return args.toArray(new String[args.size()]);
      }

      @Override
      public void stop(StopAction... modes) {
        impl.stop(modes);
      }

      @Override
      public boolean stopIfPassive(StopAction... modes) {
        try {
          impl.stopIfPassive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean stopIfActive(StopAction... modes) {
        try {
          impl.stopIfActive(modes);
        } catch (PlatformStopException stop) {
          warn("unable to stop server", stop);
          return false;
        }
        return true;
      }

      @Override
      public boolean isActive() {
        return impl.isActive();
      }

      @Override
      public boolean isStopped() {
        return impl.isStopped();
      }

      @Override
      public boolean isPassiveUnitialized() {
        return impl.isPassiveUnitialized();
      }

      @Override
      public boolean isPassiveStandby() {
        return impl.isPassiveStandby();
      }

      @Override
      public boolean isReconnectWindow() {
        return impl.isReconnectWindow();
      }

      @Override
      public String getState() {
        return impl.getState().toString();
      }

      @Override
      public long getStartTime() {
        return impl.getStartTime();
      }

      @Override
      public long getActivateTime() {
        return impl.getActivateTime();
      }

      @Override
      public String getIdentifier() {
        return impl.getL2Identifier();
      }

      @Override
      public int getClientPort() {
        return config.getServerConfiguration().getTsaPort().getPort();
      }

      @Override
      public int getServerPort() {
        return config.getServerConfiguration().getGroupPort().getPort();
      }

      @Override
      public String getServerHostName() {
        return config.getServerConfiguration().getHost();
      }

      @Override
      public int getReconnectWindowTimeout() {
        return config.getServerConfiguration().getClientReconnectWindow();
      }

      @Override
      public boolean waitUntilShutdown() {
        try {
          return impl.waitUntilShutdown();
        } finally {
          try {
            TCLogbackLogging.resetLogging();
          } catch (Exception e) {
            // Ignore
          }
        }
      }

      @Override
      public void dump() {
        impl.dump();
      }

      @Override
      public String getClusterState() {
        return impl.getClusterState(null);
      }

      @Override
      public String getConfiguration() {
        return config.rawConfigString();
      }

      @Override
      public ClassLoader getServiceClassLoader(ClassLoader parent, Class<?>... serviceClasses) {
        return new ServiceClassLoader(parent, serviceClasses);
      }

      @Override
      public <T> List<Class<? extends T>> getImplementations(Class<T> serviceClasses) {
        return loader.getImplementations(serviceClasses);
      }

      @Override
      public ServerJMX getManagement() {
        JMXSubsystem system = impl.getJMX();
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

          @Override
          public void registerMBean(String target, Object object) {
            try {
              system.getServer().registerMBean(object, TerracottaManagement.createObjectName(null, target, TerracottaManagement.MBeanDomain.PUBLIC));
            } catch (InstanceAlreadyExistsException |
                MBeanRegistrationException |
                MalformedObjectNameException |
                NotCompliantMBeanException e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public MBeanServer getMBeanServer() {
            return system.getServer();
          }
        };
      }
      
      @Override
      public Properties getCurrentChannelProperties() {
        return GuardianContext.getCurrentChannelProperties();
      }

      @Override
      public void warn(String warning, Object...event) {
        TCLogging.getConsoleLogger().warn(warning, event);
      }

      @Override
      public void console(String message, Object... sub) {
        TCLogging.getConsoleLogger().info(message, sub);
      }

      @Override
      public void audit(String msg, Properties additional) {
        impl.audit(msg, additional);
      }
    };
  }
  
  private static ConfigurationProvider getConfigurationProvider(ServiceLocator loader) {
    Collection<Class<? extends ConfigurationProvider>> pl = loader.getImplementations(ConfigurationProvider.class);
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
}
