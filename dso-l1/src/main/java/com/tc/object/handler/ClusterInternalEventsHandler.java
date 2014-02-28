/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoClusterEventsNotifier;

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
public class ClusterInternalEventsHandler extends AbstractEventHandler {

  private static final TCLogger          logger = TCLogging.getLogger(ClusterInternalEventsHandler.class);
  private static final int               EXECUTOR_MAX_THREADS = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_THREADS,
                                                                          20);
  private static final long              EXECUTOR_MAX_WAIT_SECONDS = TCPropertiesImpl
                                                                       .getProperties()
                                                                       .getLong(TCPropertiesConsts.L1_CLUSTEREVENT_EXECUTOR_MAX_WAIT_SECONDS,
                                                                               60);
  private final DsoClusterEventsNotifier dsoClusterEventsNotifier;
  private final ClusterEventExecutor     clusterEventExecutor      = new ClusterEventExecutor();

  private static class ClusterEventExecutor implements PrettyPrintable {

    private final DaemonThreadFactory daemonThreadFactory = new DaemonThreadFactory();
    private final ThreadPoolExecutor  eventExecutor       = new ThreadPoolExecutor(1, EXECUTOR_MAX_THREADS, 60L,
                                                                                   TimeUnit.SECONDS,
                                                                                   new SynchronousQueue<Runnable>(),
                                                                                   daemonThreadFactory,
                                                                                   new ThreadPoolExecutor.DiscardPolicy());

    public ThreadPoolExecutor getExecutorService() {
      return eventExecutor;
    }

    @Override
    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.print("clusterEventExecutor active: " + eventExecutor.getActiveCount() + " queue: "
                    + eventExecutor.getQueue().size()).flush();
      return out;
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

  public ClusterInternalEventsHandler(final DsoClusterEventsNotifier eventsNotifier) {
    this.dsoClusterEventsNotifier = eventsNotifier;
  }

  @Override
  public void handleEvent(final EventContext context) {
    ThreadPoolExecutor service = clusterEventExecutor.getExecutorService();
    Future eventFuture = service.submit(new Runnable() {
      @Override
      public void run() {
        if (context instanceof ClusterInternalEventsContext) {
          ClusterInternalEventsContext eventContext = (ClusterInternalEventsContext) context;
          dsoClusterEventsNotifier.notifyDsoClusterListener(eventContext.getEventType(), eventContext.getEvent(),
                                                            eventContext.getDsoClusterListener());
        } else {
          throw new AssertionError("Unknown Context " + context);
        }
      }
    });

    try {
      eventFuture.get(EXECUTOR_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("clusterEventExecutor interrupted while waiting for result context :" + context, e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    } catch (TimeoutException e) {
      logger.warn("clusterEventExecutor timedout while waiting for result context :" + context, e);
    }
  }

  @Override
  public synchronized void destroy() {
    super.destroy();
    clusterEventExecutor.getExecutorService().shutdownNow();
  }
}
