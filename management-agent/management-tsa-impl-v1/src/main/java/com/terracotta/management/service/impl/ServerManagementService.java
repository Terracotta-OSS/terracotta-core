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
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.Representable;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.objectserver.api.BackupManager;
import com.tc.objectserver.api.GCStats;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.LicenseEntity;
import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.L1MBeansSourceUtils;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.ManagementSourceException;
import com.terracotta.management.service.impl.util.RemoteManagementSource;
import com.terracotta.management.web.proxy.ProxyException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;
import javax.management.ObjectName;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;

import static com.terracotta.management.service.impl.util.RemoteManagementSource.toCsv;

/**
 * @author Ludovic Orban
 */
public class ServerManagementService implements L1MBeansSource {

  private static final String[] SERVER_ENTITY_ATTRIBUTE_NAMES = new String[] { "Version", "BuildID",
      "DescriptionOfCapabilities", "PersistenceMode", "FailoverMode", "TSAListenPort", "TSAGroupPort", "State",
      "StartTime", "ActivateTime", "Restartable", "ResourceState" };

  private static final String[] SERVER_STATS_ATTRIBUTE_NAMES = new String[] { "LiveObjectCount",
      "ReadOperationRate", "WriteOperationRate", "OffheapMaxSize", "OffheapReservedSize", "OffheapUsedSize",
      "EvictionRate", "ExpirationRate", "StorageStats" };


  private final LocalManagementSource localManagementSource;
  private final ExecutorService executorService;
  private final TimeoutService timeoutService;
  private final RemoteManagementSource remoteManagementSource;
  private final SecurityContextService securityContextService;

  public ServerManagementService(ExecutorService executorService, TimeoutService timeoutService, LocalManagementSource localManagementSource, RemoteManagementSource remoteManagementSource, SecurityContextService securityContextService) {
    this.executorService = executorService;
    this.timeoutService = timeoutService;
    this.localManagementSource = localManagementSource;
    this.remoteManagementSource = remoteManagementSource;
    this.securityContextService = securityContextService;
  }

  public boolean isEnterpriseEdition() throws ServiceExecutionException {
    try {
      return localManagementSource.isEnterpriseEdition();
    } catch (ManagementSourceException e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  public Collection<String> getL2Urls() throws ServiceExecutionException {
    try {
      return localManagementSource.getServerUrls().values();
    } catch (ManagementSourceException e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  public Collection<ThreadDumpEntity> serversThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    return forEachServer("serversThreadDump", serverNames, new ForEachServer<ThreadDumpEntity>() {
      @Override
      public Collection<ThreadDumpEntity> queryLocalServer(L2Info member) {
        ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
        threadDumpEntity.setVersion(localManagementSource.getVersion());
        threadDumpEntity.setSourceId(member.name());
        threadDumpEntity.setNodeType(ThreadDumpEntity.NodeType.SERVER);
        try {
          threadDumpEntity.setDump(localManagementSource.serverThreadDump());
        } catch (ManagementSourceException mse) {
          threadDumpEntity.setDump("Unavailable");
        }
        return Collections.singleton(threadDumpEntity);
      }

      @Override
      public Collection<ThreadDumpEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("threadDump")
            .path("servers")
            .matrixParam("names", member.name());

        try {
          return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, ThreadDumpEntity.class);
        } catch (ProcessingException che) {
          ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
          threadDumpEntity.setVersion(localManagementSource.getVersion());
          threadDumpEntity.setSourceId(member.name());
          threadDumpEntity.setNodeType(ThreadDumpEntity.NodeType.SERVER);
          threadDumpEntity.setDump("Unavailable");
          return Collections.singleton(threadDumpEntity);
        }
      }
    });
  }

  public Collection<StatisticsEntity> getServersStatistics(Set<String> serverNames, final Set<String> attributesToShow) throws ServiceExecutionException {
    final String[] mbeanAttributeNames = (attributesToShow == null) ?
        SERVER_STATS_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    return forEachServer("getServersStatistics", serverNames, new ForEachServer<StatisticsEntity>() {
      @Override
      public Collection<StatisticsEntity> queryLocalServer(L2Info member) {
        StatisticsEntity statisticsEntity = new StatisticsEntity();
        statisticsEntity.setSourceId(member.name());
        statisticsEntity.setVersion(localManagementSource.getVersion());
        try {
          statisticsEntity.getStatistics().putAll(localManagementSource.getDsoAttributes(mbeanAttributeNames));
        } catch (ManagementSourceException e) {
          statisticsEntity.getStatistics().put("Error", e.getMessage());
        }
        return Collections.singleton(statisticsEntity);
      }

      @Override
      public Collection<StatisticsEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("statistics")
            .path("servers")
            .matrixParam("names", member.name());
        if (attributesToShow != null) {
          for (String attribute : attributesToShow) {
            uriBuilder.queryParam("show", attribute);
          }
        }

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, StatisticsEntity.class);
      }
    });
  }

  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames, int maxDgcStatsEntries) throws ServiceExecutionException {
    return forEachServer("getDgcStatistics", serverNames, maxDgcStatsEntries, new ForEachServer<StatisticsEntity>() {
      @Override
      public Collection<StatisticsEntity> queryLocalServer(L2Info member) {
        Collection<StatisticsEntity> localResult = new ArrayList<StatisticsEntity>();
        try {
          GCStats[] attributes = localManagementSource.getGcStats();
          for (GCStats gcStat : attributes) {
            StatisticsEntity statisticsEntity = new StatisticsEntity();
            statisticsEntity.setSourceId(member.name());
            statisticsEntity.setVersion(localManagementSource.getVersion());

            statisticsEntity.getStatistics().put("Iteration", gcStat.getIteration());
            statisticsEntity.getStatistics().put("ActualGarbageCount", gcStat.getActualGarbageCount());
            statisticsEntity.getStatistics().put("BeginObjectCount", gcStat.getBeginObjectCount());
            statisticsEntity.getStatistics().put("CandidateGarbageCount", gcStat.getCandidateGarbageCount());
            statisticsEntity.getStatistics().put("ElapsedTime", gcStat.getElapsedTime());
            statisticsEntity.getStatistics().put("EndObjectCount", gcStat.getEndObjectCount());
            statisticsEntity.getStatistics().put("MarkStageTime", gcStat.getMarkStageTime());
            statisticsEntity.getStatistics().put("PausedStageTime", gcStat.getPausedStageTime());
            statisticsEntity.getStatistics().put("StartTime", gcStat.getStartTime());
            statisticsEntity.getStatistics().put("Status", gcStat.getStatus());
            statisticsEntity.getStatistics().put("Type", gcStat.getType());

            localResult.add(statisticsEntity);
          }
        } catch (ManagementSourceException e) {
          StatisticsEntity statisticsEntity = new StatisticsEntity();
          statisticsEntity.setSourceId(member.name());
          statisticsEntity.setVersion(localManagementSource.getVersion());

          statisticsEntity.getStatistics().put("Error", e.getMessage());

          localResult.add(statisticsEntity);
        }
        return localResult;
      }

      @Override
      public Collection<StatisticsEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("statistics")
            .path("dgc")
            .matrixParam("serverNames", member.name());

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, StatisticsEntity.class);
      }
    });
  }

  public Collection<ConfigEntity> getServerConfigs(final Set<String> serverNames) throws ServiceExecutionException {
    return forEachServer("getServerConfigs", serverNames, new ForEachServer<ConfigEntity>() {
      @Override
      public Collection<ConfigEntity> queryLocalServer(L2Info member) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setVersion(localManagementSource.getVersion());
        configEntity.setSourceId(member.name());
        try {
          configEntity.getAttributes().putAll(localManagementSource.getServerInfoAttributes());
        } catch (ManagementSourceException mse) {
          configEntity.getAttributes().put("Error", mse.getMessage());
        }
        return Collections.singleton(configEntity);
      }

      @Override
      public Collection<ConfigEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("configurations")
            .path("servers")
            .matrixParam("names", member.name());

        try {
          return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, ConfigEntity.class);
        } catch (ProcessingException che) {
          ConfigEntity configEntity = new ConfigEntity();
          configEntity.setVersion(localManagementSource.getVersion());
          configEntity.setSourceId(member.name());
          configEntity.getAttributes().put("Error", che.getMessage());
          return Collections.singleton(configEntity);
        }
      }
    });
  }

  public Collection<BackupEntity> getBackupsStatus(Set<String> serverNames) throws ServiceExecutionException {
    return forEachServer("getBackupsStatus", serverNames, new ForEachServer<BackupEntity>() {
      @Override
      public Collection<BackupEntity> queryLocalServer(L2Info member) {
        if (!localManagementSource.isActiveCoordinator()) {
          return null;
        }

        Collection<BackupEntity> localResult = new ArrayList<BackupEntity>();
        Map<String, String> backups = localManagementSource.getBackupStatuses();
        for (String name : backups.keySet()) {
          String status = backups.get(name);
          BackupEntity backupEntity = new BackupEntity();
          backupEntity.setVersion(localManagementSource.getVersion());
          backupEntity.setSourceId(member.name());
          backupEntity.setName(name);
          backupEntity.setStatus(status);
          if ("FAILED".equals(status)) {
            backupEntity.setError(localManagementSource.getBackupFailureReason(name));
          }
          localResult.add(backupEntity);
        }
        return localResult;
      }

      @Override
      public Collection<BackupEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("backups")
            .matrixParam("serverNames", member.name());

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, BackupEntity.class);
      }
    });
  }

  public Collection<LicenseEntity> getLicenseProperties(Set<String> serverNames) throws ServiceExecutionException {
    return forEachServer("getLicenseProperties", serverNames, new ForEachServer<LicenseEntity>() {
      @Override
      public Collection<LicenseEntity> queryLocalServer(L2Info member) {
        LicenseEntity licenseEntity = new LicenseEntity();
        licenseEntity.setVersion(localManagementSource.getVersion());
        licenseEntity.setSourceId(member.name());
        licenseEntity.setProperties(localManagementSource.getLicenseProperties());
        return Collections.singleton(licenseEntity);
      }

      @Override
      public Collection<LicenseEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("licenseProperties")
            .matrixParam("serverNames", member.name());

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, LicenseEntity.class);
      }
    });
  }
  
  public Collection<LogEntity> getLogs(Set<String> serverNames, final Long sinceWhen) throws ServiceExecutionException {
    return forEachServer("getLogs", serverNames, new ForEachServer<LogEntity>() {
      @Override
      public Collection<LogEntity> queryLocalServer(L2Info member) {
        Collection<LogEntity> localResult = new ArrayList<LogEntity>();
        try {
          Collection<Notification> logNotifications = localManagementSource.getNotifications(sinceWhen);
          for (Notification logNotification : logNotifications) {
            LogEntity logEntity = new LogEntity();
            logEntity.setSourceId(member.name());
            logEntity.setVersion(localManagementSource.getVersion());
            logEntity.setMessage(logNotification.getMessage());
            logEntity.setTimestamp(logNotification.getTimeStamp());
            logEntity.setThrowableStringRep((String[])logNotification.getUserData());

            localResult.add(logEntity);
          }
        } catch (Exception e) {
          LogEntity logEntity = new LogEntity();
          logEntity.setSourceId(member.name());
          logEntity.setVersion(localManagementSource.getVersion());
          logEntity.setMessage(e.getMessage());

          localResult.add(logEntity);
        }
        return localResult;
      }

      @Override
      public Collection<LogEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("logs")
            .matrixParam("names", member.name());
        if (sinceWhen != null) { uriBuilder.matrixParam("sinceWhen", sinceWhen); }

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, LogEntity.class);
      }
    });
  }

  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, final Long sinceWhen, final Set<String> acceptableTypes, final Set<String> acceptableLevels, final boolean read) throws ServiceExecutionException {
    return forEachServer("getOperatorEvents", serverNames, new ForEachServer<OperatorEventEntity>() {
      @Override
      public Collection<OperatorEventEntity> queryLocalServer(L2Info member) {
        Collection<OperatorEventEntity> localResult = new ArrayList<OperatorEventEntity>();
        try {
          Collection<TerracottaOperatorEvent> operatorEvents = localManagementSource.getOperatorEvents(sinceWhen);
          for (TerracottaOperatorEvent operatorEvent : operatorEvents) {
            if (operatorEvent.isRead() && read) {
              // filter out read events
              continue;
            }
            if (acceptableTypes != null) {
              // filter out event types
              if (!acceptableTypes.contains(operatorEvent.getEventTypeAsString())) {
                continue;
              }
            }
            if (acceptableLevels != null) {
              // filter out event levels
              if (!acceptableLevels.contains(operatorEvent.getEventLevelAsString())) {
                continue;
              }
            }

            OperatorEventEntity operatorEventEntity = new OperatorEventEntity();
            operatorEventEntity.setSourceId(member.name());
            operatorEventEntity.setVersion(localManagementSource.getVersion());
            operatorEventEntity.setMessage(operatorEvent.getEventMessage());
            operatorEventEntity.setTimestamp(operatorEvent.getEventTime().getTime());
            operatorEventEntity.setCollapseString(operatorEvent.getCollapseString());
            operatorEventEntity.setEventSubsystem(operatorEvent.getEventSubsystemAsString());
            operatorEventEntity.setEventType(operatorEvent.getEventTypeAsString());
            operatorEventEntity.setEventLevel(operatorEvent.getEventLevelAsString());
            operatorEventEntity.setRead(operatorEvent.isRead());

            localResult.add(operatorEventEntity);
          }
        } catch (Exception e) {
          OperatorEventEntity operatorEventEntity = new OperatorEventEntity();
          operatorEventEntity.setSourceId(member.name());
          operatorEventEntity.setVersion(localManagementSource.getVersion());
          operatorEventEntity.setMessage(e.getMessage());

          localResult.add(operatorEventEntity);
        }
        return localResult;
      }

      @Override
      public Collection<OperatorEventEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("operatorEvents")
            .matrixParam("names", member.name());
        if (sinceWhen != null) { uriBuilder.queryParam("sinceWhen", sinceWhen); }
        if (acceptableTypes != null) { uriBuilder.queryParam("eventTypes", toCsv(acceptableTypes)); }
        if (acceptableLevels != null) { uriBuilder.queryParam("eventLevels", toCsv(acceptableLevels)); }
        uriBuilder.queryParam("filterOutRead", read);

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, OperatorEventEntity.class);
      }
    });
  }

  public Collection<MBeanEntity> queryMBeans(Set<String> serverNames, final String query) throws ServiceExecutionException {
    return forEachServer("queryMBeans", serverNames, new ForEachServer<MBeanEntity>() {
      @Override
      public Collection<MBeanEntity> queryLocalServer(L2Info member) {
        Collection<MBeanEntity> localResult = new ArrayList<MBeanEntity>();
        try {
          Set<ObjectName> objectNames = localManagementSource.queryNames(query);
          for (ObjectName objectName : objectNames) {
            List<MBeanEntity.AttributeEntity> attributeEntities = new ArrayList<MBeanEntity.AttributeEntity>();
            Map<String, String> attributes = localManagementSource.getMBeanAttributeInfo(objectName);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
              MBeanEntity.AttributeEntity attributeEntity = new MBeanEntity.AttributeEntity();
              attributeEntity.setName(entry.getKey());
              attributeEntity.setType(entry.getValue());
              attributeEntities.add(attributeEntity);
            }

            MBeanEntity mBeanEntity = new MBeanEntity();
            mBeanEntity.setSourceId(member.name());
            mBeanEntity.setVersion(localManagementSource.getVersion());
            mBeanEntity.setObjectName(objectName.toString());
            mBeanEntity.setAttributes(attributeEntities.toArray(new MBeanEntity.AttributeEntity[attributeEntities.size()]));

            localResult.add(mBeanEntity);
          }
        } catch (Exception e) {
          // ignore error when an MBean cannot be introspected
        }
        return localResult;
      }

      @Override
      public Collection<MBeanEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("jmx")
            .matrixParam("names", member.name());
        if (query != null) { uriBuilder.queryParam("q", query); }

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, MBeanEntity.class);
      }
    });
  }

  // this method duplicates the logic in forEachServer()
  public Collection<ServerGroupEntity> getServerGroups(Set<String> serverNames) throws ServiceExecutionException {
    Collection<ServerGroupEntity> localServerGroupEntities = new ArrayList<ServerGroupEntity>();
    Map<String, Future<Collection<ServerGroupEntity>>> futures = new HashMap<String, Future<Collection<ServerGroupEntity>>>();

    ServerGroupInfo[] serverGroupInfos = localManagementSource.getServerGroupInfos();
    for (final ServerGroupInfo serverGroupInfo : serverGroupInfos) {
      ServerGroupEntity serverGroupEntity = new ServerGroupEntity();

      serverGroupEntity.setVersion(localManagementSource.getVersion());
      serverGroupEntity.setName(serverGroupInfo.name());
      serverGroupEntity.setCoordinator(serverGroupInfo.isCoordinator());
      serverGroupEntity.setId(serverGroupInfo.id());

      L2Info[] members = serverGroupInfo.members();
      for (final L2Info member : members) {
        if (serverNames != null && !serverNames.contains(member.name())) { continue; }

        if (member.name().equals(localManagementSource.getLocalServerName())) {
          getServerGroups_local(localServerGroupEntities, serverGroupEntity, member);
        } else {
          getServerGroups_remote(futures, serverGroupInfo, member);
        }
      }
    }

    try {
      Map<String, ServerGroupEntity> mergedResult = new HashMap<String, ServerGroupEntity>();

      Collection<ServerGroupEntity> remoteServerGroupEntities = remoteManagementSource.collectEntitiesCollectionFromFutures(futures, timeoutService
          .getCallTimeout(), "getServerGroups", Integer.MAX_VALUE);
      for (ServerGroupEntity serverGroupEntity : remoteManagementSource.merge(localServerGroupEntities, remoteServerGroupEntities)) {
        ServerGroupEntity existingSge = mergedResult.get(serverGroupEntity.getName());
        if (existingSge == null) {
          mergedResult.put(serverGroupEntity.getName(), serverGroupEntity);
        } else {
          existingSge.getServers().addAll(serverGroupEntity.getServers());
        }
      }

      return mergedResult.values();
    } catch (Exception e) {
      throw new ServiceExecutionException("error executing remote getServerGroups", e);
    }
  }

  private void getServerGroups_remote(Map<String, Future<Collection<ServerGroupEntity>>> futures, final ServerGroupInfo serverGroupInfo, final L2Info member) {
    final SecurityContextService.SecurityContext context = securityContextService.getSecurityContext();
    Future<Collection<ServerGroupEntity>> future = executorService.submit(new Callable<Collection<ServerGroupEntity>>() {
      @Override
      public Collection<ServerGroupEntity> call() throws Exception {
        securityContextService.setSecurityContext(context);
        try {
          UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
              .path("agents")
              .path("topologies")
              .path("servers")
              .matrixParam("names", member.name());

          try {
            Collection<TopologyEntity> topologyEntities = remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, TopologyEntity.class);
            return Collections.singleton(findServerGroupEntityContainingServerWithName(topologyEntities, member.name()));
          } catch (ProcessingException che) {
            ServerGroupEntity sgEntity = new ServerGroupEntity();
            sgEntity.setVersion(localManagementSource.getVersion());
            sgEntity.setName(serverGroupInfo.name());
            sgEntity.setCoordinator(serverGroupInfo.isCoordinator());
            sgEntity.setId(serverGroupInfo.id());

            ServerEntity sEntity = new ServerEntity();
            sEntity.setVersion(localManagementSource.getVersion());
            sEntity.getAttributes().put("Name", member.name());
            sEntity.getAttributes().put("Host", member.host());
            sEntity.getAttributes().put("ManagementPort", member.managementPort());
            sEntity.getAttributes().put("HostAddress", member.safeGetHostAddress());

            sgEntity.getServers().add(sEntity);
            return Collections.singleton(sgEntity);
          }
        } finally {
          securityContextService.clearSecurityContext();
        }
      }
    });
    futures.put(member.name(), future);
  }

  private void getServerGroups_local(Collection<ServerGroupEntity> localServerGroupEntities, ServerGroupEntity serverGroupEntity, L2Info member) {
    ServerEntity serverEntity;
    serverEntity = new ServerEntity();
    serverEntity.setVersion(localManagementSource.getVersion());
    serverEntity.getAttributes().put("Name", member.name());
    serverEntity.getAttributes().put("Host", member.host());
    serverEntity.getAttributes().put("ManagementPort", member.managementPort());
    serverEntity.getAttributes().put("HostAddress", member.safeGetHostAddress());
    serverEntity.getAttributes().putAll(localManagementSource.getServerAttributes(SERVER_ENTITY_ATTRIBUTE_NAMES));

    serverGroupEntity.getServers().add(serverEntity);
    localServerGroupEntities.add(serverGroupEntity);
  }

  private static ServerGroupEntity findServerGroupEntityContainingServerWithName(Collection<TopologyEntity> topologyEntities, String name) {
    for (TopologyEntity topologyEntity : topologyEntities) {
      Set<ServerGroupEntity> serverGroupEntities = topologyEntity.getServerGroupEntities();
      for (ServerGroupEntity serverGroupEntity : serverGroupEntities) {
        Set<ServerEntity> servers = serverGroupEntity.getServers();
        for (ServerEntity server : servers) {
          if (name.equals(server.getAttributes().get("Name"))) {
            return serverGroupEntity;
          }
        }
      }
    }
    return null;
  }

  public Map<String, Integer> getUnreadOperatorEventCount(Set<String> serverNames) throws ServiceExecutionException {
    Collection<TopologyEntity> topologyEntities = forEachServer("getUnreadOperatorEventCount", serverNames, new ForEachServer<TopologyEntity>() {
      @Override
      public Collection<TopologyEntity> queryLocalServer(L2Info member) {
        TopologyEntity topologyEntity = new TopologyEntity();
        topologyEntity.setVersion(localManagementSource.getVersion());
        topologyEntity.setUnreadOperatorEventCount(localManagementSource.getUnreadOperatorEventCount());
        return Collections.singleton(topologyEntity);
      }

      @Override
      public Collection<TopologyEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("topologies")
            .path("unreadOperatorEventCount")
            .matrixParam("serverNames", member.name());

        return remoteManagementSource.getFromRemoteL2(member.name(), uriBuilder.build(), Collection.class, TopologyEntity.class);
      }
    });

    Map<String, Integer> result = new HashMap<String, Integer>();
    for (TopologyEntity topologyEntity : topologyEntities) {
      Map<String, Integer> unreadOperatorEventCount = topologyEntity.getUnreadOperatorEventCount();

      for (Map.Entry<String, Integer> entry : unreadOperatorEventCount.entrySet()) {
        String key = entry.getKey();
        Integer value = entry.getValue();

        Integer totalValue = result.get(key);
        if (totalValue == null) { totalValue = 0; }
        totalValue += value;
        result.put(key, totalValue);
      }
    }
    return result;
  }

  public void runDgc(Set<String> serverNames) throws ServiceExecutionException {
    forEachServer("runDgc", serverNames, new ForEachServer<Representable>() {
      @Override
      public Collection<Representable> queryLocalServer(L2Info member) {
        if (!localManagementSource.isActiveCoordinator()) {
          return null;
        }

        localManagementSource.runDgc();
        return null;
      }

      @Override
      public Collection<Representable> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("dgc")
            .matrixParam("serverNames", member.name());

        remoteManagementSource.postToRemoteL2(member.name(), uriBuilder.build());
        return null;
      }
    });
  }

  public void dumpClusterState(Set<String> serverNames) throws ServiceExecutionException {
    forEachServer("dumpClusterState", serverNames, new ForEachServer<Representable>() {
      @Override
      public Collection<Representable> queryLocalServer(L2Info member) {
        localManagementSource.dumpClusterState();
        return null;
      }

      @Override
      public Collection<Representable> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("dumpClusterState")
            .matrixParam("serverNames", member.name());

        remoteManagementSource.postToRemoteL2(member.name(), uriBuilder.build());
        return null;
      }
    });
  }

  public Collection<BackupEntity> backup(Set<String> serverNames, String givenBackupName) throws ServiceExecutionException {
    final String backupName = givenBackupName != null ?
        givenBackupName :
        "backup." + new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());

    return forEachServer("backup", serverNames, new ForEachServer<BackupEntity>() {
      @Override
      public Collection<BackupEntity> queryLocalServer(L2Info member) {
        if (!localManagementSource.isActiveCoordinator()) {
          return null;
        }

        BackupEntity backupEntity = new BackupEntity();
        backupEntity.setVersion(localManagementSource.getVersion());
        backupEntity.setSourceId(member.name());
        backupEntity.setName(backupName);

        try {
          localManagementSource.backup(backupName);
          backupEntity.setStatus(localManagementSource.getBackupStatus(backupName));
        } catch (Exception e) {
          backupEntity.setStatus(BackupManager.BackupStatus.FAILED.name());
          backupEntity.setError(e.getMessage());
        }

        return Collections.singleton(backupEntity);
      }

      @Override
      public Collection<BackupEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("backups")
            .matrixParam("serverNames", member.name())
            .queryParam("name", backupName);

        return remoteManagementSource.postToRemoteL2(member.name(), uriBuilder.build(), Collection.class, BackupEntity.class);
      }
    });
  }

  public void shutdownServers(Set<String> serverNames) throws ServiceExecutionException {
    final AtomicBoolean includeLocalServer = new AtomicBoolean(false);

    forEachServer("shutdownServers", serverNames, new ForEachServer<Representable>() {
      @Override
      public Collection<Representable> queryLocalServer(L2Info member) {
        includeLocalServer.set(true);
        return null;
      }

      @Override
      public Collection<Representable> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("shutdown")
            .matrixParam("names", member.name());

        remoteManagementSource.postToRemoteL2(member.name(), uriBuilder.build());
        return null;
      }
    });

    // the local server must always be the last one to shutdown
    if (includeLocalServer.get()) {
      localManagementSource.shutdownServer();
    }
  }

  public boolean markOperatorEvent(final OperatorEventEntity operatorEventEntity, final boolean read) throws ServiceExecutionException {
    String sourceId = operatorEventEntity.getSourceId();
    if (sourceId.equals(localManagementSource.getLocalServerName())) {
      TerracottaOperatorEvent terracottaOperatorEvent = new TerracottaOperatorEventImpl(
                                                                                        TerracottaOperatorEvent.EventLevel.valueOf(operatorEventEntity.getEventLevel()),
                                                                                        TerracottaOperatorEvent.EventSubsystem.valueOf(operatorEventEntity.getEventSubsystem()),
                                                                                        TerracottaOperatorEvent.EventType.valueOf(operatorEventEntity.getEventType()),
                                                                                        operatorEventEntity.getMessage(), operatorEventEntity.getTimestamp(), operatorEventEntity.getCollapseString());

      return localManagementSource.markOperatorEvent(terracottaOperatorEvent, read);
    } else {
      UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
          .path("agents")
          .path("operatorEvents");
      uriBuilder = (read) ? uriBuilder.path("read") : uriBuilder.path("unread");

      return remoteManagementSource.postToRemoteL2(sourceId, uriBuilder.build(), Collections.singleton(operatorEventEntity), Boolean.class);
    }
  }

  public Collection<TopologyReloadStatusEntity> reloadConfiguration(Set<String> serverNames) throws ServiceExecutionException {
    return forEachServer("reloadConfiguration", serverNames, new ForEachServer<TopologyReloadStatusEntity>() {
      @Override
      public Collection<TopologyReloadStatusEntity> queryLocalServer(L2Info member) {
        TopologyReloadStatusEntity topologyReloadStatusEntity = new TopologyReloadStatusEntity();
        topologyReloadStatusEntity.setSourceId(member.name());
        topologyReloadStatusEntity.setVersion(localManagementSource.getVersion());
        try {
          TopologyReloadStatus topologyReloadStatus = localManagementSource.reloadConfiguration();
          topologyReloadStatusEntity.setStatus(topologyReloadStatus.name());
        } catch (ManagementSourceException e) {
          topologyReloadStatusEntity.setStatus(e.getMessage());
        }
        return Collections.singleton(topologyReloadStatusEntity);
      }

      @Override
      public Collection<TopologyReloadStatusEntity> queryRemoteServer(L2Info member) throws Exception {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("reloadConfiguration")
            .matrixParam("serverNames", member.name());

        return remoteManagementSource.postToRemoteL2(member.name(), uriBuilder.build(), Collection.class, TopologyReloadStatusEntity.class);
      }
    });
  }

  interface ForEachServer<T> {
    Collection<T> queryLocalServer(L2Info member);
    Collection<T> queryRemoteServer(L2Info member) throws Exception;
  }

  private <T extends Representable> Collection<T> forEachServer(String methodName, Set<String> serverNames, final ForEachServer<T> fes) throws ServiceExecutionException {
    return forEachServer(methodName, serverNames, Integer.MAX_VALUE, fes);
  }

  // the logic of this method has been duplicated in getServerGroups()
  private <T extends Representable> Collection<T> forEachServer(String methodName, Set<String> serverNames, int maxEntries, final ForEachServer<T> fes) throws ServiceExecutionException {
    Collection<T> localResult = new ArrayList<T>();
    Map<String, Future<Collection<T>>> futures = new HashMap<String, Future<Collection<T>>>();

    L2Info[] members = localManagementSource.getL2Infos();
    for (final L2Info member : members) {
      if (serverNames != null && !serverNames.contains(member.name())) { continue; }

      if (member.name().equals(localManagementSource.getLocalServerName())) {
        Collection<T> c = fes.queryLocalServer(member);
        if (c != null) {
          localResult.addAll(c);
        }
      } else {
        final SecurityContextService.SecurityContext context = securityContextService.getSecurityContext();
        Future<Collection<T>> future = executorService.submit(new Callable<Collection<T>>() {
          @Override
          public Collection<T> call() throws Exception {
            securityContextService.setSecurityContext(context);
            try {
              return fes.queryRemoteServer(member);
            } finally {
              securityContextService.clearSecurityContext();
            }
          }
        });
        futures.put(member.name(), future);
      }
    }

    try {
      return remoteManagementSource.merge(localResult, remoteManagementSource.collectEntitiesCollectionFromFutures(futures, timeoutService
          .getCallTimeout(), methodName, maxEntries));
    } catch (Exception e) {
      remoteManagementSource.cancelFutures(futures.values());
      throw new ServiceExecutionException("error executing remote " + methodName, e);
    }
  }

  @Override
  public boolean containsJmxMBeans() {
    return localManagementSource.containsJmxMBeans();
  }

  @Override
  public void proxyClientRequest() throws ProxyException, ServiceExecutionException {
    L1MBeansSourceUtils.proxyClientRequest(getActiveL2UrlContainingMBeans());
  }

  private String getActiveL2UrlContainingMBeans() throws ServiceExecutionException {
    String name = getActiveL2ContainingMBeansName();
    return name == null ? null : localManagementSource.getServerUrls().get(name);
  }

  @Override
  public String getActiveL2ContainingMBeansName() throws ServiceExecutionException {
    Collection<ServerGroupEntity> serverGroups = getServerGroups(null);
    for (ServerGroupEntity serverGroup : serverGroups) {
      Set<ServerEntity> servers = serverGroup.getServers();
      for (ServerEntity server : servers) {
        String status = (String)server.getAttributes().get("State");

        // the active of coordinator group is always the one where the MBeans are tunneled to
        if ("ACTIVE-COORDINATOR".equals(status) && serverGroup.isCoordinator()) {
          return (String)server.getAttributes().get("Name");
        }
      }
    }

    return null;
  }

}
