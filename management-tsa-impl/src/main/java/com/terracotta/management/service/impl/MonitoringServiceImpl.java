/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.net.ClientID;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.service.MonitoringService;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class MonitoringServiceImpl implements MonitoringService {

  public static final String[] SERVER_STATS_MBEAN_ATTRIBUTE_NAMES =
      new String[] { "BroadcastRate", "CacheHitRatio", "CachedObjectCount", "ExactOffheapObjectCachedCount",
          "GlobalLockRecallRate", "GlobalServerMapGetSizeRequestsCount", "GlobalServerMapGetSizeRequestsRate",
          "GlobalServerMapGetValueRequestsCount", "GlobalServerMapGetValueRequestsRate", "L2DiskFaultRate",
          "LastCollectionElapsedTime", "LastCollectionGarbageCount", "LiveObjectCount", "ObjectFaultRate",
          "ObjectFlushRate", "OffHeapFaultRate", "OffHeapFlushRate", "OffheapMapAllocatedMemory", "OffheapMaxDataSize",
          "OffheapObjectAllocatedMemory", "OffheapObjectCachedCount", "OffheapTotalAllocatedSize", "OnHeapFaultRate",
          "OnHeapFlushRate", "PendingTransactionsCount", "TransactionRate", "TransactionSizeRate" };

  public static final String[] CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES =
      new String[] { "ObjectFaultRate", "ObjectFlushRate", "PendingTransactionsCount", "TransactionRate",
          "ServerMapGetSizeRequestsCount", "ServerMapGetSizeRequestsRate", "ServerMapGetValueRequestsCount",
          "ServerMapGetValueRequestsRate" };

  @Override
  public Set<String> getAllClientIds() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      Set<String> clientNames = new HashSet<String>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServer.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (ObjectName clientObjectName : clientObjectNames) {
        ClientID clientID = (ClientID)mBeanServer.getAttribute(clientObjectName, "ClientID");
        clientNames.add("" + clientID.toLong());
      }

      return clientNames;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Set<String> getAllServerNames() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      Set<String> serverNames = new HashSet<String>();

      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {

        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          serverNames.add(member.name());
        }

      }

      return serverNames;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public StatisticsEntity getClientStatistics(String clientId) throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(clientId);
      statisticsEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      AttributeList attributes = mBeanServer.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId),
          CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }

      return statisticsEntity;
    } catch (InstanceNotFoundException infe) {
      return null;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public StatisticsEntity getServerStatistics(String serverName) throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(serverName);
      statisticsEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      //TODO: make use of serverName, but how to get data from a specific L2?
      AttributeList attributes = mBeanServer.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"),
          SERVER_STATS_MBEAN_ATTRIBUTE_NAMES);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }

      return statisticsEntity;
    } catch (InstanceNotFoundException infe) {
      return null;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }
}
