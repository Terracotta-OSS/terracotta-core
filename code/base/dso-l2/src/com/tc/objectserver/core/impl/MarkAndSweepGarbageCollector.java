/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerEventListener;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.GCLogger;
import com.tc.objectserver.impl.GCStatsImpl;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.exceptions.AgentStatisticsManagerException;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet2;
import com.tc.util.State;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.NullLifeCycleState;
import com.tc.util.concurrent.StoppableThread;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MarkAndSweepGarbageCollector implements GarbageCollector {

  private static final TCLogger          logger                    = TCLogging
                                                                       .getLogger(MarkAndSweepGarbageCollector.class);
  private static final ChangeCollector   NULL_CHANGE_COLLECTOR     = new ChangeCollector() {
                                                                     public void changed(ObjectID changedObject,
                                                                                         ObjectID oldReference,
                                                                                         ObjectID newReference) {
                                                                       return;
                                                                     }

                                                                     public void addNewReferencesTo(Set set) {
                                                                       return;
                                                                     }

                                                                     public PrettyPrinter prettyPrint(PrettyPrinter out) {
                                                                       return out.println("NULL CHANGE COLLECTOR");
                                                                     }
                                                                   };
  private static final Filter            NULL_FILTER               = new Filter() {
                                                                     public boolean shouldVisit(
                                                                                                ObjectID referencedObject) {
                                                                       return true;
                                                                     }
                                                                   };
  private static final LifeCycleState    NULL_LIFECYCLE_STATE      = new NullLifeCycleState();
  public static final String             DISTRIBUTED_GC_STATISTICS = "distributed gc";

  private static final State             GC_DISABLED               = new State("GC_DISABLED");
  private static final State             GC_RUNNING                = new State("GC_RUNNING");
  private static final State             GC_SLEEP                  = new State("GC_SLEEP");
  private static final State             GC_PAUSING                = new State("GC_PAUSING");
  private static final State             GC_PAUSED                 = new State("GC_PAUSED");
  private static final State             GC_DELETE                 = new State("GC_DELETE");

  private final GCLogger                 gcLogger;
  private final List                     eventListeners            = new CopyOnWriteArrayList();
  private final AtomicInteger            gcIterationCounter        = new AtomicInteger(0);
  private final ObjectManager            objectManager;
  private final ClientStateManager       stateManager;
  private final StatisticsAgentSubSystem statisticsAgentSubSystem;

  private State                          state                     = GC_SLEEP;
  private LifeCycleState                 lifeCycleState;
  private volatile ChangeCollector       referenceCollector        = NULL_CHANGE_COLLECTOR;
  private LifeCycleState                 gcState                   = new NullLifeCycleState();
  private volatile boolean               started                   = false;

  public MarkAndSweepGarbageCollector(ObjectManager objectManager, ClientStateManager stateManager, boolean verboseGC,
                                      StatisticsAgentSubSystem agentSubSystem) {
    this.gcLogger = new GCLogger(logger, verboseGC);
    this.objectManager = objectManager;
    this.stateManager = stateManager;
    this.statisticsAgentSubSystem = agentSubSystem;
    Assert.assertNotNull(statisticsAgentSubSystem);
  }

  private Set rescue(final Set gcResults, final List rescueTimes) {
    long start = System.currentTimeMillis();
    Set rescueIds = new ObjectIDSet2();
    stateManager.addAllReferencedIdsTo(rescueIds);
    int stateManagerIds = rescueIds.size();

    addNewReferencesTo(rescueIds);
    int referenceCollectorIds = rescueIds.size() - stateManagerIds;

    logger.debug("rescueIds: " + rescueIds.size() + ", stateManagerIds: " + stateManagerIds
                 + ", additional referenceCollectorIds: " + referenceCollectorIds);

    rescueIds.retainAll(gcResults);

    Filter rescueFilter = new Filter() {
      public boolean shouldVisit(ObjectID referencedObject) {
        return gcResults.contains(referencedObject);
      }
    };

    Set rv = collect(rescueFilter, rescueIds, gcResults, gcState);
    rescueTimes.add(new Long(System.currentTimeMillis() - start));
    return rv;
  }

  public void gc() {

    while (!requestGCStart()) {
      gcLogger.log_GCDisabled();
      logger.info("GC is Disabled. Waiting for 1 min before checking again ...");
      ThreadUtil.reallySleep(60000);
    }

    int gcIteration = gcIterationCounter.incrementAndGet();
    GCStatsImpl gcStats = new GCStatsImpl(gcIteration);

    gcLogger.log_GCStart(gcIteration);
    long startMillis = System.currentTimeMillis();
    gcStats.setStartTime(startMillis);

    Set rootIDs = null;
    ObjectIDSet2 managedIDs = null;

    this.referenceCollector = new NewReferenceCollector();

    rootIDs = objectManager.getRootIDs();
    managedIDs = objectManager.getAllObjectIDs();

    gcStats.setBeginObjectCount(managedIDs.size());

    if (gcState.isStopRequested()) { return; }

    gcLogger.log_markStart(managedIDs);
    Set gcResults = collect(NULL_FILTER, rootIDs, managedIDs, gcState);
    gcLogger.log_markResults(gcResults);

    if (gcState.isStopRequested()) { return; }

    List rescueTimes = new ArrayList();

    gcLogger.log_rescue(1, gcResults);
    gcResults = rescue(gcResults, rescueTimes);

    requestGCPause();

    gcLogger.log_quiescing();

    if (gcState.isStopRequested()) { return; }

    objectManager.waitUntilReadyToGC();

    if (gcState.isStopRequested()) { return; }

    long pauseStartMillis = System.currentTimeMillis();
    gcLogger.log_paused();

    // Assert.eval("No pending lookups allowed during GC pause.", pending.size() == 0);

    gcLogger.log_rescue(2, gcResults);

    gcStats.setCandidateGarbageCount(gcResults.size());
    Set toDelete = Collections.unmodifiableSet(rescue(new ObjectIDSet2(gcResults), rescueTimes));

    if (gcState.isStopRequested()) { return; }
    gcLogger.log_sweep(toDelete);

    gcLogger.log_notifyGCComplete();

    this.referenceCollector = NULL_CHANGE_COLLECTOR;

    long deleteStartMillis = System.currentTimeMillis();
    // Delete Garbage
    deleteGarbage(new GCResultContext(gcIteration, toDelete));

    gcStats.setActualGarbageCount(toDelete.size());
    long endMillis = System.currentTimeMillis();
    gcStats.setElapsedTime(endMillis - startMillis);
    gcLogger.log_GCComplete(startMillis, pauseStartMillis, deleteStartMillis, rescueTimes, endMillis, gcIteration);

    gcLogger.push(gcStats);
    fireGCCompleteEvent(gcStats, toDelete);

    if (statisticsAgentSubSystem.isActive()) {
      storeGCStats(gcStats);
    }
  }

  public boolean deleteGarbage(GCResultContext gcResult) {
    if (requestGCDeleteStart()) {
      objectManager.notifyGCComplete(gcResult);
      notifyGCComplete();
      return true;
    }
    return false;
  }

  private void storeGCStats(GCStats gcStats) {
    Date moment = new Date();
    AgentStatisticsManager agentStatisticsManager = statisticsAgentSubSystem.getStatisticsManager();
    Collection sessions = agentStatisticsManager.getActiveSessionIDsForAction(DISTRIBUTED_GC_STATISTICS);
    if (sessions != null && sessions.size() > 0) {
      StatisticData[] datas = getGCStatisticsData(gcStats);
      storeStatisticsDatas(moment, sessions, datas);
    }
  }

  private StatisticData[] getGCStatisticsData(GCStats gcStats) {
    List<StatisticData> datas = new ArrayList<StatisticData>();
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "iteration", (long) gcStats.getIteration()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "start time", gcStats.getStartTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "elapsed time", gcStats.getElapsedTime()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "begin object count", gcStats.getBeginObjectCount()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "candidate garbage count", gcStats
        .getCandidateGarbageCount()));
    datas.add(new StatisticData(DISTRIBUTED_GC_STATISTICS, "actual garbage count", gcStats.getActualGarbageCount()));
    return datas.toArray(new StatisticData[datas.size()]);
  }

  private synchronized void storeStatisticsDatas(Date moment, Collection sessions, StatisticData[] datas) {
    try {
      for (Iterator sessionsIterator = sessions.iterator(); sessionsIterator.hasNext();) {
        String session = (String) sessionsIterator.next();
        for (int i = 0; i < datas.length; i++) {
          StatisticData data = datas[i];
          statisticsAgentSubSystem.getStatisticsManager().injectStatisticData(session, data.moment(moment));
        }
      }
    } catch (AgentStatisticsManagerException e) {
      logger.error("Unexpected error while trying to store Cache Objects Evict Request statistics statistics.", e);
    }
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    referenceCollector.changed(changedObject, oldReference, newReference);
  }

  public Set collect(Filter filter, Collection rootIds, Set managedObjectIds) {
    return collect(filter, rootIds, managedObjectIds, NULL_LIFECYCLE_STATE);
  }

  public Set collect(Filter filter, Collection rootIds, Set managedObjectIds, LifeCycleState aLifeCycleState) {
    this.lifeCycleState = aLifeCycleState;

    long start = System.currentTimeMillis();
    logstart_collect(rootIds, managedObjectIds);

    for (Iterator i = rootIds.iterator(); i.hasNext();) {
      ObjectID rootId = (ObjectID) i.next();
      managedObjectIds.remove(rootId);
      if (lifeCycleState.isStopRequested()) return Collections.EMPTY_SET;
      collectRoot(filter, rootId, managedObjectIds);
    }

    profile_collect(start);

    return managedObjectIds;
  }

  private void collectRoot(Filter filter, ObjectID rootId, Set managedObjectIds) {
    Set toBeVisited = new ObjectIDSet2();
    toBeVisited.add(rootId);

    while (!toBeVisited.isEmpty()) {

      for (Iterator i = new ObjectIDSet2(toBeVisited).iterator(); i.hasNext();) {
        ObjectID id = (ObjectID) i.next();
        if (lifeCycleState.isStopRequested()) return;
        ManagedObject obj = objectManager.getObjectByIDOrNull(id);
        toBeVisited.remove(id);
        if (obj == null) {
          logger.warn("Looked up a new Object before its initialized, skipping : " + id);
          continue;
        }

        for (Iterator r = obj.getObjectReferences().iterator(); r.hasNext();) {
          ObjectID mid = (ObjectID) r.next();
          if (mid.isNull() || !managedObjectIds.contains(mid)) continue;
          if (filter.shouldVisit(mid)) toBeVisited.add(mid);
          managedObjectIds.remove(mid);
        }
        objectManager.releaseReadOnly(obj);
      }
    }
  }

  private synchronized boolean requestGCStart() {
    if (started && state == GC_SLEEP) {
      state = GC_RUNNING;
      return true;
    }
    // Can't start GC
    return false;
  }

  public synchronized void enableGC() {
    if (GC_DISABLED == state) {
      state = GC_SLEEP;
    } else {
      logger.warn("GC is already enabled : " + state);
    }
  }

  public synchronized boolean disableGC() {
    if (GC_SLEEP == state) {
      state = GC_DISABLED;
      return true;
    }
    // GC is already running, can't be disabled
    return false;
  }

  public synchronized boolean isDisabled() {
    return GC_DISABLED == state;
  }

  public synchronized boolean isPausingOrPaused() {
    return GC_PAUSED == state || GC_PAUSING == state;
  }

  public synchronized boolean isPaused() {
    return state == GC_PAUSED;
  }

  public synchronized void requestGCPause() {
    state = GC_PAUSING;
  }

  public synchronized void notifyReadyToGC() {
    if (state == GC_PAUSING) {
      state = GC_PAUSED;
    }
  }

  /**
   * In Active server, state transitions from GC_PAUSED to GC_DELETE and in the passive server,
   * state transitions from GC_SLEEP to GC_DELETE.
   */
  private synchronized boolean requestGCDeleteStart() {
    if (state == GC_SLEEP || state == GC_PAUSED) {
      state = GC_DELETE;
      return true;
    }
    return false;
  }

  public synchronized void notifyGCComplete() {
    state = GC_SLEEP;
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName()).print("[").print(state).print("]");
  }

  private void logstart_collect(Collection rootIds, Set managedObjectIds) {
    if (logger.isDebugEnabled()) logger.debug("collect(): rootIds=" + rootIds.size() + ", managedObjectIds="
                                              + managedObjectIds.size());
  }

  private void profile_collect(long start) {
    if (logger.isDebugEnabled()) logger.debug("collect: " + (System.currentTimeMillis() - start) + " ms.");
  }

  private static class NewReferenceCollector implements ChangeCollector {

    Set newReferences = new ObjectIDSet2();

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      synchronized (newReferences) {
        newReferences.add(newReference);
      }
    }

    public void addNewReferencesTo(Set set) {
      long start = System.currentTimeMillis();
      synchronized (newReferences) {
        set.addAll(newReferences);
      }
      profile_addNewReferencesTo(start);
    }

    private void profile_addNewReferencesTo(long start) {
      if (logger.isDebugEnabled()) {
        logger.debug("addNewReferencesTo: " + (System.currentTimeMillis() - start) + " ms.");
      }
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      synchronized (newReferences) {
        return out.println("newReferences: ").println(newReferences);
      }
    }
  }

  private interface ChangeCollector extends ManagedObjectChangeListener, PrettyPrintable {
    public void addNewReferencesTo(Set set);
  }

  public void addNewReferencesTo(Set rescueIds) {
    referenceCollector.addNewReferencesTo(rescueIds);
  }

  public void start() {
    this.started = true;
    gcState.start();
  }

  public void stop() {
    this.started = false;
    int count = 0;
    while (!this.gcState.stopAndWait(5000) && (count < 6)) {
      count++;
      logger.warn("GC Thread did not stop");
    }
  }

  public boolean isStarted() {
    return this.started;
  }

  public void setState(StoppableThread st) {
    this.gcState = st;
  }

  private void fireGCCompleteEvent(GCStats gcStats, Set deleted) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      try {
        ObjectManagerEventListener listener = (ObjectManagerEventListener) iter.next();
        listener.garbageCollectionComplete(gcStats, deleted);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e);
        } else {
          logger.warn("Exception in GCComplete event callback: " + e.getMessage());
        }
      }
    }
  }

  public void addListener(ObjectManagerEventListener listener) {
    eventListeners.add(listener);
  }

  public GCStats[] getGarbageCollectorStats() {
    return gcLogger.getGarbageCollectorStats();
  }
}