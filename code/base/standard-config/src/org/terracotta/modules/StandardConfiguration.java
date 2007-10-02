/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
    }

    private void configUnsafe() {
        getOrCreateSpec("sun.misc.Unsafe");
        configHelper.addCustomAdapter("sun.misc.Unsafe", new UnsafeAdapter());
        getOrCreateSpec("com.tcclient.util.DSOUnsafe");
        configHelper.addCustomAdapter("com.tcclient.util.DSOUnsafe",
                new DSOUnsafeAdapter());
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
