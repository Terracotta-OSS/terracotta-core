/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import org.apache.commons.collections.set.ListOrderedSet;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockManagerMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.tx.ServerTransactionManagerEventListener;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;
import com.tc.statistics.StatisticData;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.DoubleStatistic;
import com.tc.stats.statistics.Statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * This is the top-level MBean for the DSO subsystem, off which to hang JSR-77 Stats and Config MBeans.
 * 
 * @see DSOMBean
 * @see DSOStatsImpl
 */
public class DSO extends AbstractNotifyingMBean implements DSOMBean {

  private final static TCLogger               logger                 = TCLogging.getLogger(DSO.class);
  private final static String                 DSO_OBJECT_NAME_PREFIX = L2MBeanNames.DSO.getCanonicalName() + ",";

  private final DSOStatsImpl                  dsoStats;
  private final GCStatsEventPublisher         gcStatsPublisher;
  private final ObjectManagerMBean            objMgr;
  private final MBeanServer                   mbeanServer;
  private final ArrayList                     rootObjectNames        = new ArrayList();
  private final Set                           clientObjectNames      = new ListOrderedSet();
  private final Map<ObjectName, DSOClient>    clientMap              = new HashMap<ObjectName, DSOClient>();
  private final DSOChannelManagerMBean        channelMgr;
  private final ServerTransactionManagerMBean txnMgr;
  private final LockManagerMBean              lockMgr;
  private final ChannelStats                  channelStats;
  private final ObjectInstanceMonitorMBean    instanceMonitor;
  private final ClientStateManager            clientStateManager;

  public DSO(final ServerManagementContext managementContext, final ServerConfigurationContext configContext,
             final MBeanServer mbeanServer, final GCStatsEventPublisher gcStatsPublisher)
      throws NotCompliantMBeanException {
    super(DSOMBean.class);
    try {
      // TraceImplementation.init(TraceTags.LEVEL_TRACE);
    } catch (Exception e) {/**/
    }
    this.mbeanServer = mbeanServer;
    this.gcStatsPublisher = gcStatsPublisher;
    this.dsoStats = new DSOStatsImpl(managementContext);
    this.lockMgr = managementContext.getLockManager();
    this.objMgr = managementContext.getObjectManager();
    this.channelMgr = managementContext.getChannelManager();
    this.txnMgr = managementContext.getTransactionManager();
    this.channelStats = managementContext.getChannelStats();
    this.instanceMonitor = managementContext.getInstanceMonitor();
    this.clientStateManager = configContext.getClientStateManager();

    // add various listeners (do this before the setupXXX() methods below so we don't ever miss anything)
    txnMgr.addRootListener(new TransactionManagerListener());
    this.gcStatsPublisher.addListener(new DSOGCStatsEventListener());
    channelMgr.addEventListener(new ChannelManagerListener());

    setupRoots();
    setupClients();
  }

  public void reset() {
    // TODO: implement this?
  }

  public DSOStats getStats() {
    return dsoStats;
  }

  public CountStatistic getObjectFlushRate() {
    return getStats().getObjectFlushRate();
  }

  public long getNativeObjectFlushRate() {
    return getStats().getNativeObjectFlushRate();
  }

  public DoubleStatistic getCacheHitRatio() {
    return getStats().getCacheHitRatio();
  }

  public double getNativeCacheHitRatio() {
    return getStats().getNativeCacheHitRatio();
  }
  
  public CountStatistic getCacheMissRate() {
    return getStats().getCacheMissRate();
  }

  public long getNativeCacheMissRate() {
    return getStats().getNativeCacheMissRate();
  }
  
  public CountStatistic getTransactionRate() {
    return getStats().getTransactionRate();
  }

  public long getNativeTransactionRate() {
    return getStats().getNativeTransactionRate();
  }
  
  public CountStatistic getObjectFaultRate() {
    return getStats().getObjectFaultRate();
  }

  public long getNativeObjectFaultRate() {
    return getStats().getNativeObjectFaultRate();
  }
  
  public Statistic[] getStatistics(String[] names) {
    return getStats().getStatistics(names);
  }

  public GCStats[] getGarbageCollectorStats() {
    return gcStatsPublisher.getGarbageCollectorStats();
  }

  public ObjectName[] getRoots() {
    synchronized (rootObjectNames) {
      return (ObjectName[]) rootObjectNames.toArray(new ObjectName[rootObjectNames.size()]);
    }
  }

  public LockMBean[] getLocks() {
    return this.lockMgr.getAllLocks();
  }

  public ObjectName[] getClients() {
    synchronized (clientObjectNames) {
      return (ObjectName[]) clientObjectNames.toArray(new ObjectName[clientObjectNames.size()]);
    }
  }

  public DSOClassInfo[] getClassInfo() {
    Map counts = instanceMonitor.getInstanceCounts();
    List<DSOClassInfo> list = new ArrayList<DSOClassInfo>();

    Iterator<Map.Entry<String, Integer>> iter = counts.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Integer> entry = iter.next();
      list.add(new DSOClassInfo(entry.getKey(), entry.getValue()));
    }

    return list.toArray(new DSOClassInfo[list.size()]);
  }

  public ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException {
    return this.objMgr.lookupFacade(objectID, limit);
  }

  private void setupRoots() {
    for (Iterator iter = objMgr.getRootNames(); iter.hasNext();) {
      String name = (String) iter.next();
      final ObjectID rootID;
      try {
        rootID = objMgr.lookupRootID(name);
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }

      addRootMBean(name, rootID);
    }
  }

  private void setupClients() {
    MessageChannel[] channels = channelMgr.getActiveChannels();
    for (int i = 0; i < channels.length; i++) {
      addClientMBean(channels[i]);
    }
  }

  private ObjectName makeClientObjectName(MessageChannel channel) {
    try {
      return new ObjectName(DSO_OBJECT_NAME_PREFIX + "channelID=" + channel.getChannelID().toLong());
    } catch (MalformedObjectNameException e) {
      // this shouldn't happen
      throw new RuntimeException(e);
    }
  }

  private ObjectName makeRootObjectName(String name, ObjectID id) {
    try {
      return new ObjectName(DSO_OBJECT_NAME_PREFIX + "rootID=" + id.toLong());
    } catch (MalformedObjectNameException e) {
      // this shouldn't happen
      throw new RuntimeException(e);
    }
  }

  private void addRootMBean(String name, ObjectID rootID) {
    // XXX: There should be a cleaner way to do this ignore
    if (name.startsWith("@")) {
      // ignore iternal roots
      return;
    }

    synchronized (rootObjectNames) {
      ObjectName rootName = makeRootObjectName(name, rootID);
      if (mbeanServer.isRegistered(rootName)) {
        // this can happen since the initial root setup races with the object manager "root created" event
        logger.debug("Root MBean already registered for name " + rootName);
        return;
      }

      DSORoot dsoRoot = new DSORoot(rootID, objMgr, name);
      try {
        mbeanServer.registerMBean(dsoRoot, rootName);
        rootObjectNames.add(rootName);
        sendNotification(ROOT_ADDED, rootName);
      } catch (Exception e) {
        logger.error(e);
      }
    }
  }

  private void removeClientMBean(MessageChannel channel) {
    ObjectName clientName = makeClientObjectName(channel);

    synchronized (clientObjectNames) {
      try {
        if (mbeanServer.isRegistered(clientName)) {
          sendNotification(CLIENT_DETACHED, clientName);
          mbeanServer.unregisterMBean(clientName);
        }
      } catch (Exception e) {
        logger.error(e);
      } finally {
        clientObjectNames.remove(clientName);
        clientMap.remove(clientName);
      }
    }
  }

  private void addClientMBean(final MessageChannel channel) {
    synchronized (clientObjectNames) {
      ObjectName clientName = makeClientObjectName(channel);
      if (mbeanServer.isRegistered(clientName)) {
        logger.debug("channel MBean already registered for name " + clientName);
        return;
      }

      try {
        final DSOClient client = new DSOClient(mbeanServer, channel, channelStats, channelMgr.getClientIDFor(channel
            .getChannelID()), clientStateManager);
        mbeanServer.registerMBean(client, clientName);
        clientObjectNames.add(clientName);
        clientMap.put(clientName, client);
        sendNotification(CLIENT_ATTACHED, clientName);
      } catch (Exception e) {
        logger.error("Unable to register DSO client MBean", e);
      }
    }
  }

  public Map<ObjectName, CountStatistic> getAllPendingTransactionsCount() {
    Map<ObjectName, CountStatistic> map = new HashMap<ObjectName, CountStatistic>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        map.put(clientBeanName, clientMap.get(clientBeanName).getPendingTransactionsCount());
      }
    }
    return map;
  }

  public Map<ObjectName, CountStatistic> getClientTransactionRates() {
    Map<ObjectName, CountStatistic> result = new HashMap<ObjectName, CountStatistic>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getTransactionRate());
      }
    }
    return result;
  }

  public Map<ObjectName, StatisticData[]> getL1CpuUsages() {
    Map<ObjectName, StatisticData[]> result = new HashMap<ObjectName, StatisticData[]>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getCpuUsage());
      }
    }
    return result;
  }

  public Map<ObjectName, Map> getL1Statistics() {
    Map<ObjectName, Map> result = new HashMap<ObjectName, Map>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getStatistics());
      }
    }
    return result;
  }

  private static final ExecutorService pool = Executors.newCachedThreadPool();

  private static class PrimaryClientStatWorker implements Callable<Map> {
    private final ObjectName clientBeanName;
    private final DSOClient  client;

    private PrimaryClientStatWorker(ObjectName clientBeanName, DSOClient client) {
      this.clientBeanName = clientBeanName;
      this.client = client;
    }

    public Map call() {
      try {
        Map result = client.getStatistics();
        if (result != null) {
          result.put("TransactionRate", client.getTransactionRate());
          result.put("clientBeanName", clientBeanName);
        }
        return result;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /*
   * MemoryUsage, CpuUsage, TransactionRate
   */
  public Map<ObjectName, Map> getPrimaryClientStatistics() {
    Map<ObjectName, Map> result = new HashMap<ObjectName, Map>();
    List<Callable<Map>> tasks = new ArrayList<Callable<Map>>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        tasks.add(new PrimaryClientStatWorker(clientBeanName, clientMap.get(clientBeanName)));
      }
    }
    try {
      List<Future<Map>> results = pool.invokeAll(tasks, 2, TimeUnit.SECONDS);
      Iterator<Future<Map>> resultIter = results.iterator();
      while (resultIter.hasNext()) {
        Future<Map> future = resultIter.next();
        if (future.isDone()) {
          try {
            Map statsMap = future.get();
            if (statsMap != null) {
              result.put((ObjectName) statsMap.remove("clientBeanName"), statsMap);
            }
          } catch (Exception e) {
            /**/
          }
        }
      }
    } catch (InterruptedException ie) {/**/
    }
    return result;
  }

  public int getLiveObjectCount() {
    return objMgr.getLiveObjectCount();
  }

  public Map<ObjectName, Integer> getClientLiveObjectCount() {
    Map<ObjectName, Integer> result = new HashMap<ObjectName, Integer>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getLiveObjectCount());
      }
    }
    return result;
  }

  public boolean isResident(NodeID node, ObjectID oid) {
    return clientStateManager.hasReference(node, oid);
  }

  private class TransactionManagerListener implements ServerTransactionManagerEventListener {
    public void rootCreated(String name, ObjectID rootID) {
      addRootMBean(name, rootID);
    }
  }

  private class DSOGCStatsEventListener implements GCStatsEventListener {

    public void update(GCStats stats) {
      sendNotification(GC_STATUS_UPDATE, stats);
    }

  }

  private class ChannelManagerListener implements DSOChannelManagerEventListener {
    public void channelCreated(MessageChannel channel) {
      addClientMBean(channel);
    }

    public void channelRemoved(MessageChannel channel) {
      removeClientMBean(channel);
    }
  }
}
