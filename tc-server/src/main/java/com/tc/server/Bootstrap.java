/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.server;

import com.tc.classloader.ServiceLocator;
import com.tc.config.ServerConfigurationManager;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.productinfo.ProductInfo;
import com.tc.spi.SPIServer;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

/**
 *
 */
public class Bootstrap implements BootstrapService {
  
  @Override
  public Future<Boolean> createServer(List<String> args, OutputStream out, ClassLoader loader) {
    ServiceLocator locator = ServiceLocator.createPlatformServiceLoader(loader);
    TCLogbackLogging.bootstrapLogging(out, locator);

    Logger console = LoggerFactory.getLogger(TCLogbackLogging.CONSOLE);

    ServerConfigurationManager setup = new ServerConfigurationManager(
      getConfigurationProvider(locator),
      locator,
      args
    );

    writeVersion(setup.getProductInfo(), console);
    writePID(console);

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
    return new ServerFuture(server, threadGroup);
  }

  private static void writeVersion(ProductInfo info, Logger console) {
    // Write build info always
    String longProductString = info.toLongString();
    console.info(longProductString);

    console.info("Extensions:");
    for (String ext : info.getExtensions()) {
      console.info(ext);
    }

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      console.info(longPatchString);
    }

    String versionMessage = info.versionMessage();
    if (!versionMessage.isEmpty()) {
      console.info(versionMessage);
    }
  }

  private static void writePID(Logger console) {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      console.info("PID is {}", pid);
    } catch (Throwable t) {
      console.warn("Unable to fetch the PID of this process.");
    }
  }
  
  private static void writeSystemProperties() {
    try {
      Properties properties = System.getProperties();
      int maxKeyLength = 1;

      List<String> keys = new ArrayList<>();
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

  private SPIServer wrap(ServerConfigurationManager config, List<String> args, ServiceLocator loader, TCServerImpl impl) {
    return new PauseableServer(config, args, loader, impl);
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
