/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.PostInit;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.logging.DefaultLoggerProvider;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.Stats;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author steve
 */
public class StageManagerImpl implements StageManager {

  private static final boolean     MONITOR       = TCPropertiesImpl.getProperties()
                                                     .getBoolean(TCPropertiesConsts.TC_STAGE_MONITOR_ENABLED);
  private static final long        MONITOR_DELAY = TCPropertiesImpl.getProperties()
                                                     .getLong(TCPropertiesConsts.TC_STAGE_MONITOR_DELAY);

  private final Map<String, Stage<?>>   stages        = new ConcurrentHashMap<String, Stage<?>>();
  private final Map<String, Class<?>> classVerifications = new ConcurrentHashMap<String, Class<?>>();
  private TCLoggerProvider           loggerProvider;
  private final ThreadGroup          group;
  private String[]                   stageNames    = new String[] {};
  private final QueueFactory<?> queueFactory;
  private volatile boolean           started;

  public StageManagerImpl(ThreadGroup threadGroup, QueueFactory<?> queueFactory) {
    this.loggerProvider = new DefaultLoggerProvider();
    this.group = threadGroup;
    this.queueFactory = queueFactory;

    if (MONITOR) {
      startMonitor();
    }
  }

  private void startMonitor() {
    final TCLogger logger = loggerProvider.getLogger(getClass());
    Thread t = new Thread("SEDA Stage Monitor") {
      @Override
      public void run() {
        while (true) {
          printStats();
          ThreadUtil.reallySleep(MONITOR_DELAY);
        }
      }

      private void printStats() {
        try {
          Stats stats[] = StageManagerImpl.this.getStats();
          logger.info("Stage Depths");
          logger.info("=================================");
          for (Stats stat : stats) {
            stat.logDetails(logger);
          }
        } catch (Throwable th) {
          logger.error(th);
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }

  @Override
  public void setLoggerProvider(TCLoggerProvider loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public synchronized <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads, int maxSize) {
    return createStage(name, verification, handler, threads, threads, maxSize);
  }

  @Override
  public synchronized <EC> Stage<EC> createStage(String name, Class<EC> verification, EventHandler<EC> handler, int threads, int threadsToQueueRatio, int maxSize) {
    if (started) {
      throw new IllegalStateException("A new stage cannot be created, because StageManager is already started.");
    }

    int capacity = maxSize > 0 ? maxSize : Integer.MAX_VALUE;
    // Note that the queue factory is used by all the stages under this manager so it can't be type-safe.
    @SuppressWarnings("unchecked")
    QueueFactory<ContextWrapper<EC>> queueFactory = (QueueFactory<ContextWrapper<EC>>) this.queueFactory;
    Stage<EC> s = new StageImpl<EC>(loggerProvider, name, handler, threads, threadsToQueueRatio, group, queueFactory, capacity);
    addStage(name, s);
    this.classVerifications.put(name,  verification);
    return s;
  }

  private synchronized <EC> void addStage(String name, Stage<EC> s) {
    Object prev = stages.put(name, s);
    Assert.assertNull(prev);
    s.getSink().enableStatsCollection(MONITOR);
    stageNames = stages.keySet().toArray(new String[stages.size()]);
    Arrays.sort(stageNames);
  }

  @Override
  public synchronized void startAll(ConfigurationContext context, List<PostInit> toInit) {
    for (PostInit mgr : toInit) {
      mgr.initializeContext(context);

    }
    for (Stage<?> s : stages.values()) {
      s.start(context);
    }
    started = true;
  }

  @Override
  public void stopAll() {
    for (Stage<?> s : stages.values()) {
      s.destroy();
    }
    stages.clear();
    this.classVerifications.clear();
    started = false;
  }

  @Override
  public void cleanup() {
    // TODO: ClientConfigurationContext is not visible so can't use ClientConfigurationContext.CLUSTER_EVENTS_STAGE
    Collection<String> skipStages = new HashSet<String>();
    skipStages.add("cluster_events_stage");
    for (Stage<?> s : stages.values()) {
      if (!skipStages.contains(s.getName())) {
        s.getSink().clear();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public  <EC> Stage<EC> getStage(String name, Class<EC> verification) {
    Assert.assertTrue(this.classVerifications.get(name).equals(verification));
    return (Stage<EC>) stages.get(name);
  }

  @Override
  public synchronized Stats[] getStats() {
    final String[] names = stageNames;
    final Stats[] stats = new Stats[names.length];

    for (int i = 0; i < names.length; i++) {
      stats[i] = stages.get(names[i]).getSink().getStats(MONITOR_DELAY);
    }
    return stats;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    for (Stage<?> stage : stages.values()) {
      out.indent().visit(stage).flush();
    }
    return out;
  }
}
