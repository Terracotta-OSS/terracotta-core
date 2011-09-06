/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.bundles.LegacyDefaultModuleBase;
import com.tc.object.bytecode.DSOUnsafeAdapter;
import com.tc.object.bytecode.UnsafeAdapter;

public class StandardConfiguration extends LegacyDefaultModuleBase {

  public StandardConfiguration(StandardDSOClientConfigHelper configHelper) {
    super(configHelper);
  }

  @Override
  public void apply() {
    configFileTypes();
    configEventTypes();
    configExceptionTypes();
    configArrayTypes();
    configUnsafe();
    configThirdParty();
    configProxy();
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

  private void configThirdParty() {
    configHelper.addIncludePattern("gnu.trove..*", false, false, true);
  }

  private void configProxy() {
    configHelper.addIncludePattern("java.lang.reflect.Proxy", false, false, false);
    configHelper.addIncludePattern("com.tc.aspectwerkz.proxy..*", false, false, true);

    // TODO remove if we find a better way using ProxyApplicator etc.
    configHelper.addIncludePattern("$Proxy..*", false, false, true);
  }

}
