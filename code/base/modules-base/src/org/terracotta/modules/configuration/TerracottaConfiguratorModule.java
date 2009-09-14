/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.configuration;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.bundles.BundleSpec;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.ClassReplacementTest;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public abstract class TerracottaConfiguratorModule implements BundleActivator {

  protected StandardDSOClientConfigHelper configHelper;
  private Bundle                          thisBundle;

  protected ServiceReference getConfigHelperReference(final BundleContext context) throws Exception {
    final String CONFIGHELPER_CLASS_NAME = "com.tc.object.config.StandardDSOClientConfigHelper";
    final ServiceReference configHelperRef = context.getServiceReference(CONFIGHELPER_CLASS_NAME);
    if (configHelperRef == null) { throw new BundleException("Expected the " + CONFIGHELPER_CLASS_NAME
                                                             + " service to be registered, was unable to find it"); }
    return configHelperRef;
  }

  public final void start(final BundleContext context) throws Exception {
    thisBundle = context.getBundle();
    final ServiceReference configHelperRef = getConfigHelperReference(context);
    configHelper = (StandardDSOClientConfigHelper) context.getService(configHelperRef);
    Assert.assertNotNull(configHelper);
    addInstrumentation(context);
    context.ungetService(configHelperRef);
    registerModuleSpec(context);
    if (!Boolean.getBoolean("tc.bootjar.creation")) {
      registerMBeanSpec(context);
      registerSRASpec(context);
    }
  }

  protected Bundle getThisBundle() {
    return thisBundle;
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  protected void addInstrumentation(final BundleContext context) {
    // default empty body
  }

  protected void registerModuleSpec(final BundleContext context) {
    // default empty body
  }

  protected void registerMBeanSpec(final BundleContext context) {
    // default empty body
  }

  protected void registerSRASpec(final BundleContext context) {
    // default empty body
  }

  protected final void addClassReplacement(final Bundle bundle, final String originalClassName,
                                           final String replacementClassName) {
    addClassReplacement(bundle, originalClassName, replacementClassName, null);
  }

  protected final void addClassReplacement(final Bundle bundle, final String originalClassName,
                                           final String replacementClassName, ClassReplacementTest test) {
    URL resource = getBundleResource(bundle, ByteCodeUtil.classNameToFileName(replacementClassName));
    configHelper.addClassReplacement(originalClassName, replacementClassName, resource, test);
  }

  private URL getBundleResource(Bundle bundle, String resourceName) {
    URL bundleURL = configHelper.getBundleURL(bundle);
    if (bundleURL == null) { throw new RuntimeException(bundle.getLocation() + " was not loaded with this config"); }

    byte[] data = getResourceData(bundleURL, resourceName);
    if (data == null) { throw new RuntimeException("No resource for " + resourceName + " in " + bundleURL); }

    try {
      return new URL("TIM-bytes", "", -1, resourceName, new BytesUrlHandler(data));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Cannot create URL for " + resourceName, e);
    }
  }

  private static byte[] getResourceData(URL bundleURL, String resourceName) {
    byte[] data = null;
    InputStream in = null;
    JarInputStream jis = null;
    try {
      in = bundleURL.openStream();
      jis = new JarInputStream(in);

      JarEntry entry;
      while ((entry = jis.getNextJarEntry()) != null) {
        if (entry.getName().equals(resourceName)) {
          data = IOUtils.toByteArray(jis);
          break;
        }
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(jis);
    }

    return data;
  }

  /**
   * Export the given class that normally resides in a config bundle (aka. integration module) to all classloaders that
   * might try to load it. This is sort of like creating a jar containing the one given class and appending into the
   * lookup path of every classloader NOTE: The export will only work for class loads that pass through
   * java.lang.ClassLoader.loadClassInternal(). Specifically if the loadClass() method is directly being invoked from
   * code someplace, the class export will not function. Code that does a "new <exported class name>", or that uses
   * java.lang.Class.forName(..) will work though
   * 
   * @param classname the bundle class name to export
   * @param targetSystemLoaderOnly True if only the systen classloader should have visibility to this exported class
   */
  protected final void addExportedBundleClass(final Bundle bundle, final String classname,
                                              final boolean targetSystemLoaderOnly) {
    URL url = getBundleResource(bundle, ByteCodeUtil.classNameToFileName(classname));
    configHelper.addClassResource(classname, url, targetSystemLoaderOnly);
  }

  protected final void addExportedBundleClass(final Bundle bundle, final String classname) {
    addExportedBundleClass(bundle, classname, false);
  }

  /**
   * Export the given class that normally resides in tc.jar to all classloaders that might try to load it. This is sort
   * of like creating a jar containing the one given class and appending into the lookup path of every classloader NOTE:
   * The export will only work for class loads that pass through java.lang.ClassLoader.loadClassInternal(). Specifically
   * if the loadClass() method is directly being invoked from code someplace, the class export will not function. Code
   * that does a "new <exported class name>", or that uses java.lang.Class.forName(..) will work though
   * 
   * @param classname the tc.jar class name to export
   */
  protected final void addExportedTcJarClass(final String classname) {
    URL resource = TerracottaConfiguratorModule.class.getClassLoader().getResource(
                                                                                   ByteCodeUtil
                                                                                       .classNameToFileName(classname));

    if (resource == null) { throw new RuntimeException("Exported TC jar class " + classname + " does not exist."); }

    configHelper.addClassResource(classname, resource, false);
  }

  protected TransparencyClassSpec getOrCreateSpec(final String expr, final boolean markAsPreInstrumented) {
    final TransparencyClassSpec spec = configHelper.getOrCreateSpec(expr);
    if (markAsPreInstrumented) spec.markPreInstrumented();
    return spec;
  }

  protected TransparencyClassSpec getOrCreateSpec(final String expr) {
    return getOrCreateSpec(expr, true);
  }

  protected void addLock(final String expr, final LockDefinition ld) {
    configHelper.addLock(expr, ld);
  }

  protected Bundle getExportedBundle(final BundleContext context, final String targetBundleName) {
    // find the bundle that contains the replacement classes
    for (Bundle bundle : context.getBundles()) {
      if (BundleSpec.isMatchingSymbolicName(targetBundleName, bundle.getSymbolicName())) {
        //
        return bundle;
      }
    }
    return null;
  }

  private static class BytesUrlHandler extends URLStreamHandler {

    private final byte data[];

    public BytesUrlHandler(byte data[]) {
      this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) {
      return new URLConnection(u) {

        @Override
        public void connect() {
          //
        }

        @Override
        public InputStream getInputStream() {
          return new ByteArrayInputStream(data);
        }

      };
    }
  }

}
