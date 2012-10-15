/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.objectserver.api.GCStats;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.MonitoringService;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
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

  private static final String[] SERVER_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] {
      "BroadcastRate", "CacheHitRatio", "CachedObjectCount", "ExactOffheapObjectCachedCount",
      "GlobalLockRecallRate", "GlobalServerMapGetSizeRequestsCount", "GlobalServerMapGetSizeRequestsRate",
      "GlobalServerMapGetValueRequestsCount", "GlobalServerMapGetValueRequestsRate", "L2DiskFaultRate",
      "LastCollectionElapsedTime", "LastCollectionGarbageCount", "LiveObjectCount", "ObjectFaultRate",
      "ObjectFlushRate", "OffHeapFaultRate", "OffHeapFlushRate", "OffheapMapAllocatedMemory", "OffheapMaxDataSize",
      "OffheapObjectAllocatedMemory", "OffheapObjectCachedCount", "OffheapTotalAllocatedSize", "OnHeapFaultRate",
      "OnHeapFlushRate", "PendingTransactionsCount", "TransactionRate", "TransactionSizeRate" };

  private static final int MAX_DGC_STATS_ENTRIES = 1000;

  private final TsaManagementClientService tsaManagementClientService;

  public MonitoringServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Set<String> getAllClientIds() throws ServiceExecutionException {
    return tsaManagementClientService.getAllClientIds();
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
    return tsaManagementClientService.getClientStatistics(clientId);
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

  @Override
  public Collection<StatisticsEntity> getDgcStatistics() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    Collection<StatisticsEntity> statisticsEntities = new ArrayList<StatisticsEntity>();

    try {
      GCStats[] attributes = (GCStats[])mBeanServer.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "GarbageCollectorStats");

      int count = 0;
      for (GCStats gcStat : attributes) {
        StatisticsEntity statisticsEntity = new StatisticsEntity();
        statisticsEntity.setSourceId("DGC");
        statisticsEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

        statisticsEntity.getStatistics().put("Iteration", gcStat.getIteration());
        statisticsEntity.getStatistics().put("ActualGarbageCount", gcStat.getActualGarbageCount());
        statisticsEntity.getStatistics().put("BeginObjectCount", gcStat.getBeginObjectCount());
        statisticsEntity.getStatistics().put("CandidateGarbageCount", gcStat.getCandidateGarbageCount());
        statisticsEntity.getStatistics().put("DeleteStageTime", gcStat.getDeleteStageTime());
        statisticsEntity.getStatistics().put("ElapsedTime", gcStat.getElapsedTime());
        statisticsEntity.getStatistics().put("EndObjectCount", gcStat.getEndObjectCount());
        statisticsEntity.getStatistics().put("MarkStageTime", gcStat.getMarkStageTime());
        statisticsEntity.getStatistics().put("PausedStageTime", gcStat.getPausedStageTime());
        statisticsEntity.getStatistics().put("StartTime", gcStat.getStartTime());
        statisticsEntity.getStatistics().put("Status", gcStat.getStatus());
        statisticsEntity.getStatistics().put("Type", gcStat.getType());

        statisticsEntities.add(statisticsEntity);
        count++;
        if (count >= MAX_DGC_STATS_ENTRIES) {
          break;
        }
      }

      return statisticsEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }
}
