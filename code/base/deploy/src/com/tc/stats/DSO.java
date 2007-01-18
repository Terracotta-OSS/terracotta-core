/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import org.apache.commons.collections.set.ListOrderedSet;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectInstanceMonitorMBean;
import com.tc.objectserver.api.ObjectManagerEventListener;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockManagerMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.tx.ServerTransactionManagerEventListener;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.DoubleStatistic;
import com.tc.stats.statistics.Statistic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
  private final ObjectManagerMBean            objMgr;
  private final MBeanServer                   mbeanServer;
  private final ArrayList                     rootObjectNames        = new ArrayList();
  private final Set                           clientObjectNames      = new ListOrderedSet();
  private final DSOChannelManagerMBean        channelMgr;
  private final ServerTransactionManagerMBean txnMgr;
  private final LockManagerMBean              lockMgr;
  private final ChannelStats                  channelStats;
  private final ObjectInstanceMonitorMBean    instanceMonitor;

  public DSO(final ServerManagementContext context, final MBeanServer mbeanServer) throws NotCompliantMBeanException {
    super(DSOMBean.class);
    try {
      //TraceImplementation.init(TraceTags.LEVEL_TRACE);
    } catch(Exception e) {/**/}
    this.mbeanServer = mbeanServer;
    this.dsoStats = new DSOStatsImpl(context);
    this.lockMgr = context.getLockManager();
    this.objMgr = context.getObjectManager();
    this.channelMgr = context.getChannelManager();
    this.txnMgr = context.getTransactionManager();
    this.channelStats = context.getChannelStats();
    this.instanceMonitor = context.getInstanceMonitor();

    // add various listeners (do this before the setupXXX() methods below so we don't ever miss anything)
    txnMgr.addRootListener(new TransactionManagerListener());
    objMgr.addListener(new ObjectManagerListener());
    channelMgr.addEventListener(new ChannelManagerListener());

    setupRoots();
    setupClients();
  }

  public void reset() {
    // TODO:  implement this?
  }

  public DSOStats getStats() {
    return dsoStats;
  }

  public CountStatistic getObjectFlushRate() {
    return getStats().getObjectFlushRate();
  }

  public DoubleStatistic getCacheHitRatio() {
    return getStats().getCacheHitRatio();
  }

  public CountStatistic getCacheMissRate() {
    return getStats().getCacheMissRate();
  }


  public CountStatistic getTransactionRate() {
    return getStats().getTransactionRate();
  }

  public CountStatistic getObjectFaultRate() {
    return getStats().getObjectFaultRate();
  }

  public Statistic[] getStatistics(String[] names) {
    return getStats().getStatistics(names);
  }

  public GCStats[] getGarbageCollectorStats() {
    return getStats().getGarbageCollectorStats();
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

  public DeadlockChain[] scanForDeadLocks() {
    return this.lockMgr.scanForDeadlocks();
  }

  public DSOClassInfo[] getClassInfo() {
    Map counts = instanceMonitor.getInstanceCounts();
    DSOClassInfo[] rv = new DSOClassInfo[counts.size()];

    int i = 0;
    for (Iterator iter = counts.keySet().iterator(); iter.hasNext();) {
      String type = (String) iter.next();
      int count = ((Integer) counts.get(type)).intValue();
      rv[i++] = new DSOClassInfo(type, count);
    }

    return rv;
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
        final DSOClient client = new DSOClient(channel, channelStats);
        mbeanServer.registerMBean(client, clientName);
        clientObjectNames.add(clientName);
        sendNotification(CLIENT_ATTACHED, clientName);
      } catch (Exception e) {
        logger.error("Unable to register DSO client MBean", e);
      }
    }
  }

  private class TransactionManagerListener implements ServerTransactionManagerEventListener {

    public void rootCreated(String name, ObjectID rootID) {
      addRootMBean(name, rootID);
    }

  }

  private class ObjectManagerListener implements ObjectManagerEventListener {
    public void garbageCollectionComplete(GCStats stats) {
      sendNotification(GC_COMPLETED, stats);
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
