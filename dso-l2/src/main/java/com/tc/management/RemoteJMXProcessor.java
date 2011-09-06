/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadPreferenceExecutor;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.management.remote.message.Message;

public class RemoteJMXProcessor implements Sink {

  private final Executor executor;

  {
    TCProperties props = TCPropertiesImpl.getProperties();
    int maxThreads = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
    int idleTime = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_IDLETIME);

    // we're not using a standard thread pool executor here since it seems that some jmx tasks are inter-dependent (such
    // that if they are queued, things will lock up)
    executor = new ThreadPreferenceExecutor(getClass().getSimpleName(), maxThreads, idleTime, TimeUnit.SECONDS,
                                            TCLogging.getLogger(RemoteJMXProcessor.class));
  }

  public void add(final EventContext context) {
    final CallbackExecuteContext callbackContext = (CallbackExecuteContext) context;

    try {
      executor.execute(new Runnable() {
        public void run() {
          Thread currentThread = Thread.currentThread();
          ClassLoader prevLoader = currentThread.getContextClassLoader();
          currentThread.setContextClassLoader(callbackContext.getThreadContextLoader());

          try {
            Message result = callbackContext.getCallback().execute(callbackContext.getRequest());
            callbackContext.getFuture().set(result);
          } catch (Throwable t) {
            callbackContext.getFuture().setException(t);
          } finally {
            currentThread.setContextClassLoader(prevLoader);
          }
        }
      });
    } catch (Throwable t) {
      callbackContext.getFuture().setException(t);
    }
  }

  public boolean addLossy(EventContext context) {
    throw new UnsupportedOperationException();
  }

  public void addMany(Collection contexts) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public AddPredicate getPredicate() {
    throw new UnsupportedOperationException();
  }

  public void setAddPredicate(AddPredicate predicate) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    throw new UnsupportedOperationException();
  }

  public void enableStatsCollection(boolean enable) {
    throw new UnsupportedOperationException();
  }

  public Stats getStats(long frequency) {
    throw new UnsupportedOperationException();
  }

  public Stats getStatsAndReset(long frequency) {
    throw new UnsupportedOperationException();
  }

  public boolean isStatsCollectionEnabled() {
    throw new UnsupportedOperationException();
  }

  public void resetStats() {
    throw new UnsupportedOperationException();
  }

}
