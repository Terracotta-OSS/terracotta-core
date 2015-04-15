/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.ToolkitRuntimeException;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.ThreadUtil;
import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookupAsync;
import com.terracotta.toolkit.nonstop.NonStopContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class NonStopInitializationService<T extends ToolkitObject> {
  private static final TCLogger LOGGER                        = TCLogging.getLogger(NonStopInitializationService.class);
  private static final String   CORE_POOL_SIZE_CONFIG_STRING  = "com.tc.non.stop.toolkit.threadpool.core.size";
  private static final String   MAX_POOL_SIZE_CONFIG_STRING   = "com.tc.non.stop.toolkit.threadpool.max.size";
  private static final String   KEEP_ALIVE_TIME_CONFIG_STRING = "com.tc.non.stop.toolkit.threadpool.keep.alive";
  private static final String   MAX_RETRIES_STRING               = "com.tc.non.stop.toolkit.initialization.max.retries";
  private static final String   MAX_RETRY_TIMEOUT_MILLIS_STRING  = "com.tc.non.stop.toolkit.initialization.max.timeout.retries";

  private static final int      CORE_POOL_SIZE_DEFAULT  = 5;
  private static final int      MAX_POOL_SIZE_DEFAULT   = 50;
  private static final int      KEEP_ALIVE_TIME_DEFAULT = 60;
  private static final int      MAX_RETRIES_DEFAULT              = 10;
  private static final long     MAX_RETRY_TIMEOUT_MILLIS_DEFAULT = 300;

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

          boolean result = false;
          boolean recoverableException = false;
          int retryCount = Integer.getInteger(MAX_RETRIES_STRING, MAX_RETRIES_DEFAULT);

          do {
            recoverableException = false;
            try {
              result = toolkitObjectLookup.initialize();
            } catch (RejoinException e) {
              LOGGER.error("Rejoin Exception", e);
              recoverableException = retryCount-- > 0;
              if (!recoverableException) {
                throw e;
              }
              LOGGER.debug("Retrying NonStopServiceInitialization => Retry Count : " + retryCount);
              waitForNextRetry();
            }
          } while (recoverableException);

          return result;
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
   * This method holds the calling thread until the given Future returns the result or the Cluster operations get/are
   * disabled. In case the Cluster operations get disabled while waiting, the method call returns honoring the nonstop
   * timeout.
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

    if (!initializationCompletd && LOGGER.isDebugEnabled()) {
      LOGGER.debug("Returning without completing Cache initialization. Operations Enabled = " + areOperationsEnabled());
    }
  }

  private boolean areOperationsEnabled() {
    return context.getNonStopClusterListener().areOperationsEnabled();
  }

  private boolean waitForNextRetry() {
    long sleepTime = Long.getLong(MAX_RETRY_TIMEOUT_MILLIS_STRING, MAX_RETRY_TIMEOUT_MILLIS_DEFAULT);
    ThreadUtil.reallySleep(sleepTime); // delay between retry attempts
    return true;
  }

  private ExecutorService getThreadPool() {
    int corePoolSize = Integer.getInteger(CORE_POOL_SIZE_CONFIG_STRING, CORE_POOL_SIZE_DEFAULT);
    int maxPoolSize = Integer.getInteger(MAX_POOL_SIZE_CONFIG_STRING, MAX_POOL_SIZE_DEFAULT);
    int keepAliveTime = Integer.getInteger(KEEP_ALIVE_TIME_CONFIG_STRING, KEEP_ALIVE_TIME_DEFAULT);

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
                                  new LinkedBlockingQueue<Runnable>(), daemonThreadFactory);
  }

}
