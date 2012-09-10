/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.toolkit.TerracottaProperties;
import com.terracotta.toolkit.events.DestroyableToolkitNotifier;
import com.terracotta.toolkit.events.ToolkitNotifierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.ToolkitTypeRootsFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link ClusteredListFactory}
 */
public class ToolkitNotifierFactoryImpl extends
    AbstractPrimaryToolkitObjectFactory<ToolkitNotifier, ToolkitNotifierImpl> {
  public static final String                       TOOLKIT_NOTIFIER_EXECUTOR_SERVICE = "toolkitNotifierExecutorService";

  private static int                               MAX_NOTIFIER_THREAD_COUNT         = new TerracottaProperties()
                                                                                         .getInteger("maxToolkitNotifierThreadCount",
                                                                                                     20);

  private static final NotifierIsolatedTypeFactory FACTORY                           = new NotifierIsolatedTypeFactory();

  public ToolkitNotifierFactoryImpl(ToolkitInternal toolkit, ToolkitTypeRootsFactory rootsFactory) {
    super(toolkit, rootsFactory.createAggregateIsolatedTypeRoot(ToolkitTypeConstants.TOOLKIT_NOTIFIER_ROOT_NAME,
                                                                FACTORY));
    final ExecutorService notifierService = new ThreadPoolExecutor(0, MAX_NOTIFIER_THREAD_COUNT, 60L, TimeUnit.SECONDS,
                                                                   new SynchronousQueue<Runnable>(),
                                                                   new ThreadFactory() {
                                                                     private final AtomicInteger count = new AtomicInteger();

                                                                     @Override
                                                                     public Thread newThread(Runnable runnable) {
                                                                       Thread thread = new Thread(
                                                                                                  runnable,
                                                                                                  "ToolkitNotifier-"
                                                                                                      + count
                                                                                                          .incrementAndGet());
                                                                       thread.setDaemon(true);
                                                                       return thread;
                                                                     }
                                                                   });
    ExecutorService service = ManagerUtil.registerObjectByNameIfAbsent(TOOLKIT_NOTIFIER_EXECUTOR_SERVICE,
                                                                       notifierService);
    if (service == notifierService) {
      toolkit.registerBeforeShutdownHook(new Runnable() {
        @Override
        public void run() {
          notifierService.shutdown();
        }
      });
    }
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.NOTIFIER;
  }

  private static class NotifierIsolatedTypeFactory implements
      IsolatedToolkitTypeFactory<ToolkitNotifier, ToolkitNotifierImpl> {

    @Override
    public ToolkitNotifier createIsolatedToolkitType(ToolkitObjectFactory factory, String name, Configuration config,
                                                     ToolkitNotifierImpl tcClusteredObject) {
      return new DestroyableToolkitNotifier(factory, tcClusteredObject, name);
    }

    @Override
    public ToolkitNotifierImpl createTCClusteredObject(Configuration config) {
      return new ToolkitNotifierImpl();
    }

  }

}