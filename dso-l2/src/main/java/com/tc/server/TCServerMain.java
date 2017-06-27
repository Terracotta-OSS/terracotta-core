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
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.util.ProductInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.service.ServiceConfigParser;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TCServerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCServerMain.class);

  public static TCServer server;
  public static L2ConfigurationSetupManager setup;

  public static void main(String[] args) {
    writeVersion();
    writePID();

    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);

      ClassLoader systemLoader = ServiceLocator.getPlatformLoader();
      Thread.currentThread().setContextClassLoader(systemLoader);

//  set this as the context loader for creation of all the infrastructure at bootstrap time.

      setup = factory.createL2TVSConfigurationSetupManager(null, new ServiceClassLoader(ServiceLocator.getImplementations(ServiceConfigParser.class, systemLoader)));

      String logDir = setup.commonl2Config().logsPath().getCanonicalPath() + "/" +
                      setup.dsoL2Config().host() + "-" + setup.dsoL2Config().tsaPort().getValue();
      TCLogbackLogging.redirectLogging(logDir);

      writeSystemProperties();

      server = ServerFactory.createServer(setup,threadGroup);
      server.start();

      server.waitUntilShutdown();

    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }
  
  public static TCServer getServer() {
    return server;
  }
  
  public static L2ConfigurationSetupManager getSetupManager() {
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
  }

  private static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      LOGGER.info("PID is {}", pid);
    } catch (Throwable t) {
      LOGGER.warn("Unable to fetch the PID of this process.");
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