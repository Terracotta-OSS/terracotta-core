/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.plugins;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.bundles.EmbeddedOSGiEventHandler;
import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.bundles.Resolver;
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
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.VendorVmSignature;
import com.tc.util.VendorVmSignatureException;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public static void initModules(final DSOClientConfigHelper configHelper, final ClassProvider classProvider,
                                 final boolean forBootJar) {
    EmbeddedOSGiRuntime osgiRuntime = null;
    synchronized (lock) {
      final Modules modules = configHelper.getModulesForInitialization();
      if (modules == null) {
        consoleLogger.warn("Modules configuration might not have been properly initialized.");
        return;
      }

      try {
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
        initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), forBootJar);
        if (!forBootJar) {
          getModulesCustomApplicatorSpecs(osgiRuntime, configHelper);
        }
      } catch (Exception e) {
        logger.error(e); // at least log this exception, it's very frustrating if it is completely swallowed
        System.exit(-9);
      } finally {
        if (forBootJar) {
          shutdown(osgiRuntime);
        }
      }
    }
  }

  private static void shutdown(final EmbeddedOSGiRuntime osgiRuntime) {
    if (osgiRuntime != null) {
      osgiRuntime.shutdown();
    }
  }

  static void initModules(final EmbeddedOSGiRuntime osgiRuntime, final DSOClientConfigHelper configHelper,
                                  final ClassProvider classProvider, final Module[] modules, final boolean forBootJar)
      throws BundleException {

    if (configHelper instanceof StandardDSOClientConfigHelper) {
      final Dictionary serviceProps = new Hashtable();
      serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
      serviceProps.put(Constants.SERVICE_DESCRIPTION, "Main point of entry for programmatic access to"
                                                      + " the Terracotta bytecode instrumentation");
      osgiRuntime.registerService(StandardDSOClientConfigHelper.class.getName(), configHelper, serviceProps);
    }

    EmbeddedOSGiEventHandler handler = new EmbeddedOSGiEventHandler() {
      public void callback(final Object payload) throws BundleException {
        Assert.assertTrue(payload instanceof Bundle);
        Bundle bundle = (Bundle) payload;
        if (bundle != null) {
          if (!forBootJar) registerClassLoader(classProvider, bundle);
          loadConfiguration(configHelper, bundle);
        }
      }
    };

    final List moduleList = new ArrayList();
    moduleList.addAll(getAdditionalModules());
    moduleList.addAll(Arrays.asList(modules));

    final Module[] allModules = (Module[]) moduleList.toArray(new Module[moduleList.size()]);
    
    final URL[] osgiRepositories = osgiRuntime.getRepositories();
    final Resolver resolver = new Resolver(Resolver.urlsToStrings(osgiRepositories));
    final File[] locations = resolver.resolve(allModules);

    final URL[] bundleURLs = new URL[locations.length];
    for(int i=0; i<locations.length; i++) {
      try {
        bundleURLs[i] = locations[i].toURL();
      } catch(MalformedURLException e) {
        throw new RuntimeException("Malformed file URL for bundle: " + locations[i].getAbsolutePath(), e);
      }
    }
    
    osgiRuntime.installBundles(bundleURLs);
    osgiRuntime.startBundles(bundleURLs, handler);
  }

  private static List getAdditionalModules() {
    final List modules = new ArrayList();
    final TCProperties modulesProps = TCPropertiesImpl.getProperties().getPropertiesFor("l1.modules");
    final String additionalModuleList = modulesProps != null ? modulesProps.getProperty("additional", true) : null;

    if (additionalModuleList != null) {
      final String[] additionalModules = additionalModuleList.split(";");
      Pattern pattern = Pattern.compile("(.+?)-([0-9\\.]+)-([0-9\\.\\-]+)");
      for (int i = 0; i < additionalModules.length; i++) {
        if (additionalModules[i].length() == 0) continue;

        final Matcher matcher = pattern.matcher(additionalModules[i]);
        if (!matcher.find() || matcher.groupCount() < 3) {
          logger.error("Invalid bundle-jar filename " + additionalModules[i]
                       + "; filenames need to match the pattern: " + pattern.toString());
          continue;
        }

        String component = matcher.group(1);
        final String componentVersion = matcher.group(2);
        final String moduleVersion = matcher.group(3).replaceFirst("\\.$", "");

        final Module module = Module.Factory.newInstance();
        String groupId = module.getGroupId(); // rely on the constant defined in the schema for the default groupId
        final int n = component.lastIndexOf('.');
        if (n > 0) {
          groupId = component.substring(0, n);
          component = component.substring(n + 1);
          module.setGroupId(groupId);
        }

        module.setName(component + "-" + componentVersion);
        module.setVersion(moduleVersion);
        modules.add(module);
      }
    }

    return modules;
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

  /**
   * Extract the list of xml-fragment files that a config bundle should use for instrumentation.
   */
  public static String[] getConfigPath(final Bundle bundle) throws BundleException {
    final VendorVmSignature vmsig;
    try {
      vmsig = new VendorVmSignature();
    } catch (VendorVmSignatureException e) {
      throw new BundleException(e.getMessage());
    }

    final String TERRACOTTA_CONFIGURATION = "Terracotta-Configuration";
    final String TERRACOTTA_CONFIGURATION_FOR_VM = TERRACOTTA_CONFIGURATION + VendorVmSignature.SIGNATURE_SEPARATOR
                                                   + vmsig.getSignature();

    String path = (String) bundle.getHeaders().get(TERRACOTTA_CONFIGURATION_FOR_VM);
    if (path == null) {
      path = (String) bundle.getHeaders().get(TERRACOTTA_CONFIGURATION);
      if (path == null) {
        path = "terracotta.xml";
      }
    }
    
    final String[] paths = path.split(",");
    for (int i = 0; i < paths.length; i++) {
      paths[i] = paths[i].trim();
      if (!paths[i].endsWith(".xml")) {
        paths[i] = paths[i].concat(".xml");
      }
    }

    return paths;
  }

  private static void loadConfiguration(final DSOClientConfigHelper configHelper, final Bundle bundle)
      throws BundleException {
    // attempt to load all of the config fragments found in the config-bundle
    final String[] paths = getConfigPath(bundle);
    for (int i = 0; i < paths.length; i++) {
      final String configPath = paths[i];
      final InputStream is;
      try {
        is = JarResourceLoader.getJarResource(new URL(bundle.getLocation()), configPath);
      } catch (MalformedURLException murle) {
        throw new BundleException("Unable to create URL from: " + bundle.getLocation(), murle);
      } catch (IOException ioe) {
        throw new BundleException("Unable to extract " + configPath + " from URL: " + bundle.getLocation(), ioe);
      }

      if (is == null) {
        continue;
      }

      // otherwise, merge it with the current configuration
      try {
        final DsoApplication application = DsoApplication.Factory.parse(is);
        if (application != null) {
          final ConfigLoader loader = new ConfigLoader(configHelper, logger);
          loader.loadDsoConfig(application);
          logConfig(application, bundle, configPath);
          // loader.loadSpringConfig(application.getSpring());
        }
        is.close();
      } catch (IOException ioe) {
        String msg = "Error reading configuration from bundle: " + bundle.getSymbolicName() + " located at " + bundle.getLocation();
        consoleLogger.warn(msg, ioe);
        logger.warn(msg, ioe);
        throw new BundleException(msg, ioe);
      } catch (XmlException xmle) {
        String msg = "Error parsing configuration from bundle: " + bundle.getSymbolicName() + " located at " + bundle.getLocation();
        consoleLogger.warn(msg, xmle);
        logger.warn(msg, xmle);
        throw new BundleException(msg, xmle);
      } catch (ConfigurationSetupException cse) {
        String msg = "Invalid configuration in bundle: " + bundle.getSymbolicName() + " located at " + bundle.getLocation() + ": " + cse.getMessage();
        consoleLogger.warn(msg, cse);
        logger.warn(msg, cse);
        throw new BundleException(msg, cse);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
  }
  
  private static void logConfig(DsoApplication application, Bundle bundle, String configPath) {
    logger.info("Config loaded from module: " + bundle.getSymbolicName() + " (" + configPath + ")");
    ByteArrayOutputStream bas = new ByteArrayOutputStream();
    BufferedOutputStream buf = new BufferedOutputStream(bas);
    try {
      application.save(buf);
      buf.close();
      logger.info("Here's the config from the module:\n\n" + bas.toString());
    } catch (IOException e) {
      logger.warn("Unable to generate a log entry to for the module's config info.");
    }
  }
}
