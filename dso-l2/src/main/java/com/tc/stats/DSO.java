/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import org.apache.commons.collections.set.ListOrderedSet;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionPolicy;
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
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.locks.LockManagerMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.StorageDataStats;
import com.tc.objectserver.tx.ServerTransactionManagerEventListener;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.stats.api.DSOClassInfo;
import com.tc.stats.api.DSOMBean;
import com.tc.stats.api.DSOStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.AttributeList;
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

  private final static TCLogger                        logger                 = TCLogging.getLogger(DSO.class);
  private final static String                          DSO_OBJECT_NAME_PREFIX = L2MBeanNames.DSO.getCanonicalName()
                                                                                + ",";

  private final DSOStatsImpl                           dsoStats;
  private final GCStatsEventPublisher                  gcStatsPublisher;
  private final ObjectManagerMBean                     objMgr;
  private final MBeanServer                            mbeanServer;
  private final ArrayList                              rootObjectNames        = new ArrayList();
  private final Set                                    clientObjectNames      = new ListOrderedSet();
  private final Map<ObjectName, DSOClient>             clientMap              = new HashMap<ObjectName, DSOClient>();
  private final DSOChannelManagerMBean                 channelMgr;
  private final ServerTransactionManagerMBean          txnMgr;
  private final LockManagerMBean                       lockMgr;
  private final ChannelStats                           channelStats;
  private final ObjectInstanceMonitorMBean             instanceMonitor;
  private final ClientStateManager                     clientStateManager;
  private final TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider;
  private final OffheapStats                           offheapStats;
  private final StorageDataStats                       storageStats;
  private final IndexManager                           indexManager;
  private final ConnectionPolicy                       connectionPolicy;

  public DSO(final ServerManagementContext managementContext, final ServerConfigurationContext configContext,
             final MBeanServer mbeanServer, final GCStatsEventPublisher gcStatsPublisher,
             TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider, OffheapStats offheapStats,
             StorageDataStats storageStats)
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
    this.indexManager = managementContext.getIndexManager();
    this.instanceMonitor = managementContext.getInstanceMonitor();
    this.clientStateManager = configContext.getClientStateManager();
    this.operatorEventHistoryProvider = operatorEventHistoryProvider;
    this.offheapStats = offheapStats;
    this.storageStats = storageStats;
    this.connectionPolicy = managementContext.getConnectionPolicy();

    // add various listeners (do this before the setupXXX() methods below so we don't ever miss anything)
    txnMgr.addRootListener(new TransactionManagerListener());
    this.gcStatsPublisher.addListener(new DSOGCStatsEventListener());
    channelMgr.addEventListener(new ChannelManagerListener());

    setupRoots();
    setupClients();
  }

  @Override
  public void optimizeSearchIndex(String indexName) {
    indexManager.optimizeSearchIndex(indexName);
  }

  @Override
  public String[] getSearchIndexNames() {
    return indexManager.getSearchIndexNames();
  }

  @Override
  public void reset() {
    // TODO: implement this?
  }

  @Override
  public DSOStats getStats() {
    return dsoStats;
  }

  @Override
  public long getTransactionRate() {
    return getStats().getTransactionRate();
  }

  @Override
  public long getReadOperationRate() {
    return getStats().getReadOperationRate();
  }

  @Override
  public long getGlobalLockRecallRate() {
    return getStats().getGlobalLockRecallRate();
  }

  @Override
  public long getTransactionSizeRate() {
    return getStats().getTransactionSizeRate();
  }

  @Override
  public long getBroadcastRate() {
    return getStats().getBroadcastRate();
  }

  @Override
  public Number[] getStatistics(String[] names) {
    return getStats().getStatistics(names);
  }

  @Override
  public GCStats[] getGarbageCollectorStats() {
    return gcStatsPublisher.getGarbageCollectorStats();
  }

  @Override
  public List<TerracottaOperatorEvent> getOperatorEvents() {
    return this.operatorEventHistoryProvider.getOperatorEvents();
  }

  @Override
  public List<TerracottaOperatorEvent> getOperatorEvents(long sinceTimestamp) {
    return this.operatorEventHistoryProvider.getOperatorEvents(sinceTimestamp);
  }

  @Override
  public boolean markOperatorEvent(TerracottaOperatorEvent operatorEvent, boolean read) {
    return operatorEventHistoryProvider.markOperatorEvent(operatorEvent, read);
  }

  @Override
  public Map<String, Integer> getUnreadOperatorEventCount() {
    return operatorEventHistoryProvider.getUnreadCounts();
  }

  @Override
  public ObjectName[] getRoots() {
    synchronized (rootObjectNames) {
      return (ObjectName[]) rootObjectNames.toArray(new ObjectName[rootObjectNames.size()]);
    }
  }

  @Override
  public LockMBean[] getLocks() {
    return this.lockMgr.getAllLocks();
  }

  @Override
  public ObjectName[] getClients() {
    synchronized (clientObjectNames) {
      return (ObjectName[]) clientObjectNames.toArray(new ObjectName[clientObjectNames.size()]);
    }
  }

  @Override
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

  @Override
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
        continue;
      }

      addRootMBean(name, rootID);
    }
  }

  private void setupClients() {
    MessageChannel[] channels = channelMgr.getActiveChannels();
    for (MessageChannel channel : channels) {
      addClientMBean(channel);
    }
  }

  private ObjectName makeClientObjectName(MessageChannel channel) {
    try {
      return new ObjectName(DSO_OBJECT_NAME_PREFIX + "channelID=" + channel.getChannelID().toLong() +
                            ",productId=" + channel.getProductId());
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
      // ignore literal roots
      return;
    }

    synchronized (rootObjectNames) {
      ObjectName rootName = makeRootObjectName(name, rootID);
      if (mbeanServer.isRegistered(rootName)) {
        // this can happen since the initial root setup races with the object manager "root created" event
        logger.debug("Root MBean already registered for name " + rootName);
        return;
      }

      try {
        DSORoot dsoRoot = new DSORoot(rootID, objMgr, name);
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
        DSOClient client = clientMap.remove(clientName);
        if (client != null) {
          client.stopListeningForTunneledBeans();
        }
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
        logger.error("Unable to register terracotta client MBean", e);
      }
    }
  }

  @Override
  public Map<ObjectName, Long> getAllPendingTransactionsCount() {
    Map<ObjectName, Long> map = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        map.put(clientBeanName, clientMap.get(clientBeanName).getPendingTransactionsCount());
      }
    }
    return map;
  }

  /**
   * Sum of all unacknowledged client transactions
   */
  @Override
  public long getPendingTransactionsCount() {
    long result = 0;
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result += clientMap.get(clientBeanName).getPendingTransactionsCount();
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Long> getClientTransactionRates() {
    Map<ObjectName, Long> result = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getTransactionRate());
      }
    }
    return result;
  }

  @Override
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

    @Override
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
  @Override
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
          } catch (InterruptedException e) {
            /**/
          } catch (ExecutionException e) {
            /**/
          }
        }
      }
    } catch (InterruptedException ie) {/**/
    }
    return result;
  }

  @Override
  public int getLiveObjectCount() {
    return objMgr.getLiveObjectCount();
  }

  @Override
  public long getLastCollectionGarbageCount() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getActualGarbageCount() : -1;
  }

  @Override
  public long getLastCollectionElapsedTime() {
    GCStats gcStats = gcStatsPublisher.getLastGarbageCollectorStats();
    return gcStats != null ? gcStats.getElapsedTime() : -1;
  }

  @Override
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

  @Override
  public long getGlobalServerMapGetSizeRequestsCount() {
    return getStats().getGlobalServerMapGetSizeRequestsCount();
  }

  @Override
  public long getGlobalServerMapGetSizeRequestsRate() {
    return getStats().getGlobalServerMapGetSizeRequestsRate();
  }

  @Override
  public long getGlobalServerMapGetValueRequestsCount() {
    return getStats().getGlobalServerMapGetValueRequestsCount();
  }

  @Override
  public long getGlobalServerMapGetValueRequestsRate() {
    return getStats().getGlobalServerMapGetValueRequestsRate();
  }

  @Override
  public long getWriteOperationRate() {
    return getStats().getWriteOperationRate();
  }

  @Override
  public Map<ObjectName, Long> getServerMapGetSizeRequestsCount() {
    Map<ObjectName, Long> result = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getServerMapGetSizeRequestsCount());
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Long> getServerMapGetSizeRequestsRate() {
    Map<ObjectName, Long> result = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getServerMapGetSizeRequestsRate());
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Long> getServerMapGetValueRequestsCount() {
    Map<ObjectName, Long> result = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getServerMapGetValueRequestsCount());
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Long> getServerMapGetValueRequestsRate() {
    Map<ObjectName, Long> result = new HashMap<ObjectName, Long>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        result.put(clientBeanName, clientMap.get(clientBeanName).getServerMapGetValueRequestsRate());
      }
    }
    return result;
  }

  @Override
  public boolean isResident(NodeID node, ObjectID oid) {
    return clientStateManager.hasReference(node, oid);
  }

  private class TransactionManagerListener implements ServerTransactionManagerEventListener {
    @Override
    public void rootCreated(String name, ObjectID rootID) {
      addRootMBean(name, rootID);
    }
  }

  private class DSOGCStatsEventListener implements GCStatsEventListener {

    @Override
    public void update(GCStats stats) {
      sendNotification(GC_STATUS_UPDATE, stats);
    }

  }

  private class ChannelManagerListener implements DSOChannelManagerEventListener {
    @Override
    public void channelCreated(MessageChannel channel) {
      addClientMBean(channel);
    }

    @Override
    public void channelRemoved(MessageChannel channel) {
      removeClientMBean(channel);
    }
  }

  private static class SourcedAttributeList {
    final ObjectName    objectName;
    final AttributeList attributeList;

    private SourcedAttributeList(ObjectName objectName, AttributeList attributeList) {
      this.objectName = objectName;
      this.attributeList = attributeList;
    }
  }

  private static final AttributeList EMPTY_ATTR_LIST = new AttributeList();

  private class AttributeListTask implements Callable<SourcedAttributeList> {
    private final ObjectName  objectName;
    private final Set<String> attributeSet;

    AttributeListTask(ObjectName objectName, Set<String> attributeSet) {
      this.objectName = objectName;
      this.attributeSet = attributeSet;
    }

    @Override
    public SourcedAttributeList call() {
      AttributeList attributeList;
      try {
        attributeList = mbeanServer.getAttributes(objectName, attributeSet.toArray(new String[0]));
      } catch (Exception e) {
        attributeList = EMPTY_ATTR_LIST;
      }
      return new SourcedAttributeList(objectName, attributeList);
    }
  }

  private static Exception newPlainException(Exception e) {
    String type = e.getClass().getName();
    if (type.startsWith("java.") || type.startsWith("javax.")) {
      return e;
    } else {
      RuntimeException result = new RuntimeException(e.getMessage());
      result.setStackTrace(e.getStackTrace());
      return result;
    }
  }

  @Override
  public Map<ObjectName, Exception> setAttribute(Set<ObjectName> onSet, String attrName, Object attrValue) {
    Map<ObjectName, Exception> result = new HashMap<ObjectName, Exception>();
    Iterator<ObjectName> onIter = onSet.iterator();
    Attribute attribute = new Attribute(attrName, attrValue);
    ObjectName on;
    while (onIter.hasNext()) {
      on = onIter.next();
      try {
        mbeanServer.setAttribute(on, attribute);
      } catch (Exception e) {
        result.put(on, newPlainException(e));
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Exception> setAttribute(String attrName, Map<ObjectName, Object> attrMap) {
    Map<ObjectName, Exception> result = new HashMap<ObjectName, Exception>();
    Iterator<Entry<ObjectName, Object>> entryIter = attrMap.entrySet().iterator();
    while (entryIter.hasNext()) {
      Entry<ObjectName, Object> entry = entryIter.next();
      ObjectName on = entry.getKey();
      try {
        Attribute attribute = new Attribute(attrName, entry.getValue());
        mbeanServer.setAttribute(on, attribute);
      } catch (Exception e) {
        result.put(on, newPlainException(e));
      }
    }
    return result;
  }

  @Override
  public Map<ObjectName, Map<String, Object>> getAttributeMap(Map<ObjectName, Set<String>> attributeMap, long timeout,
                                                              TimeUnit unit) {
    Map<ObjectName, Map<String, Object>> result = new HashMap<ObjectName, Map<String, Object>>();
    List<Callable<SourcedAttributeList>> tasks = new ArrayList<Callable<SourcedAttributeList>>();
    Iterator<Entry<ObjectName, Set<String>>> entryIter = attributeMap.entrySet().iterator();
    while (entryIter.hasNext()) {
      Entry<ObjectName, Set<String>> entry = entryIter.next();
      tasks.add(new AttributeListTask(entry.getKey(), entry.getValue()));
    }
    try {
      List<Future<SourcedAttributeList>> results = pool.invokeAll(tasks, timeout, unit);
      Iterator<Future<SourcedAttributeList>> resultIter = results.iterator();
      while (resultIter.hasNext()) {
        Future<SourcedAttributeList> future = resultIter.next();
        if (future.isDone() && !future.isCancelled()) {
          try {
            SourcedAttributeList sal = future.get();
            Iterator<Object> attrIter = sal.attributeList.iterator();
            Map<String, Object> onMap = new HashMap<String, Object>();
            while (attrIter.hasNext()) {
              Attribute attr = (Attribute) attrIter.next();
              onMap.put(attr.getName(), attr.getValue());
            }
            result.put(sal.objectName, onMap);
          } catch (CancellationException ce) {
            /**/
          } catch (ExecutionException ee) {
            /**/
          }
        }
      }
    } catch (InterruptedException ie) {/**/
    }
    return result;
  }

  private static class SimpleInvokeResult {
    final ObjectName objectName;
    final Object     result;

    private SimpleInvokeResult(ObjectName objectName, Object result) {
      this.objectName = objectName;
      this.result = result;
    }
  }

  private static final Object[] SIMPLE_INVOKE_PARAMS = new Object[0];
  private static final String[] SIMPLE_INVOKE_SIG    = new String[0];

  private class SimpleInvokeTask implements Callable<SimpleInvokeResult> {
    private final ObjectName objectName;
    private final String     operation;
    private final Object[]   arguments;
    private final String[]   signatures;

    SimpleInvokeTask(ObjectName objectName, String operation, Object[] arguments, String[] signatures) {
      this.objectName = objectName;
      this.operation = operation;
      this.arguments = arguments;
      this.signatures = signatures;
    }

    @Override
    public SimpleInvokeResult call() {
      Object result;
      try {
        result = mbeanServer.invoke(objectName, operation, arguments, signatures);
      } catch (Exception e) {
        result = e;
      }
      return new SimpleInvokeResult(objectName, result);
    }
  }

  @Override
  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit) {
    return invoke(onSet, operation, timeout, unit, SIMPLE_INVOKE_PARAMS, SIMPLE_INVOKE_SIG);
  }

  @Override
  public Map<ObjectName, Object> invoke(Set<ObjectName> onSet, String operation, long timeout, TimeUnit unit,
                                        Object[] args, String[] sigs) {
    Map<ObjectName, Object> result = new HashMap<ObjectName, Object>();
    List<Callable<SimpleInvokeResult>> tasks = new ArrayList<Callable<SimpleInvokeResult>>();
    Iterator<ObjectName> onIter = onSet.iterator();
    while (onIter.hasNext()) {
      tasks.add(new SimpleInvokeTask(onIter.next(), operation, args, sigs));
    }
    try {
      List<Future<SimpleInvokeResult>> results = pool.invokeAll(tasks, timeout, unit);
      Iterator<Future<SimpleInvokeResult>> resultIter = results.iterator();
      while (resultIter.hasNext()) {
        Future<SimpleInvokeResult> future = resultIter.next();
        if (future.isDone() && !future.isCancelled()) {
          try {
            SimpleInvokeResult sir = future.get();
            result.put(sir.objectName, sir.result);
          } catch (CancellationException ce) {
            /**/
          } catch (ExecutionException ee) {
            /**/
          }
        }
      }
    } catch (InterruptedException ie) {/**/
    }
    return result;
  }

  @Override
  public long getOffheapMaxSize() {
    return offheapStats.getOffheapMaxSize();
  }

  @Override
  public long getOffheapReservedSize() {
    return offheapStats.getOffheapReservedSize();
  }

  @Override
  public long getOffheapUsedSize() {
    return offheapStats.getOffheapUsedSize();
  }

  @Override
  public int getActiveLicensedClientCount() {
    return connectionPolicy.getNumberOfActiveConnections();
  }

  @Override
  public int getLicensedClientHighCount() {
    return connectionPolicy.getConnectionHighWatermark();
  }

  @Override
  public long getEvictionRate() {
    return getStats().getEvictionRate();
  }

  @Override
  public long getExpirationRate() {
    return getStats().getExpirationRate();
  }

  @Override
  public Map<String, Map<String, Long>> getStorageStats() {
    return storageStats.getStorageStats();
  }
}
