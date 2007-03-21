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
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ModulesLoader {

  private static final TCLogger logger = TCLogging.getLogger(ModulesLoader.class);

  private static final Object   lock   = new Object();

  private ModulesLoader() {
    // cannot be instantiated
  }

  public static void initModules(final DSOClientConfigHelper configHelper, final ClassProvider classProvider, final boolean forBootJar) {
    EmbeddedOSGiRuntime osgiRuntime = null;
    synchronized (lock) {
      final Modules modules = configHelper.getModulesForInitialization();
      if (modules != null && modules.sizeOfModuleArray() > 0) {
        try {
          osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
        } catch (Exception e) {
          throw new RuntimeException("Unable to create runtime for plugins", e);
        }
        try {
          initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), forBootJar);
          if (!forBootJar) {
            getModulesCustomApplicatorSpecs(osgiRuntime, configHelper);
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
  }

  private static void initModules(final EmbeddedOSGiRuntime osgiRuntime, final DSOClientConfigHelper configHelper,
                                  final ClassProvider classProvider, final Module[] modules, boolean forBootJar) throws BundleException {
    // The "modules-common" bundle contains a convenience superclass that some bundles extend
    osgiRuntime.installBundle("modules-common", "1.0");
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
        if (!forBootJar) {
          registerClassLoader(classProvider, bundle);
        }
        loadConfiguration(configHelper, bundle);
      }
    }
  }

  private static void registerClassLoader(final ClassProvider classProvider, final Bundle bundle) throws BundleException {
    NamedClassLoader ncl = getClassLoader(bundle);

    String loaderName = Namespace.createLoaderName(Namespace.MODULES_NAMESPACE, ncl.toString());
    ncl.__tc_setClassLoaderName(loaderName);
    classProvider.registerNamedLoader(ncl);
  }

  private static NamedClassLoader getClassLoader(Bundle bundle) throws BundleException {
    try {
      Method m = bundle.getClass().getDeclaredMethod("getClassLoader", new Class[0]);
      m.setAccessible(true);
      ClassLoader classLoader = (ClassLoader) m.invoke(bundle, new Object[0]);
      return (NamedClassLoader) classLoader;
    } catch (Throwable t) {
      throw new BundleException("Unable to get classloader for bundle.", t);
    }
  }

  private static void getModulesCustomApplicatorSpecs(final EmbeddedOSGiRuntime osgiRuntime,
                                                      final DSOClientConfigHelper configHelper)
      throws InvalidSyntaxException {
    ServiceReference[] serviceReferences = osgiRuntime.getAllServiceReferences(ModuleSpec.class.getName(), null);
    if (serviceReferences == null) { return; }
    ModuleSpec[] modulesSpecs = new ModuleSpec[serviceReferences.length];
    for (int i = 0; i < serviceReferences.length; i++) {
      modulesSpecs[i] = (ModuleSpec) osgiRuntime.getService(serviceReferences[i]);
      osgiRuntime.ungetService(serviceReferences[i]);
    }
    configHelper.setModuleSpecs(modulesSpecs);
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

  public static InputStream getJarResource(final URL location, final String resource) throws IOException {
    final JarInputStream jis = new JarInputStream(location.openStream());
    for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
      if (entry.getName().equals(resource)) { return jis; }
    }
    return null;
  }

}
