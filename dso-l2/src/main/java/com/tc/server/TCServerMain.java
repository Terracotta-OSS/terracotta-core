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
import com.tc.config.Directories;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import org.terracotta.config.service.ServiceConfigParser;

public class TCServerMain {
  
  public static TCServer server;
  public static L2ConfigurationSetupManager setup;

  public static void main(String[] args) {
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);
      URL[] purls = Arrays.stream(Directories.getServerPluginsLibDir().listFiles())
          .filter(TCServerMain::fileFilter)
          .map(TCServerMain::toURL)
          .toArray(i->new URL[i]);

      ClassLoader pluginLoader = new URLClassLoader(purls);
//  set this as the context loader for creation of all the infrastructure at bootstrap time.
      Thread.currentThread().setContextClassLoader(pluginLoader);
      
      setup = factory.createL2TVSConfigurationSetupManager(null, new ServiceClassLoader(ServiceLocator.getImplementations(ServiceConfigParser.class, pluginLoader)));
      server = ServerFactory.createServer(setup,threadGroup);
      server.start();

      server.waitUntilShutdown();

    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }
  
  private static boolean fileFilter(File target) {
    String name = target.getName().toLowerCase();
    return name.endsWith(".jar") || name.endsWith(".zip");
  }
  
  private static URL toURL(File uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException mal) {
      return null;
    }
  }
  
  public static TCServer getServer() {
    return server;
  }
  
  public static L2ConfigurationSetupManager getSetupManager() {
    return setup;
  }
}