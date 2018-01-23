/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.cluster.ClusterEventsNotifier;
import com.tc.cluster.ClusterInternalEventsContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Handler firing the dso cluster internal events to the listeners
 */
public class ClusterInternalEventsHandler<EC> extends AbstractEventHandler<EC> {

  private static final Logger logger = LoggerFactory.getLogger(ClusterInternalEventsHandler.class);
  private static final int               EXECUTOR_MAX_THREADS = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_THREADS,
                                                                          20);
  private static final long              EXECUTOR_MAX_WAIT_SECONDS = TCPropertiesImpl
                                                                       .getProperties()
                                                                       .getLong(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_WAIT_SECONDS,
                                                                               60);
  private final ClusterEventsNotifier    clusterEventsNotifier;
  private final ClusterEventExecutor     clusterEventExecutor      = new ClusterEventExecutor();

  private static class ClusterEventExecutor {

    private final DaemonThreadFactory daemonThreadFactory = new DaemonThreadFactory();
    private final ThreadPoolExecutor  eventExecutor       = new ThreadPoolExecutor(1, EXECUTOR_MAX_THREADS, 60L,
                                                                                   TimeUnit.SECONDS,
                                                                                   new SynchronousQueue<Runnable>(),
                                                                                   daemonThreadFactory,
                                                                                   new ThreadPoolExecutor.DiscardPolicy());

    public ThreadPoolExecutor getExecutorService() {
      return eventExecutor;
    }
  }

  private static class DaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger();
    private final String        threadName   = "cluster-events-processor-";

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, threadName + threadNumber.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    }
  }

  public ClusterInternalEventsHandler(ClusterEventsNotifier eventsNotifier) {
    this.clusterEventsNotifier = eventsNotifier;
  }

  @Override
  public void handleEvent(final EC context) {
    ThreadPoolExecutor service = clusterEventExecutor.getExecutorService();
    Future<?> eventFuture = service.submit(new Runnable() {
      @Override
      public void run() {
        if (context instanceof ClusterInternalEventsContext) {
          ClusterInternalEventsContext eventContext = (ClusterInternalEventsContext) context;
          clusterEventsNotifier.notifyClusterListener(eventContext.getEventType(), eventContext.getEvent(),
                                                            eventContext.getClusterListener());
        } else {
          throw new AssertionError("Unknown Context " + context);
        }
      }
    });

    try {
      eventFuture.get(EXECUTOR_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.info("clusterEventExecutor interrupted while waiting for result context :" + context, e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    } catch (TimeoutException e) {
      logger.info("clusterEventExecutor timedout while waiting for result context :" + context, e);
    }
  }

  @Override
  public synchronized void destroy() {
    super.destroy();
    clusterEventExecutor.getExecutorService().shutdownNow();
  }
}
