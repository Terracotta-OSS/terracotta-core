/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.plugins;

import org.apache.xmlbeans.XmlException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigLoader;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ModulesLoader {
  private static final TCLogger logger = TCLogging.getLogger(ModulesLoader.class);

  private ModulesLoader() {
    // cannot be instantiated
  }

  public static void initPlugins(final DSOClientConfigHelper configHelper, final boolean forBootJar) {
    EmbeddedOSGiRuntime osgiRuntime;

    final Modules modules = configHelper.getModulesForInitialization();
    if (modules != null && modules.sizeOfModuleArray() > 0) {
      try {
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      } catch (Exception e) {
        throw new RuntimeException("Unable to create runtime for plugins", e);
      }
      try {
        initPlugins(osgiRuntime, configHelper, modules.getModuleArray());
        if (!forBootJar) {
          getPluginsCustomApplicatorSpecs(osgiRuntime, configHelper);
        }
      } catch (BundleException be1) {
        try {
          osgiRuntime.shutdown();
        } catch (BundleException be2) {
          logger.error("Error shutting down plugin runtime", be2);
        }
        throw new RuntimeException("Exception initializing plugins", be1);
      } catch (InvalidSyntaxException be1) {
        try {
          osgiRuntime.shutdown();
        } catch (BundleException be2) {
          logger.error("Error shutting down plugin runtime", be2);
        }
        throw new RuntimeException("Exception initializing plugins", be1);
      } finally {
        if (forBootJar) {
          try {
            osgiRuntime.shutdown();
          } catch (BundleException be2) {
            logger.error("Error shutting down plugin runtime", be2);
          }
        }
      }
    } else {
      osgiRuntime = null;
    }
  }

  private static void initPlugins(final EmbeddedOSGiRuntime osgiRuntime, final DSOClientConfigHelper configHelper,
                                  final Module[] modules) throws BundleException {
    for (int pos = 0; pos < modules.length; ++pos) {
      String bundle = modules[pos].getName() + "-" + modules[pos].getVersion();
      logger.info("Installing OSGI bundle " + bundle);
      osgiRuntime.installBundle(modules[pos].getName(), modules[pos].getVersion());
      logger.info("Installation of OSGI bundle " + bundle + " successful");
    }
    if (configHelper instanceof StandardDSOClientConfigHelper) {
      final Dictionary serviceProps = new Hashtable();
      serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
      serviceProps.put(Constants.SERVICE_DESCRIPTION, "Main point of entry for programmatic access to"
                                                      + " the Terracotta bytecode instrumentation");
      osgiRuntime.registerService(configHelper, serviceProps);
    }
    for (int pos = 0; pos < modules.length; ++pos) {
      String name = modules[pos].getName();
      String version = modules[pos].getVersion();

      osgiRuntime.startBundle(name, version);

      Bundle bundle = osgiRuntime.getBundle(name, version);
      if (bundle != null) {
        loadConfiguration(configHelper, bundle);
      }
    }
  }

  private static void getPluginsCustomApplicatorSpecs(final EmbeddedOSGiRuntime osgiRuntime,
                                                      final DSOClientConfigHelper configHelper)
      throws InvalidSyntaxException {
    ServiceReference[] serviceReferences = osgiRuntime.getAllServiceReferences(ModuleSpec.class.getName(), null);
    if (serviceReferences == null) { return; }
    ModuleSpec[] pluginSpecs = new ModuleSpec[serviceReferences.length];
    for (int i = 0; i < serviceReferences.length; i++) {
      pluginSpecs[i] = (ModuleSpec) osgiRuntime.getService(serviceReferences[i]);
      osgiRuntime.ungetService(serviceReferences[i]);
    }
    configHelper.setModuleSpecs(pluginSpecs);
  }

  private static void loadConfiguration(final DSOClientConfigHelper configHelper, final Bundle bundle)
      throws BundleException {
    String config = (String) bundle.getHeaders().get("Terracotta-Configuration");
    if (config == null) {
      config = "terracotta.xml";
    }

    final InputStream is;
    try {
      is = getJarResource(new URL(bundle.getLocation()), config);
    } catch (MalformedURLException murle) {
      throw new BundleException("Unable to create URL from: " + bundle.getLocation(), murle);
    } catch (IOException ioe) {
      throw new BundleException("Unable to extract " + config + " from URL: " + bundle.getLocation(), ioe);
    }
    if (is == null) { return; }
    try {
      DsoApplication application = DsoApplication.Factory.parse(is);
      if (application != null) {
        ConfigLoader loader = new ConfigLoader(configHelper, logger);
        loader.loadDsoConfig(application);
        logger.info("Module configuration loaded for " + bundle.getSymbolicName());
        // loader.loadSpringConfig(application.getSpring());
      }
    } catch (IOException ioe) {
      logger.warn("Unable to read configuration from bundle: " + bundle.getSymbolicName(), ioe);
    } catch (XmlException xmle) {
      logger.warn("Unable to parse configuration from bundle: " + bundle.getSymbolicName(), xmle);
    } catch (ConfigurationSetupException cse) {
      logger.warn("Unable to load configuration from bundle: " + bundle.getSymbolicName(), cse);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }
  }

  private static InputStream getJarResource(final URL location, final String resource) throws IOException {
    final JarInputStream jis = new JarInputStream(location.openStream());
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      if (entry.getName().equals(resource)) { return jis; }
    }
    return null;
  }

}
