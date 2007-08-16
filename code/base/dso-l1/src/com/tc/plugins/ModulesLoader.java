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
import com.tc.bundles.EmbeddedOSGiRuntimeCallbackHandler;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigLoader;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;
import com.tc.object.util.JarResourceLoader;
import com.tc.util.VendorVmSignature;
import com.tc.util.VendorVmSignatureException;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;

public class ModulesLoader {

  private static final Comparator SERVICE_COMPARATOR = new Comparator() {

                                                       public int compare(Object arg0, Object arg1) {
                                                         ServiceReference s1 = (ServiceReference) arg0;
                                                         ServiceReference s2 = (ServiceReference) arg1;

                                                         Integer r1 = (Integer) s1
                                                             .getProperty(Constants.SERVICE_RANKING);
                                                         Integer r2 = (Integer) s2
                                                             .getProperty(Constants.SERVICE_RANKING);

                                                         if (r1 == null) r1 = ModuleSpec.NORMAL_RANK;
                                                         if (r2 == null) r2 = ModuleSpec.NORMAL_RANK;

                                                         return r2.compareTo(r1);
                                                       }

                                                     };

  private static final TCLogger   logger             = TCLogging.getLogger(ModulesLoader.class);
  private static final TCLogger   consoleLogger      = CustomerLogging.getConsoleLogger();

  private static final Object     lock               = new Object();

  private ModulesLoader() {
    // cannot be instantiated
  }

  private static void insertBasicConfigBundles(Modules modules) {
    Module m = modules.addNewModule();
    m.setName("modules-common-1.0");
    m.setVersion("1.0.0");
  }

  public static void initModules(final DSOClientConfigHelper configHelper, final ClassProvider classProvider,
                                 final boolean forBootJar) {
    EmbeddedOSGiRuntime osgiRuntime = null;
    synchronized (lock) {
      Modules modules = configHelper.getModulesForInitialization();
      insertBasicConfigBundles(modules);

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
          consoleLogger.fatal("Unable to initialize modules, shutting down.  See log for details.", be1);
          try {
            osgiRuntime.shutdown();
          } catch (BundleException be2) {
            logger.error("Error shutting down plugin runtime", be2);
          }
          System.exit(-1);
        } catch (InvalidSyntaxException be1) {
          consoleLogger.fatal("Unable to initialize modules, shutting down.  See log for details", be1);
          try {
            osgiRuntime.shutdown();
          } catch (BundleException be2) {
            logger.error("Error shutting down plugin runtime", be2);
          }
          System.exit(-1);
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
                                  final ClassProvider classProvider, final Module[] modules, final boolean forBootJar)
      throws BundleException {
    // install all available bundles
    osgiRuntime.installBundles();

    if (configHelper instanceof StandardDSOClientConfigHelper) {
      final Dictionary serviceProps = new Hashtable();
      serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
      serviceProps.put(Constants.SERVICE_DESCRIPTION, "Main point of entry for programmatic access to"
                                                      + " the Terracotta bytecode instrumentation");
      osgiRuntime.registerService(configHelper, serviceProps);
    }

    // now start the bundles that are listed in the modules section of the config
    EmbeddedOSGiRuntimeCallbackHandler callback = new EmbeddedOSGiRuntimeCallbackHandler() {
      public void callback(final Object payload) throws BundleException {
        Bundle bundle = (Bundle) payload;
        if (bundle != null) {
          if (!forBootJar) {
            registerClassLoader(classProvider, bundle);
          }
          loadConfiguration(configHelper, bundle);
        }
      }
    };

    for (int pos = 0; pos < modules.length; ++pos) {
      String name = modules[pos].getName();
      String version = modules[pos].getVersion();
      osgiRuntime.startBundle(name, version, callback);
    }
  }

  private static void registerClassLoader(final ClassProvider classProvider, final Bundle bundle)
      throws BundleException {
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
    if (serviceReferences != null && serviceReferences.length > 0) {
      Arrays.sort(serviceReferences, SERVICE_COMPARATOR);
    }

    if (serviceReferences == null) { return; }
    ModuleSpec[] modulesSpecs = new ModuleSpec[serviceReferences.length];
    for (int i = 0; i < serviceReferences.length; i++) {
      modulesSpecs[i] = (ModuleSpec) osgiRuntime.getService(serviceReferences[i]);
      osgiRuntime.ungetService(serviceReferences[i]);
    }
    configHelper.setModuleSpecs(modulesSpecs);
  }

  private static String getConfigPath(final Bundle bundle) throws BundleException {
    final VendorVmSignature vmsig;
    try {
      vmsig = new VendorVmSignature();
      final String TC_CONFIG_HEADER = "Terracotta-Configuration";
      final String TC_CONFIG_HEADER_FOR_VM = TC_CONFIG_HEADER + VendorVmSignature.SIGNATURE_SEPARATOR
                                             + vmsig.getSignature();
      // check if the config-bundle indicates a vm vendor specific terracotta configuration...
      String path = (String) bundle.getHeaders().get(TC_CONFIG_HEADER_FOR_VM); // config for vendor specific vm
      if (path == null) { //
        path = (String) bundle.getHeaders().get(TC_CONFIG_HEADER); // terracotta specific config (eg: tests)
        if (path == null) { //
          path = "terracotta.xml"; // default terracotta config 
        }
      }
      return path;
    } catch (VendorVmSignatureException e) {
      throw new BundleException(e.getMessage());
    }
  }

  private static void loadConfiguration(final DSOClientConfigHelper configHelper, final Bundle bundle)
      throws BundleException {

    // attempt to load the config-bundle's fragment of the configuration file 
    final String configPath = getConfigPath(bundle);
    final InputStream is;
    try {
      is = JarResourceLoader.getJarResource(new URL(bundle.getLocation()), configPath);
    } catch (MalformedURLException murle) {
      throw new BundleException("Unable to create URL from: " + bundle.getLocation(), murle);
    } catch (IOException ioe) {
      throw new BundleException("Unable to extract " + configPath + " from URL: " + bundle.getLocation(), ioe);
    }

    // if config-bundle's fragment of the configuration file is not included in the jar file
    // then we don't need to merge it in with the current configuration --- but make a note of it.
    if (is == null) {
      logger.warn("The config file '" + configPath + "', for module '" + bundle.getSymbolicName()
                  + "' does not appear to be a part of the module's config-bundle jar file contents.");
      return;
    }

    // otherwise, merge it with the current configuration
    try {
      DsoApplication application = DsoApplication.Factory.parse(is);
      if (application != null) {
        ConfigLoader loader = new ConfigLoader(configHelper, logger);
        loader.loadDsoConfig(application);
        logger.info("Module configuration loaded for " + bundle.getSymbolicName() + " (" + configPath + ")");
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

}
