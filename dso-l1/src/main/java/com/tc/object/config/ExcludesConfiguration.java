/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.bundles.LegacyDefaultModuleBase;
import com.tc.object.config.StandardDSOClientConfigHelper;

public class ExcludesConfiguration extends LegacyDefaultModuleBase {

  public ExcludesConfiguration(StandardDSOClientConfigHelper configHelper) {
    super(configHelper);
  }

  @Override
  public void apply() {
    configAutoLockExcludes();
    configPermanentExcludes();
    configNonPortables();
  }

  private void configNonPortables() {
    configHelper.addNonportablePattern("javax.servlet.GenericServlet");
  }

  private void configAutoLockExcludes() {
    configHelper.addAutoLockExcludePattern("* java.lang.Throwable.*(..)");
  }

  private void configPermanentExcludes() {
    configHelper.addPermanentExcludePattern("java.awt.Component");
    configHelper.addPermanentExcludePattern("java.lang.Thread");
    configHelper.addPermanentExcludePattern("java.lang.ThreadLocal");
    configHelper.addPermanentExcludePattern("java.lang.ThreadGroup");
    configHelper.addPermanentExcludePattern("java.lang.Process");
    configHelper.addPermanentExcludePattern("java.lang.ClassLoader");
    configHelper.addPermanentExcludePattern("java.lang.Runtime");
    configHelper.addPermanentExcludePattern("java.io.FileReader");
    configHelper.addPermanentExcludePattern("java.io.FileWriter");
    configHelper.addPermanentExcludePattern("java.io.FileDescriptor");
    configHelper.addPermanentExcludePattern("java.io.FileInputStream");
    configHelper.addPermanentExcludePattern("java.io.FileOutputStream");
    configHelper.addPermanentExcludePattern("java.net.DatagramSocket");
    configHelper.addPermanentExcludePattern("java.net.DatagramSocketImpl");
    configHelper.addPermanentExcludePattern("java.net.MulticastSocket");
    configHelper.addPermanentExcludePattern("java.net.ServerSocket");
    configHelper.addPermanentExcludePattern("java.net.Socket");
    configHelper.addPermanentExcludePattern("java.net.SocketImpl");
    configHelper.addPermanentExcludePattern("java.nio.channels.DatagramChannel");
    configHelper.addPermanentExcludePattern("java.nio.channels.FileChannel");
    configHelper.addPermanentExcludePattern("java.nio.channels.FileLock");
    configHelper.addPermanentExcludePattern("java.nio.channels.ServerSocketChannel");
    configHelper.addPermanentExcludePattern("java.nio.channels.SocketChannel");
    configHelper.addPermanentExcludePattern("java.util.logging.FileHandler");
    configHelper.addPermanentExcludePattern("java.util.logging.SocketHandler");
    configHelper.addPermanentExcludePattern("com.sun.crypto.provider..*");

    //
    configHelper.addPermanentExcludePattern("java.util.WeakHashMap+");
    configHelper.addPermanentExcludePattern("java.lang.ref.*");

    // unsupported java.util.concurrent types
    configHelper.addPermanentExcludePattern("java.util.concurrent.AbstractExecutorService");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ArrayBlockingQueue*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentLinkedQueue*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListMap*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListSet*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.CountDownLatch*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.DelayQueue*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.Exchanger*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ExecutorCompletionService*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.LinkedBlockingDeque*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.PriorityBlockingQueue*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ScheduledThreadPoolExecutor*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.Semaphore*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.SynchronousQueue*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.ThreadPoolExecutor*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerArray*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerFieldUpdater*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongArray*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongFieldUpdater*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicMarkableReference*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceArray*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceFieldUpdater*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.atomic.AtomicStampedReference*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedLongSynchronizer*");
    // configHelper.addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedSynchronizer*");
    configHelper.addPermanentExcludePattern("java.util.concurrent.locks.LockSupport*");
  }

}
