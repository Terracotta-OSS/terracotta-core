/**
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.TransparencyClassSpec;
import com.tc.util.runtime.Vm;

public class Jdk15PreInstrumentedConfiguration extends TerracottaConfiguratorModule {

  protected void addInstrumentation(final BundleContext context) {
    super.addInstrumentation(context);
    addJDK15PreInstrumentedSpec();
  }

  private void addJDK15PreInstrumentedSpec() {
    if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
      getOrCreateSpec("java.util.concurrent.CyclicBarrier");
      TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.CyclicBarrier$Generation");
      spec.setHonorJDKSubVersionSpecific(true);
      getOrCreateSpec("java.util.concurrent.TimeUnit");

      if (!Vm.isIBM()) {
        spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicInteger");
        spec.setHonorVolatile(true);

        if (!Vm.isAzul()) {
          spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicLong");
          spec.setHonorVolatile(true);
        }
      }

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
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.FutureTask$Sync");
    configHelper.addWriteAutolock("* java.util.concurrent.FutureTask$Sync.*(..)");
    spec.setHonorTransient(true);
    spec.addDistributedMethodCall("managedInnerCancel", "()V", false);
    getOrCreateSpec("java.util.concurrent.FutureTask");
    getOrCreateSpec("java.util.concurrent.Executors$RunnableAdapter");
  }
}
