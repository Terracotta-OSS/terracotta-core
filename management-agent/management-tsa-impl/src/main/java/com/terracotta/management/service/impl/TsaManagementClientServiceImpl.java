/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;
import com.tc.management.beans.object.EnterpriseTCServerMbean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Conversion;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.ThreadDumpEntity.NodeType;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.security.KeychainInitializationException;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;
import com.terracotta.management.web.utils.TSAConfig;
import com.terracotta.management.web.utils.TSASslSocketFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipInputStream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.net.ssl.HttpsURLConnection;

/**
 * @author Ludovic Orban
 */
public class TsaManagementClientServiceImpl implements TsaManagementClientService {

  private static final Logger LOG = LoggerFactory.getLogger(TsaManagementClientServiceImpl.class);

  private static final int ZIP_BUFFER_SIZE = 2048;
  private static final String[]  SERVER_ENTITY_ATTRIBUTE_NAMES      = new String[] { "Version", "BuildID",
      "DescriptionOfCapabilities", "PersistenceMode", "FailoverMode", "TSAListenPort", "TSAGroupPort", "State",
      "StartTime", "ActivateTime", "Restartable", "ResourceState" };

  private static final String[]  CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "ObjectFaultRate",
      "ObjectFlushRate", "TransactionRate"                         };

  private static final String[]  SERVER_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "LiveObjectCount",
      "ObjectFaultRate", "ObjectFlushRate", "WriteOperationRate", "OffheapMaxSize", "OffheapReservedSize",
      "OffheapUsedSize", "EvictionRate", "ExpirationRate"          };

  private final JmxConnectorPool jmxConnectorPool;
  private final boolean secure;
  private final ExecutorService executorService;

  public TsaManagementClientServiceImpl(JmxConnectorPool jmxConnectorPool, boolean secure, ExecutorService executorService) {
    this.jmxConnectorPool = jmxConnectorPool;
    this.secure = secure;
    this.executorService = executorService;
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump(Set<String> clientIds) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      final MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsL1MBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingL1MBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptyList();
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      List<Future<ThreadDumpEntity>> futures = new ArrayList<Future<ThreadDumpEntity>>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (final ObjectName clientObjectName : clientObjectNames) {
        final String clientId = "" + mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        if (clientIds != null && !clientIds.contains(clientId)) {
          continue;
        }

        Future<ThreadDumpEntity> future = executorService.submit(new Callable<ThreadDumpEntity>() {
          @Override
          public ThreadDumpEntity call() throws Exception {
            return clientThreadDump(mBeanServerConnection, clientObjectName, clientId);
          }
        });
        futures.add(future);
      }

      Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();
      for (Future<ThreadDumpEntity> future : futures) {
        ThreadDumpEntity threadDumpEntity = future.get();
        threadDumpEntities.add(threadDumpEntity);
      }
      return threadDumpEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting client stack traces", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private ThreadDumpEntity clientThreadDump(MBeanServerConnection mBeanServerConnection, ObjectName clientObjectName, String clientId) {
    try {
      ObjectName l1InfoObjectName = (ObjectName)mBeanServerConnection.getAttribute(clientObjectName, "L1InfoBeanName");
      L1InfoMBean l1InfoMBean = JMX.newMBeanProxy(mBeanServerConnection, l1InfoObjectName, L1InfoMBean.class);

      byte[] bytes = l1InfoMBean.takeCompressedThreadDump(10000L);
      ThreadDumpEntity threadDumpEntity = unzipThreadDump(bytes);
      threadDumpEntity.setNodeType(NodeType.CLIENT);
      threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      threadDumpEntity.setSourceId(clientId);

      return threadDumpEntity;
    } catch (Exception e) {
      ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
      threadDumpEntity.setNodeType(NodeType.CLIENT);
      threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      threadDumpEntity.setSourceId(clientId);
      threadDumpEntity.setDump("Unavailable");

      return threadDumpEntity;
    }
  }

  @Override
  public Collection<ThreadDumpEntity> serversThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");
      List<Future<ThreadDumpEntity>> futures = new ArrayList<Future<ThreadDumpEntity>>();

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<ThreadDumpEntity> future = executorService.submit(new Callable<ThreadDumpEntity>() {
            @Override
            public ThreadDumpEntity call() throws Exception {
              return serverTheadDump(sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();
      for (Future<ThreadDumpEntity> future : futures) {
        ThreadDumpEntity result = future.get();
        threadDumpEntities.add(result);
      }
      return threadDumpEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting remote servers thread dump", e);
    }
  }

  private ThreadDumpEntity serverTheadDump(String sourceId, int jmxPort, String jmxHost) {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);
      ThreadDumpEntity threadDumpEntity = threadDump(tcServerInfoMBean);

      threadDumpEntity.setSourceId(sourceId);
      threadDumpEntity.setNodeType(NodeType.SERVER);
      threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      return threadDumpEntity;
    } catch (Exception e) {
      ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
      threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      threadDumpEntity.setSourceId(sourceId);
      threadDumpEntity.setNodeType(NodeType.SERVER);
      threadDumpEntity.setDump("Unavailable");

      return threadDumpEntity;
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private ServerEntity buildServerEntity(L2Info l2Info) throws ServiceExecutionException {
    return buildServerEntity(l2Info, SERVER_ENTITY_ATTRIBUTE_NAMES);
  }

  private ServerEntity buildServerEntity(L2Info l2Info, String[] attributeNames) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      ServerEntity serverEntity = new ServerEntity();

      serverEntity.getAttributes().put("Name", l2Info.name());
      serverEntity.getAttributes().put("Host", l2Info.host());
      serverEntity.getAttributes().put("JmxPort", l2Info.jmxPort());
      serverEntity.getAttributes().put("HostAddress", l2Info.safeGetHostAddress());

      jmxConnector = jmxConnectorPool.getConnector(l2Info.host(), l2Info.jmxPort());
      MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();


      AttributeList attributes = mBeanServer.getAttributes(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), attributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        serverEntity.getAttributes().put(attribute.getName(), attribute.getValue());
      }

      return serverEntity;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public Collection<ClientEntity> getClientEntities() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      final MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsL1MBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingL1MBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptyList();
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      List<Future<ClientEntity>> futures = new ArrayList<Future<ClientEntity>>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (final ObjectName clientObjectName : clientObjectNames) {
        Future<ClientEntity> future = executorService.submit(new Callable<ClientEntity>() {
          @Override
          public ClientEntity call() throws Exception {
            return getClientEntity(mBeanServerConnection, clientObjectName);
          }
        });
        futures.add(future);
      }

      Collection<ClientEntity> clientEntities = new ArrayList<ClientEntity>();
      for (Future<ClientEntity> future : futures) {
        ClientEntity clientEntity = future.get();
        if (clientEntity != null) { clientEntities.add(clientEntity); }
      }
      return clientEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private ClientEntity getClientEntity(MBeanServerConnection mBeanServerConnection, ObjectName clientObjectName) {
    ClientEntity clientEntity = new ClientEntity();
    clientEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

    try {
      ObjectName l1InfoObjectName = (ObjectName)mBeanServerConnection.getAttribute(clientObjectName, "L1InfoBeanName");

      clientEntity.getAttributes().put("RemoteAddress", mBeanServerConnection.getAttribute(clientObjectName, "RemoteAddress"));
      Long clientId = (Long)mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
      clientEntity.getAttributes().put("ClientID", "" + clientId);

      clientEntity.getAttributes().put("Version", mBeanServerConnection.getAttribute(l1InfoObjectName, "Version"));
      clientEntity.getAttributes().put("BuildID", mBeanServerConnection.getAttribute(l1InfoObjectName, "BuildID"));

      return clientEntity;
    } catch (Exception e) {
      /* client must have disconnected */
      return null;
    }
  }

  @Override
  public Collection<ServerGroupEntity> getTopology() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      Collection<ServerGroupEntity> serverGroupEntities = new ArrayList<ServerGroupEntity>();

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        ServerGroupEntity serverGroupEntity = new ServerGroupEntity();

        serverGroupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        serverGroupEntity.setName(serverGroupInfo.name());
        serverGroupEntity.setId(serverGroupInfo.id());


        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          try {
            ServerEntity serverEntity = buildServerEntity(member);
            serverEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            serverGroupEntity.getServers().add(serverEntity);
          } catch (ServiceExecutionException see) {
            // unable to contact an L2, add a server entity with minimal info
            ServerEntity serverEntity = new ServerEntity();
            serverEntity.getAttributes().put("Name", member.name());
            serverEntity.getAttributes().put("Host", member.host());
            serverEntity.getAttributes().put("JmxPort", member.jmxPort());
            serverEntity.getAttributes().put("HostAddress", member.safeGetHostAddress());
            serverEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            serverGroupEntity.getServers().add(serverEntity);
          }
        }

        serverGroupEntities.add(serverGroupEntity);
      }

      return serverGroupEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Collection<StatisticsEntity> getClientsStatistics(Set<String> clientIds, Set<String> attributesToShow) throws ServiceExecutionException {
    final String[] mbeanAttributeNames = (attributesToShow == null) ?
        CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    JMXConnector jmxConnector = null;
    try {
      final MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsL1MBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingL1MBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return null;
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(
          new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      List<Future<StatisticsEntity>> futures = new ArrayList<Future<StatisticsEntity>>();

      for (ObjectName clientObjectName : clientObjectNames) {
        final String clientId = "" + mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        if (clientIds != null && !clientIds.contains(clientId)) {
          continue;
        }

        Future<StatisticsEntity> future = executorService.submit(new Callable<StatisticsEntity>() {
          @Override
          public StatisticsEntity call() throws Exception {
            return getClientStatistics(mbeanAttributeNames, mBeanServerConnection, clientId);
          }
        });
        futures.add(future);
      }

      Collection<StatisticsEntity> statisticsEntities = new ArrayList<StatisticsEntity>();
      for (Future<StatisticsEntity> future : futures) {
        StatisticsEntity statisticsEntity = future.get();
        statisticsEntities.add(statisticsEntity);
      }
      return statisticsEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private StatisticsEntity getClientStatistics(String[] mbeanAttributeNames, MBeanServerConnection mBeanServerConnection, String clientId) {
    try {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(clientId);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      AttributeList attributes = mBeanServerConnection.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId),
          mbeanAttributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }
      return statisticsEntity;
    } catch (Exception e) {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(clientId);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      statisticsEntity.getStatistics().put("Error", e.getMessage());

      return statisticsEntity;
    }
  }

  @Override
  public Collection<StatisticsEntity> getServersStatistics(Set<String> serverNames, Set<String> attributesToShow) throws ServiceExecutionException {
    final String[] mbeanAttributeNames = (attributesToShow == null) ?
        SERVER_STATS_MBEAN_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    try {

      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<StatisticsEntity>> futures = new ArrayList<Future<StatisticsEntity>>();

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<StatisticsEntity> future = executorService.submit(new Callable<StatisticsEntity>() {
            @Override
            public StatisticsEntity call() throws Exception {
              return getServerStatistics(mbeanAttributeNames, sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<StatisticsEntity> result = new ArrayList<StatisticsEntity>();
      for (Future<StatisticsEntity> future : futures) {
        StatisticsEntity statisticsEntity = future.get();
        result.add(statisticsEntity);
      }
      return result;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  private StatisticsEntity getServerStatistics(String[] mbeanAttributeNames, String sourceId, int jmxPort, String jmxHost) throws IOException, InterruptedException, MalformedObjectNameException {JMXConnector jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
    MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

    try {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(sourceId);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      AttributeList attributes = mBeanServerConnection.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"),
          mbeanAttributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }

      return statisticsEntity;
    } catch (Exception e) {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(sourceId);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      statisticsEntity.getStatistics().put("Error", e.getMessage());
      return statisticsEntity;
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public boolean runDgc(Set<String> serverNames) throws ServiceExecutionException {
    try {
      boolean success = true;

      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          int jmxPort = member.jmxPort();
          String jmxHost = member.host();

          JMXConnector jmxConnector = null;
          try {
            jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            if (!serverIsActive(mBeanServerConnection)) {
              continue;
            }

            ObjectManagementMonitorMBean objectManagementMonitorMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta:type=Terracotta Server,subsystem=Object Management,name=ObjectManagement"), ObjectManagementMonitorMBean.class);

            objectManagementMonitorMBean.runGC();
          } catch (Exception e) {
            success = false;
          } finally {
            closeConnector(jmxConnector);
          }
        }
      }

      return success;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(Set<String> serverNames, int maxDgcStatsEntries) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Collection<StatisticsEntity>>> futures = new ArrayList<Future<Collection<StatisticsEntity>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<Collection<StatisticsEntity>> future = executorService.submit(new Callable<Collection<StatisticsEntity>>() {
            @Override
            public Collection<StatisticsEntity> call() throws Exception {
              return getSingleDgcStatistics(sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<StatisticsEntity> result = new ArrayList<StatisticsEntity>();
      for (Future<Collection<StatisticsEntity>> future : futures) {
        Collection<StatisticsEntity> statisticsEntities = future.get();
        if (result.size() < 100) {
          result.addAll(statisticsEntities);
        }
      }
      return result;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  private Collection<StatisticsEntity> getSingleDgcStatistics(String sourceId, int jmxPort, String jmxHost) {
    Collection<StatisticsEntity> result = new ArrayList<StatisticsEntity>();

    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      GCStats[] attributes = (GCStats[])mBeanServerConnection.getAttribute(
          new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "GarbageCollectorStats");

      for (GCStats gcStat : attributes) {
        StatisticsEntity statisticsEntity = new StatisticsEntity();
        statisticsEntity.setSourceId(sourceId);
        statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

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

        result.add(statisticsEntity);
      }

    } catch (Exception e) {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(sourceId);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      statisticsEntity.getStatistics().put("Error", e.getMessage());

      result.add(statisticsEntity);
    } finally {
      closeConnector(jmxConnector);
    }

    return result;
  }

  @Override
  public Collection<String> getL2Urls() throws ServiceExecutionException {
    try {
      Collection<String> urls = new ArrayList<String>();
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

      L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

      for (L2Info l2Info : l2Infos) {
        final String prefix = secure ? "https://" : "http://";
        String host = l2Info.tsaGroupBind();
        // if the TSA group port is bound to all addresses, use the host
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
          host = l2Info.host();
        }
        urls.add(prefix + host + ":" + l2Info.tsaGroupPort());
      }
      return urls;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Set<String> getL1Nodes() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsEhcacheMBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingEhcacheMBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptySet();
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      Set<String> nodes = new HashSet<String>();
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null);
      for (ObjectName objectName : objectNames) {
        String node = objectName.getKeyProperty("node");
        nodes.add(node);
      }
      return nodes;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public boolean isEnterpriseEdition() throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Enterprise Terracotta Server"), "Enabled");
      return true;
    } catch (InstanceNotFoundException infe) {
      return false;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public byte[] invokeMethod(String nodeName, Class<DfltSamplerRepositoryServiceMBean> clazz, String ticket, String token,
                              String securityCallbackUrl, String methodName, Class<?>[] paramClasses, Object[] params) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsEhcacheMBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingEhcacheMBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return null;
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      ObjectName objectName = null;
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null);
      for (ObjectName on : objectNames) {
        String node = on.getKeyProperty("node");
        if (node.equals(nodeName)) {
          objectName = on;
          break;
        }
      }
      if (objectName == null) {
        throw new ServiceExecutionException("Cannot find node : " + nodeName);
      }

      DfltSamplerRepositoryServiceMBean proxy = JMX.newMBeanProxy(mBeanServerConnection, objectName, clazz);
      return proxy.invoke(ticket, token, securityCallbackUrl, methodName, paramClasses, params);
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", ExceptionUtils.getRootCause(e));
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<ConfigEntity>> futures = new ArrayList<Future<ConfigEntity>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<ConfigEntity> future = executorService.submit(new Callable<ConfigEntity>() {
            @Override
            public ConfigEntity call() throws Exception {
              return getServerConfig(sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<ConfigEntity> configEntities = new ArrayList<ConfigEntity>();
      for (Future<ConfigEntity> future : futures) {
        ConfigEntity configEntity = future.get();
        configEntities.add(configEntity);
      }
      return configEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers config", e);
    }
  }

  private ConfigEntity getServerConfig(String sourceId, int jmxPort, String jmxHost) {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

      ConfigEntity configEntity = new ConfigEntity();
      configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      configEntity.setSourceId(sourceId);

      configEntity.getAttributes().put("tcProperties", tcServerInfoMBean.getTCProperties());
      configEntity.getAttributes().put("config", tcServerInfoMBean.getConfig());
      configEntity.getAttributes().put("environment", tcServerInfoMBean.getEnvironment());
      configEntity.getAttributes().put("processArguments", tcServerInfoMBean.getProcessArguments());

      return configEntity;
    } catch (Exception e) {
      ConfigEntity configEntity = new ConfigEntity();
      configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      configEntity.setSourceId(sourceId);

      configEntity.getAttributes().put("Error", e.getMessage());

      return configEntity;
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      final MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsL1MBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingL1MBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptyList();
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      List<Future<ConfigEntity>> futures = new ArrayList<Future<ConfigEntity>>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (final ObjectName clientObjectName : clientObjectNames) {
        final String clientId = "" + mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        if (clientIds != null && !clientIds.contains(clientId)) {
          continue;
        }

        Future<ConfigEntity> future = executorService.submit(new Callable<ConfigEntity>() {
          @Override
          public ConfigEntity call() throws Exception {
            return getClientConfig(mBeanServerConnection, clientObjectName, clientId);
          }
        });
        futures.add(future);
      }

      Collection<ConfigEntity> configEntities = new ArrayList<ConfigEntity>();
      for (Future<ConfigEntity> future : futures) {
        ConfigEntity configEntity = future.get();
        configEntities.add(configEntity);
      }
      return configEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting clients config", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private ConfigEntity getClientConfig(MBeanServerConnection mBeanServerConnection, ObjectName clientObjectName, String clientId) {
    try {
      ObjectName l1InfoObjectName = (ObjectName)mBeanServerConnection.getAttribute(clientObjectName, "L1InfoBeanName");
      L1InfoMBean l1InfoMBean = JMX.newMBeanProxy(mBeanServerConnection, l1InfoObjectName, L1InfoMBean.class);

      ConfigEntity configEntity = new ConfigEntity();
      configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      configEntity.setSourceId(clientId);

      configEntity.getAttributes().put("tcProperties", l1InfoMBean.getTCProperties());
      configEntity.getAttributes().put("config", l1InfoMBean.getConfig());
      configEntity.getAttributes().put("environment", l1InfoMBean.getEnvironment());
      configEntity.getAttributes().put("processArguments", l1InfoMBean.getProcessArguments());

      return configEntity;
    } catch (Exception e) {
      ConfigEntity configEntity = new ConfigEntity();
      configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      configEntity.setSourceId(clientId);

      configEntity.getAttributes().put("Error", e.getMessage());

      return configEntity;
    }
  }

  @Override
  public Collection<BackupEntity> getBackupsStatus() throws ServiceExecutionException {

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Collection<BackupEntity>>> futures = new ArrayList<Future<Collection<BackupEntity>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<Collection<BackupEntity>> future = executorService.submit(new Callable<Collection<BackupEntity>>() {
            @Override
            public Collection<BackupEntity> call() throws Exception {
              return getBackupStatus(sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<BackupEntity> backupEntities = new ArrayList<BackupEntity>();
      for (Future<Collection<BackupEntity>> future : futures) {
        Collection<BackupEntity> entities = future.get();
        backupEntities.addAll(entities);
      }
      return backupEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers backup status", e);
    }
  }

  private Collection<BackupEntity> getBackupStatus(String sourceId, int jmxPort, String jmxHost) throws IOException, InterruptedException, MalformedObjectNameException {
    Collection<BackupEntity> backupEntities = new ArrayList<BackupEntity>();

    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

      if ("ACTIVE-COORDINATOR".equals(tcServerInfoMBean.getState())) {
        try {
          Map<String, String> backups = tcServerInfoMBean.getBackupStatuses();
          for (String name : backups.keySet()) {
            BackupEntity backupEntity = new BackupEntity();
            backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            backupEntity.setSourceId(sourceId);
            backupEntity.setName(name);
            backupEntity.setStatus(backups.get(name));

            backupEntities.add(backupEntity);
          }
        } catch (Exception e) {
          BackupEntity backupEntity = new BackupEntity();
          backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          backupEntity.setSourceId(sourceId);
          backupEntity.setError(e.getMessage());
          backupEntities.add(backupEntity);
        }
      }
    } finally {
      closeConnector(jmxConnector);
    }

    return backupEntities;
  }

  @Override
  public Collection<BackupEntity> backup() throws ServiceExecutionException {
    Collection<BackupEntity> backupEntities = new ArrayList<BackupEntity>();

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss");
      final String backupName = "backup." + sdf.format(new Date());

      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          int jmxPort = member.jmxPort();
          String jmxHost = member.host();

          JMXConnector jmxConnector = null;
          try {
            jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

            if ("ACTIVE-COORDINATOR".equals(tcServerInfoMBean.getState())) {
              try {
                tcServerInfoMBean.backup(backupName);

                BackupEntity backupEntity = new BackupEntity();
                backupEntity.setSourceId(member.name());
                backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

                String runningBackup = tcServerInfoMBean.getRunningBackup();
                backupEntity.setName(runningBackup);

                if (runningBackup != null) {
                  String backupStatus = tcServerInfoMBean.getBackupStatus(runningBackup);
                  backupEntity.setStatus(backupStatus);
                }

                backupEntities.add(backupEntity);
              } catch (Exception e) {
                BackupEntity backupEntity = new BackupEntity();
                backupEntity.setSourceId(member.name());
                backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
                backupEntity.setError(e.getMessage());
                backupEntities.add(backupEntity);
              }
            }
          } catch (Exception e) {
            LOG.error("Connecting to server at '" + jmxHost + ":" + jmxPort + "'", e);
          } finally {
            closeConnector(jmxConnector);
          }
        }
      }

      return backupEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error performing servers backup", e);
    }
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, final Long sinceWhen) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Collection<LogEntity>>> futures = new ArrayList<Future<Collection<LogEntity>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }
          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<Collection<LogEntity>> future = executorService.submit(new Callable<Collection<LogEntity>>() {
            @Override
            public Collection<LogEntity> call() throws Exception {
              return getNodeLogs(sinceWhen, sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<LogEntity> logEntities = new ArrayList<LogEntity>();
      for (Future<Collection<LogEntity>> future : futures) {
        Collection<LogEntity> entities = future.get();
        logEntities.addAll(entities);
      }
      return logEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers logs", e);
    }
  }

  private Collection<LogEntity> getNodeLogs(Long sinceWhen, String sourceId, int jmxPort, String jmxHost) {
    Collection<LogEntity> logEntities = new ArrayList<LogEntity>();
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      TCLoggingBroadcasterMBean tcLoggingBroadcaster = JMX.newMBeanProxy(mBeanServerConnection,
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Logger"), TCLoggingBroadcasterMBean.class);

      List<Notification> logNotifications;
      if (sinceWhen == null) {
        logNotifications = tcLoggingBroadcaster.getLogNotifications();
      } else {
        logNotifications = tcLoggingBroadcaster.getLogNotificationsSince(sinceWhen);
      }

      for (Notification logNotification : logNotifications) {
        LogEntity logEntity = new LogEntity();
        logEntity.setSourceId(sourceId);
        logEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        logEntity.setMessage(logNotification.getMessage());
        logEntity.setTimestamp(logNotification.getTimeStamp());

        logEntities.add(logEntity);
      }
    } catch (Exception e) {
      LogEntity logEntity = new LogEntity();
      logEntity.setSourceId(sourceId);
      logEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      logEntity.setMessage(e.getMessage());

      logEntities.add(logEntity);
    } finally {
      closeConnector(jmxConnector);
    }
    return logEntities;
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, final Long sinceWhen, final boolean read) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Collection<OperatorEventEntity>>> futures = new ArrayList<Future<Collection<OperatorEventEntity>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (final L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<Collection<OperatorEventEntity>> future = executorService.submit(new Callable<Collection<OperatorEventEntity>>() {
            @Override
            public Collection<OperatorEventEntity> call() throws Exception {
              return getOperatorEventsByMember(sinceWhen, read, sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Collection<OperatorEventEntity> operatorEventEntities = new ArrayList<OperatorEventEntity>();
      for (Future<Collection<OperatorEventEntity>> future : futures) {
        Collection<OperatorEventEntity> entities = future.get();
        operatorEventEntities.addAll(entities);
      }
      return operatorEventEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting operator events", e);
    }
  }

  private Collection<OperatorEventEntity> getOperatorEventsByMember(Long sinceWhen, boolean read, String sourceId, int jmxPort, String jmxHost) {
    Collection<OperatorEventEntity> operatorEventEntities = new ArrayList<OperatorEventEntity>();

    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      DSOMBean dsoMBean = JMX.newMBeanProxy(mBeanServerConnection,
              new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), DSOMBean.class);

      List<TerracottaOperatorEvent> operatorEvents;
      if (sinceWhen == null) {
        operatorEvents = dsoMBean.getOperatorEvents();
      } else {
        operatorEvents = dsoMBean.getOperatorEvents(sinceWhen);
      }

      for (TerracottaOperatorEvent operatorEvent : operatorEvents) {
        if (operatorEvent.isRead() && read) {
          // filter out read events
          continue;
        }

        OperatorEventEntity operatorEventEntity = new OperatorEventEntity();
        operatorEventEntity.setSourceId(sourceId);
        operatorEventEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        operatorEventEntity.setMessage(operatorEvent.getEventMessage());
        operatorEventEntity.setTimestamp(operatorEvent.getEventTime().getTime());
        operatorEventEntity.setCollapseString(operatorEvent.getCollapseString());
        operatorEventEntity.setEventSubsystem(operatorEvent.getEventSubsystemAsString());
        operatorEventEntity.setEventType(operatorEvent.getEventTypeAsString());
        operatorEventEntity.setRead(operatorEvent.isRead());

        operatorEventEntities.add(operatorEventEntity);
      }
    } catch (Exception e) {
      OperatorEventEntity operatorEventEntity = new OperatorEventEntity();
      operatorEventEntity.setSourceId(sourceId);
      operatorEventEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
      operatorEventEntity.setMessage(e.getMessage());

      operatorEventEntities.add(operatorEventEntity);
    } finally {
      closeConnector(jmxConnector);
    }
    return operatorEventEntities;
  }

  @Override
  public boolean markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (!operatorEventEntity.getSourceId().equals(member.name())) {
            continue;
          }
          int jmxPort = member.jmxPort();
          String jmxHost = member.host();

          jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
          MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

          DSOMBean dsoMBean = JMX.newMBeanProxy(mBeanServerConnection,
              new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), DSOMBean.class);

          TerracottaOperatorEventImpl terracottaOperatorEvent = new TerracottaOperatorEventImpl(TerracottaOperatorEvent
              .EventType
              .valueOf(operatorEventEntity.getEventType()),
              TerracottaOperatorEvent.EventSubsystem.valueOf(operatorEventEntity.getEventSubsystem()),
              operatorEventEntity.getMessage(), operatorEventEntity.getTimestamp(), operatorEventEntity.getCollapseString());

          return dsoMBean.markOperatorEvent(terracottaOperatorEvent, read);
        }
      }
      throw new ServiceExecutionException("Unable to find back server to mark operator event as read");
    } catch (Exception e) {
      throw new ServiceExecutionException("error marking operator event as read", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }


  @Override
  public Map<String, Integer> getUnreadOperatorEventCount() throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Map<String, Integer>>> futures = new ArrayList<Future<Map<String, Integer>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        for (L2Info member : serverGroupInfo.members()) {
          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();


          Future<Map<String, Integer>> future = executorService.submit(new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
              return getNodeUnreadOperatorEventCount(jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      Map<String, Integer> result = new HashMap<String, Integer>();
      for (EventType severity : EventType.values()) {
        result.put(severity.name(), 0);
      }

      for (Future<Map<String, Integer>> future : futures) {
        Map<String, Integer> serverResult = future.get();
        for(String key : serverResult.keySet()) {
          Integer value = result.get(key);
          Integer serverValue = serverResult.get(key);

          value = value + serverValue;
          result.put(key, value);
        }
      }

      return result;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting unread operator event count", e);
    }
  }

  private Map<String, Integer> getNodeUnreadOperatorEventCount(int jmxPort, String jmxHost) throws IOException, MalformedObjectNameException {
    JMXConnector jmxConnector;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
    }
    catch (Exception e) {
      //that's ok to catch the exception here, it means the member of the array is not up
      // and can not give us any Operator Event
      return Collections.emptyMap();
    }
    try {
      DSOMBean dsoMBean = JMX.newMBeanProxy(jmxConnector.getMBeanServerConnection(),
          new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), DSOMBean.class);

      return dsoMBean.getUnreadOperatorEventCount();
    } catch (Exception e) {
      return Collections.emptyMap();
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public void shutdownServers(Set<String> serverNames) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      String localL2Name = (String)mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Identifier");

      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }
          if (member.name().equals(localL2Name)) {
            // don't shut down the local L2 now, it must be the last one
            continue;
          }
          int jmxPort = member.jmxPort();
          String jmxHost = member.host();

          JMXConnector jmxConnector = null;
          try {
            jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

            tcServerInfoMBean.shutdown();

          } catch (Exception e) {
            // ignore
          } finally {
            closeConnector(jmxConnector);
          }
        }
      }

      if (serverNames == null || serverNames.contains(localL2Name)) {
        TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServer,
            new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);
        tcServerInfoMBean.shutdown();
      }
    } catch (Exception e) {
      throw new ServiceExecutionException("error shutting down server", e);
    }
  }

  @Override
  public Collection<TopologyReloadStatusEntity> reloadConfiguration() throws ServiceExecutionException {
    return reloadConfiguration(true);
  }

  private Collection<TopologyReloadStatusEntity> reloadConfiguration(boolean retry) throws ServiceExecutionException {
    Collection<TopologyReloadStatusEntity> topologyReloadStatusEntities = new ArrayList<TopologyReloadStatusEntity>();

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          int jmxPort = member.jmxPort();
          String jmxHost = member.host();

          JMXConnector jmxConnector = null;
          try {
            jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            EnterpriseTCServerMbean enterpriseTCServerMbean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Enterprise Terracotta Server"), EnterpriseTCServerMbean.class);

            TopologyReloadStatus topologyReloadStatus = enterpriseTCServerMbean.reloadConfiguration();

            TopologyReloadStatusEntity topologyReloadStatusEntity = new TopologyReloadStatusEntity();
            topologyReloadStatusEntity.setSourceId(member.name());
            topologyReloadStatusEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            topologyReloadStatusEntity.setStatus(topologyReloadStatus.name());

            topologyReloadStatusEntities.add(topologyReloadStatusEntity);
          } catch (Exception e) {
            TopologyReloadStatusEntity topologyReloadStatusEntity = new TopologyReloadStatusEntity();
            topologyReloadStatusEntity.setSourceId(member.name());
            topologyReloadStatusEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            topologyReloadStatusEntity.setStatus(e.getMessage());

            topologyReloadStatusEntities.add(topologyReloadStatusEntity);
          } finally {
            closeConnector(jmxConnector);
          }
        }
      }
      return retry ? reloadConfiguration(false) : topologyReloadStatusEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error reloading configuration", e);
    }
  }

  @Override
  public List<String> performSecurityChecks() {
    List<String> errors = new ArrayList<String>();

    // no need to do anything if we're not running secured
    if (!TSAConfig.isSslEnabled()) {
      return errors;
    }

    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    L2Info[] l2Infos;
    try {
      l2Infos = (L2Info[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");
    } catch (Exception e) {
      errors.add("Error querying Platform MBean Server: " + e.getMessage());
      return errors;
    }

    // Check that we can connect to all L2s via JMX
    for (L2Info l2Info : l2Infos) {
      String jmxHost = l2Info.host();
      int jmxPort = l2Info.jmxPort();

      JMXConnector jmxConnector = null;
      try {
        jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      } catch (Exception e) {
        errors.add("Error opening JMX connection to " + jmxHost + ":" + jmxPort + " - " + ExceptionUtils.getRootCause(e).getMessage());
      } finally {
        if (jmxConnector != null) {
          closeConnector(jmxConnector);
        }
      }
    }

    // Check that we can perform IA
    String securityServiceLocation = TSAConfig.getSecurityServiceLocation();
    if (securityServiceLocation == null) {
      errors.add("No Security Service Location configured");
    } else {
      try {
        URL url = new URL(securityServiceLocation);
        HttpsURLConnection sslUrlConnection = (HttpsURLConnection) url.openConnection();

        TSASslSocketFactory tsaSslSocketFactory = new TSASslSocketFactory();
        sslUrlConnection.setSSLSocketFactory(tsaSslSocketFactory);

        Integer securityTimeout = TSAConfig.getSecurityTimeout();
        if (securityTimeout > -1) {
          sslUrlConnection.setConnectTimeout(securityTimeout);
          sslUrlConnection.setReadTimeout(securityTimeout);
        }

        InputStream inputStream;
        try {
          inputStream = sslUrlConnection.getInputStream();
          inputStream.close();
          throw new IOException("No Identity Assertion service running");
        } catch (IOException ioe) {
          // 401 is the expected response code
          if (sslUrlConnection.getResponseCode() != 401) {
            throw ioe;
          }
        }

      } catch (IOException ioe) {
        errors.add("Error opening connection to Security Service Location [" + securityServiceLocation + "]: " + ExceptionUtils.getRootCause(ioe).getMessage());
      } catch (Exception e) {
        errors.add("Error setting up SSL socket factory: " + e.getMessage());
      }

      // Check that the keychain contains this server's URL
      try {
        String managementUrl = TSAConfig.getManagementUrl();
        byte[] secret = TSAConfig.getKeyChain().retrieveSecret(new URIKeyName(managementUrl));
        if (secret == null) {
          errors.add("Missing keychain entry for Management URL [" + managementUrl + "]");
        } else {
          Arrays.fill(secret, (byte)0);
        }
      } catch (KeychainInitializationException kie) {
        errors.add("Error accessing keychain: " + kie.getMessage());
      } catch (URISyntaxException mue) {
        errors.add("Malformed Security Management URL: " + mue.getMessage());
      }

      // Check that Ehcache can perform IA
      try {
        byte[] secret = TSAConfig.getKeyChain().retrieveSecret(new URIKeyName("jmx:net.sf.ehcache:type=RepositoryService"));
        if (secret == null) {
          errors.add("Missing keychain entry for Ehcache URI [jmx:net.sf.ehcache:type=RepositoryService]");
        } else {
          Arrays.fill(secret, (byte)0);
        }
      } catch (KeychainInitializationException kie) {
        errors.add("Error accessing keychain: " + kie.getMessage());
      } catch (URISyntaxException mue) {
        errors.add("Malformed Ehcache management URI: " + mue.getMessage());
      }
    }

    return errors;
  }

  private ThreadDumpEntity threadDump(TCServerInfoMBean tcServerInfoMBean) throws IOException {
    byte[] bytes = tcServerInfoMBean.takeCompressedThreadDump(10000L);
    return unzipThreadDump(bytes);
  }

  private ThreadDumpEntity unzipThreadDump(byte[] bytes) throws IOException {
    ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
    zis.getNextEntry();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[ZIP_BUFFER_SIZE];

    while (true) {
      int read = zis.read(buffer);
      if (read == -1) break;
      baos.write(buffer, 0, read);
    }

    zis.close();
    baos.close();

    byte[] uncompressedBytes = baos.toByteArray();

    ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
    threadDumpEntity.setDump(Conversion.bytes2String(uncompressedBytes));
    return threadDumpEntity;
  }

  private JMXConnector findServerContainingEhcacheMBeans() throws JMException, InterruptedException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

    for (L2Info l2Info : l2Infos) {
      String jmxHost = l2Info.host();
      int jmxPort = l2Info.jmxPort();

      try {
        JMXConnector jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);

        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        if (serverContainsEhcacheMBeans(mBeanServerConnection)) {
          return jmxConnector;
        } else {
          jmxConnector.close();
        }
      } catch (IOException ioe) {
        // cannot connect to this L2, it might be down, just skip it
      }
    }
    return null; // no server has any client in the cluster at the moment
  }

  private boolean serverContainsEhcacheMBeans(MBeanServerConnection mBeanServerConnection) throws JMException, IOException {
    return !mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null).isEmpty();
  }

  private boolean localServerContainsEhcacheMBeans() throws MalformedObjectNameException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    return !mBeanServer.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null).isEmpty();
  }

  private JMXConnector findServerContainingL1MBeans() throws JMException, InterruptedException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

    for (L2Info l2Info : l2Infos) {
      String jmxHost = l2Info.host();
      int jmxPort = l2Info.jmxPort();

      try {
        JMXConnector jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);

        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        if (serverContainsL1MBeans(mBeanServerConnection)) {
          return jmxConnector;
        } else {
          jmxConnector.close();
        }
      } catch (IOException ioe) {
        // cannot connect to this L2, it might be down, just skip it
      }
    }
    return null; // no server has any client in the cluster at the moment
  }

  private boolean localServerContainsL1MBeans() throws JMException, IOException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    return serverContainsL1MBeans(mBeanServer);
  }

  private boolean serverContainsL1MBeans(MBeanServerConnection mBeanServerConnection) throws JMException, IOException {
    Set<ObjectName> dsoClientObjectNames = mBeanServerConnection.queryNames(
        new ObjectName("org.terracotta:clients=Clients,name=L1 Info Bean,type=DSO Client,node=*"), null);
    return !dsoClientObjectNames.isEmpty();
  }

  private boolean serverIsActive(MBeanServerConnection mBeanServerConnection) throws MalformedObjectNameException,
      InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
    Object state = mBeanServerConnection.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "State");
    return "ACTIVE-COORDINATOR".equals(state);
  }

  private void closeConnector(JMXConnector jmxConnector) {
    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (IOException ioe) {
        LOG.warn("error closing JMX connection", ioe);
      }
    }
  }

}
