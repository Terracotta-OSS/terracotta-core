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
package com.tc.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.RemoteManagement;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.ObjectID;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.objectserver.locks.LockMBean;
import com.tc.objectserver.locks.LockManagerMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.stats.api.ClassInfo;
import com.tc.stats.api.DSOMBean;
import com.tc.stats.api.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
 * @see StatsImpl
 */
public class DSO extends AbstractNotifyingMBean implements DSOMBean {

  private final static Logger logger = LoggerFactory.getLogger(DSO.class);
  private final StatsImpl                           dsoStats;
  private final MBeanServer                            mbeanServer;
  
  private final Set<ObjectName>                        clientObjectNames      = new LinkedHashSet<>();
  
  private final Map<ObjectName, Client>             clientMap              = new HashMap<>();
  private final DSOChannelManagerMBean                 channelMgr;
  private final ChannelStats                           channelStats;
  private final ObjectInstanceMonitorMBean             instanceMonitor;
  private final TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider;
  private final ConnectionPolicy                       connectionPolicy;
  private final RemoteManagement                       remoteManagement;

  public DSO(ServerManagementContext managementContext, ServerConfigurationContext configContext,
             MBeanServer mbeanServer,
             TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider)
      throws NotCompliantMBeanException {
    super(DSOMBean.class);
    try {
      // TraceImplementation.init(TraceTags.LEVEL_TRACE);
    } catch (Exception e) {/**/
    }
    this.mbeanServer = mbeanServer;
    this.dsoStats = new StatsImpl(managementContext);
    this.channelMgr = managementContext.getChannelManager();
    this.channelStats = managementContext.getChannelStats();
    this.instanceMonitor = managementContext.getInstanceMonitor();
    this.operatorEventHistoryProvider = operatorEventHistoryProvider;
    this.connectionPolicy = managementContext.getConnectionPolicy();
    this.remoteManagement = managementContext.getRemoteManagement();

    // add various listeners (do this before the setupXXX() methods below so we don't ever miss anything)
    channelMgr.addEventListener(new ChannelManagerListener());

    setupClients();
  }

  @Override
  public void reset() {
    // TODO: implement this?
  }

  @Override
  public Stats getStats() {
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
  public ObjectName[] getClients() {
    synchronized (clientObjectNames) {
      return clientObjectNames.toArray(new ObjectName[clientObjectNames.size()]);
    }
  }

  @Override
  public ClassInfo[] getClassInfo() {
    Map<String, Integer> counts = instanceMonitor.getInstanceCounts();
    List<ClassInfo> list = new ArrayList<>();

    Iterator<Map.Entry<String, Integer>> iter = counts.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, Integer> entry = iter.next();
      list.add(new ClassInfo(entry.getKey(), entry.getValue()));
    }

    return list.toArray(new ClassInfo[list.size()]);
  }

  private void setupClients() {
    MessageChannel[] channels = channelMgr.getActiveChannels();
    for (MessageChannel channel : channels) {
      addClientMBean(channel);
    }
  }

  private ObjectName makeClientObjectName(MessageChannel channel) {
    try {
      return TerracottaManagement.createObjectName(TerracottaManagement.Type.Client, channel.getProductId().toString() + "" + channel.getChannelID().toLong(), TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException e) {
      // this shouldn't happen
      throw new RuntimeException(e);
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
        logger.error("Exception: ", e);
      } finally {
        clientObjectNames.remove(clientName);
        Client client = clientMap.remove(clientName);
      }
    }
  }

  private void addClientMBean(MessageChannel channel) {
    synchronized (clientObjectNames) {
      ObjectName clientName = makeClientObjectName(channel);
      if (mbeanServer.isRegistered(clientName)) {
        logger.debug("channel MBean already registered for name " + clientName);
        return;
      }

      try {
        final Client client = new Client(mbeanServer, channel, channelStats, channelMgr.getClientIDFor(channel
            .getChannelID()));
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
    Map<ObjectName, Long> map = new HashMap<>();
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
    Map<ObjectName, Long> result = new HashMap<>();
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
  public Map<ObjectName, Map<String, Object>> getL1Statistics() {
    Map<ObjectName, Map<String, Object>> result = new HashMap<>();
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

  private static class PrimaryClientStatWorker implements Callable<Map<String, Object>> {
    private final ObjectName clientBeanName;
    private final Client  client;

    private PrimaryClientStatWorker(ObjectName clientBeanName, Client client) {
      this.clientBeanName = clientBeanName;
      this.client = client;
    }

    @Override
    public Map<String, Object> call() {
      try {
        Map<String, Object> result = client.getStatistics();
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
  public Map<ObjectName, Map<String, Object>> getPrimaryClientStatistics() {
    Map<ObjectName, Map<String, Object>> result = new HashMap<>();
    List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
    synchronized (clientObjectNames) {
      Iterator<ObjectName> iter = clientObjectNames.iterator();
      while (iter.hasNext()) {
        ObjectName clientBeanName = iter.next();
        tasks.add(new PrimaryClientStatWorker(clientBeanName, clientMap.get(clientBeanName)));
      }
    }
    try {
      List<Future<Map<String, Object>>> results = pool.invokeAll(tasks, 2, TimeUnit.SECONDS);
      Iterator<Future<Map<String, Object>>> resultIter = results.iterator();
      while (resultIter.hasNext()) {
        Future<Map<String, Object>> future = resultIter.next();
        if (future.isDone()) {
          try {
            Map<String, Object> statsMap = future.get();
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
    return 0;
  }

  @Override
  public Map<ObjectName, Integer> getClientLiveObjectCount() {
    Map<ObjectName, Integer> result = new HashMap<>();
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

  private class ChannelManagerListener implements DSOChannelManagerEventListener {
    @Override
    public void channelCreated(MessageChannel channel) {
      addClientMBean(channel);
    }

    @Override
    public void channelRemoved(MessageChannel channel, boolean wasActive) {
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
    Map<ObjectName, Exception> result = new HashMap<>();
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
    Map<ObjectName, Exception> result = new HashMap<>();
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
    Map<ObjectName, Map<String, Object>> result = new HashMap<>();
    List<Callable<SourcedAttributeList>> tasks = new ArrayList<>();
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
            Map<String, Object> onMap = new HashMap<>();
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
    Map<ObjectName, Object> result = new HashMap<>();
    List<Callable<SimpleInvokeResult>> tasks = new ArrayList<>();
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
  public int getActiveLicensedClientCount() {
    return connectionPolicy.getNumberOfActiveConnections();
  }

  @Override
  public int getLicensedClientHighCount() {
    return connectionPolicy.getConnectionHighWatermark();
  }

  @Override
  public RemoteManagement getRemoteManagement() {
    return remoteManagement;
  }
}
