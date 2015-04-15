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
package com.tc.management;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadPreferenceExecutor;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.management.remote.message.Message;

public class RemoteJMXProcessor implements Sink {

  private static final TCLogger logger = TCLogging.getLogger(RemoteJMXProcessor.class);

  private final Executor        executor;

  {
    TCProperties props = TCPropertiesImpl.getProperties();
    int maxThreads = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
    int idleTime = props.getInt(TCPropertiesConsts.L2_REMOTEJMX_IDLETIME);

    // we're not using a standard thread pool executor here since it seems that some jmx tasks are inter-dependent (such
    // that if they are queued, things will lock up)
    executor = new ThreadPreferenceExecutor(getClass().getSimpleName(), maxThreads, idleTime, TimeUnit.SECONDS,
                                            TCLogging.getLogger(RemoteJMXProcessor.class));
  }

  @Override
  public void add(final EventContext context) {
    final CallbackExecuteContext callbackContext = (CallbackExecuteContext) context;

    try {
      int retries = 0;
      while (true) {
        try {
          executor.execute(new Runnable() {
            @Override
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
          break;
        } catch (RejectedExecutionException e) {
          ThreadUtil.reallySleep(10);
          retries++;
        }
        if (retries % 100 == 0) {
          logger.warn("JMX Processor is saturated. Retried processing a request " + retries + " times.");
        }
      }
    } catch (Throwable t) {
      callbackContext.getFuture().setException(t);
    }
  }

  @Override
  public boolean addLossy(EventContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMany(Collection contexts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AddPredicate getPredicate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAddPredicate(AddPredicate predicate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStats(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetStats() {
    throw new UnsupportedOperationException();
  }

}
