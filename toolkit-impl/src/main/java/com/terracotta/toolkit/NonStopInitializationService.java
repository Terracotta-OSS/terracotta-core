/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookupAsync;
import com.terracotta.toolkit.nonstop.NonStopContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class NonStopInitializationService<T extends ToolkitObject> {

  private static final String   CORE_POOL_SIZE_CONFIG_STRING  = "com.tc.non.stop.toolkit.threadpool.core.size";
  private static final String   MAX_POOL_SIZE_CONFIG_STRING   = "com.tc.non.stop.toolkit.threadpool.max.size";
  private static final String   KEEP_ALIVE_TIME_CONFIG_STRING = "com.tc.non.stop.toolkit.threadpool.keep.alive";
  private static final String   QUEUE_SIZE_CONFIG_STRING      = "com.tc.non.stop.toolkit.threadpool.queue.size";
  private static final int      CORE_POOL_SIZE_DEFAULT  = 5;
  private static final int      MAX_POOL_SIZE_DEFAULT   = 50;
  private static final int      KEEP_ALIVE_TIME_DEFAULT = 60;
  private static final int      QUEUE_SIZE_DEFAULT      = 100;

  private final ExecutorService threadPool;
  private final NonStopContext  context;

  public NonStopInitializationService(NonStopContext context) {
    this.context = context;
    this.threadPool = getThreadPool();

  }


  /**
   * This method should be called when the associated NonStopToolkit is shutdown.
   * Once shutdowm, the initialization service cannot be started again.
   */
  public void shutdown() {
    threadPool.shutdownNow();
  }


  public void initialize(final AbstractToolkitObjectLookupAsync<T> toolkitObjectLookup,
                         NonStopConfiguration nonStopConfiguration) {
    if (nonStopConfiguration.isEnabled()) {
      // Create a separate Thread for creating toolkit object
      Callable<Boolean> callable = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return toolkitObjectLookup.initialize();
        }
      };

      // Submit the Thread for execution
      Future<Boolean> future = threadPool.submit(callable);

      // wait for initialization to complete (until operations are enabled)
      waitForInitialization(future, nonStopConfiguration.getTimeoutMillis());
    } else {
      toolkitObjectLookup.initialize();
    }
  }


  /**
   * This method holds the calling thread until the given Future returns the result
   * or the Cluster operations get/are disabled.
   * 
   * In case the Cluster operations get disabled while waiting, the method call returns
   * honoring the nonstop timeout.
   * 
   * @throws NonStopToolkitInstantiationException if any exception is thrown by the Future task.
   */
  private void waitForInitialization(Future<Boolean> future, long nonStopTimeOutInMillis) {
    boolean interrupted = false;
    boolean initializationCompletd = false;
    try {
      do {
        try {
          initializationCompletd = future.get(nonStopTimeOutInMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          interrupted = true;
        } catch (ExecutionException e) {
          if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException) e.getCause();
          } else {
            // This wont happen because the associated Callable never throws a CheckedException
            throw new ToolkitRuntimeException(e.getCause());
          }
        } catch (TimeoutException e) {
          // Retry if operations are enabled
        }
      } while (!initializationCompletd && areOperationsEnabled());

    } finally {
      if (interrupted) Thread.currentThread().interrupt();
    }
  }


  private boolean areOperationsEnabled() {
    return context.getNonStopClusterListener().areOperationsEnabled();
  }


  private ExecutorService getThreadPool() {
    int corePoolSize = Integer.getInteger(CORE_POOL_SIZE_CONFIG_STRING, CORE_POOL_SIZE_DEFAULT);
    int maxPoolSize = Integer.getInteger(MAX_POOL_SIZE_CONFIG_STRING, MAX_POOL_SIZE_DEFAULT);
    int keepAliveTime = Integer.getInteger(KEEP_ALIVE_TIME_CONFIG_STRING, KEEP_ALIVE_TIME_DEFAULT);
    int queueSize = Integer.getInteger(QUEUE_SIZE_CONFIG_STRING, QUEUE_SIZE_DEFAULT);

    ThreadFactory daemonThreadFactory = new ThreadFactory() {
      private final AtomicInteger threadID = new AtomicInteger();
      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "NonStopInitializationThread_" + threadID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }
    };

    return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                                  new ArrayBlockingQueue<Runnable>(queueSize), daemonThreadFactory);
  }

}
