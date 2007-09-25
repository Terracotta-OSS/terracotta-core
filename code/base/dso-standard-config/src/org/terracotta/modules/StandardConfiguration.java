/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.runtime.Vm;

public class StandardConfiguration
      extends TerracottaConfiguratorModule {

   protected void addInstrumentation(final BundleContext context) {
      super.addInstrumentation(context);
      configFileTypes();
      configEventTypes();
      configExceptionTypes();
      configArrayTypes();
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
      final TransparencyClassSpec spec = getOrCreateSpec("java.util.EventObject");
      spec.setHonorTransient(true);
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
