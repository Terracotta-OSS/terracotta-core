/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.runtime.Vm;

public class Jdk15PreInstrumentedConfiguration
      extends TerracottaConfiguratorModule {

   protected void addInstrumentation(final BundleContext context) {
      super.addInstrumentation(context);
      addJDK15PreInstrumentedSpec();
   }

   private void addJDK15PreInstrumentedSpec() {
      if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
         getOrCreateSpec("sun.misc.Unsafe");
         configHelper.addCustomAdapter("sun.misc.Unsafe",
               StandardDSOClientConfigHelper.UNSAFE_CLASSADAPTER_FACTORY);
         getOrCreateSpec("com.tcclient.util.DSOUnsafe");
         configHelper.addCustomAdapter("com.tcclient.util.DSOUnsafe",
               StandardDSOClientConfigHelper.DSOUNSAFE_CLASSADAPTER_FACTORY);
         getOrCreateSpec("java.util.concurrent.CyclicBarrier");
         TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.CyclicBarrier$Generation");
         spec.setHonorJDKSubVersionSpecific(true);
         getOrCreateSpec("java.util.concurrent.TimeUnit");

         // ---------------------------------------------------------------------
         // The following section of specs are specified in the BootJarTool
         // also.
         // They are placed again so that the honorTransient flag will
         // be honored during runtime.
         // ---------------------------------------------------------------------

         // ---------------------------------------------------------------------
         // SECTION BEGINS
         // ---------------------------------------------------------------------

         spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock");
         spec.setHonorTransient(true);
         spec.setCallConstructorOnLoad(true);

         // addJavaUtilConcurrentHashMapSpec();
         // addLogicalAdaptedLinkedBlockingQueueSpec();
         addJavaUtilConcurrentFutureTaskSpec();

         // ---------------------------------------------------------------------
         // SECTION ENDS
         // ---------------------------------------------------------------------
      }
   }

   private void addJavaUtilConcurrentFutureTaskSpec() {
      if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() >= 6) {
         getOrCreateSpec("java.util.concurrent.locks.AbstractOwnableSynchronizer");
      }

      TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.FutureTask$Sync");
      configHelper
            .addWriteAutolock("* java.util.concurrent.FutureTask$Sync.*(..)");
      spec.setHonorTransient(true);
      spec.addDistributedMethodCall("managedInnerCancel", "()V", false);
      getOrCreateSpec("java.util.concurrent.FutureTask");
      getOrCreateSpec("java.util.concurrent.Executors$RunnableAdapter");
   }
}
