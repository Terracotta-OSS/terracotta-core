/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.TransparencyClassSpec;

public class StandardConfiguration extends TerracottaConfiguratorModule {

  protected void addInstrumentation(final BundleContext context) {
    super.addInstrumentation(context);
    configFileTypes();
    configEventTypes();
    configExceptionTypes();
    configArrayTypes();
    configUnsafe();
    configTroveTypes();
    configJavaProxyTypes();
    configBackportConcurrentTypes();
  }

  private void configTroveTypes() {
    configHelper.addIncludePattern("gnu.trove..*", false, false, true);
  }

  private void configJavaProxyTypes() {
    configHelper.addIncludePattern("java.lang.reflect.Proxy", false, false, false);
    // TODO remove if we find a better way using ProxyApplicator etc.
    configHelper.addIncludePattern("$Proxy..*", false, false, true);
  }

  private void configBackportConcurrentTypes() {
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractCollection", false, false, false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractQueue", false, false, false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue", false, false,
                                   false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue$Node", false,
                                   false, false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.FutureTask", false, false, false);

    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue", false,
                                   false, false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.PriorityBlockingQueue", false,
                                   false, false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue", false, false,
                                   false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList", false, false,
                                   false);
    configHelper.addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap", false, false,
                                   false);

  }

  private void configUnsafe() {
    TransparencyClassSpec spec;

    spec = getOrCreateSpec("sun.misc.Unsafe");
    spec.setCustomClassAdapter(new UnsafeAdapter());
    spec.markPreInstrumented();

    spec = getOrCreateSpec("com.tcclient.util.DSOUnsafe");
    spec.setCustomClassAdapter(new DSOUnsafeAdapter());
    spec.markPreInstrumented();
  }

  private void configArrayTypes() {
    final TransparencyClassSpec spec = getOrCreateSpec("java.util.Arrays");
    spec.addDoNotInstrument("copyOfRange");
    spec.addDoNotInstrument("copyOf");
    getOrCreateSpec("java.util.Arrays$ArrayList");
  }

  private void configFileTypes() {
    final TransparencyClassSpec spec = getOrCreateSpec("java.io.File");
    spec.setHonorTransient(true);
  }

  private void configEventTypes() {
    getOrCreateSpec("java.util.EventObject");
  }

  private void configExceptionTypes() {
    getOrCreateSpec("java.lang.Exception");
    getOrCreateSpec("java.lang.RuntimeException");
    getOrCreateSpec("java.lang.InterruptedException");
    getOrCreateSpec("java.awt.AWTException");
    getOrCreateSpec("java.io.IOException");
    getOrCreateSpec("java.io.FileNotFoundException");
    getOrCreateSpec("java.lang.Error");
    getOrCreateSpec("java.util.ConcurrentModificationException");
    getOrCreateSpec("java.util.NoSuchElementException");
  }

}
