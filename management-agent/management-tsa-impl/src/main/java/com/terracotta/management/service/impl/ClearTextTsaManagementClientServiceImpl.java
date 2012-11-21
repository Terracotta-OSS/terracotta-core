/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.net.ClientID;
import com.tc.objectserver.api.GCStats;
import com.tc.util.Conversion;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

/**
 * @author Ludovic Orban
 */
public class ClearTextTsaManagementClientServiceImpl implements TsaManagementClientService {

  private static final Logger LOG = LoggerFactory.getLogger(ClearTextTsaManagementClientServiceImpl.class);

  private static final int ZIP_BUFFER_SIZE = 2048;
  private static final String[] SERVER_ENTITY_ATTRIBUTE_NAMES = new String[] {
      "Version", "BuildID", "DescriptionOfCapabilities", "PersistenceMode", "FailoverMode", "DSOListenPort", "DSOGroupPort", "State"};

  private static final String[] CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] {
      "ObjectFaultRate", "ObjectFlushRate", "PendingTransactionsCount", "TransactionRate",
      "ServerMapGetSizeRequestsCount", "ServerMapGetSizeRequestsRate", "ServerMapGetValueRequestsCount",
      "ServerMapGetValueRequestsRate" };

  private static final String[] SERVER_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] {
      "BroadcastRate", "CacheHitRatio", "CachedObjectCount", "ExactOffheapObjectCachedCount",
      "GlobalLockRecallRate", "GlobalServerMapGetSizeRequestsCount", "GlobalServerMapGetSizeRequestsRate",
      "GlobalServerMapGetValueRequestsCount", "GlobalServerMapGetValueRequestsRate", "L2DiskFaultRate",
      "LastCollectionElapsedTime", "LastCollectionGarbageCount", "LiveObjectCount", "ObjectFaultRate",
      "ObjectFlushRate", "OffHeapFaultRate", "OffHeapFlushRate", "OffheapMapAllocatedMemory", "OffheapMaxDataSize",
      "OffheapObjectAllocatedMemory", "OffheapObjectCachedCount", "OffheapTotalAllocatedSize", "OnHeapFaultRate",
      "OnHeapFlushRate", "PendingTransactionsCount", "TransactionRate", "TransactionSizeRate" };

  private final JmxConnectorPool jmxConnectorPool;

  public ClearTextTsaManagementClientServiceImpl(JmxConnectorPool jmxConnectorPool) {
    this.jmxConnectorPool = jmxConnectorPool;
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

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

      Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();

      Set<ObjectName> dsoClientObjectNames = mBeanServerConnection.queryNames(
          new ObjectName("org.terracotta:clients=Clients,name=L1 Info Bean,type=DSO Client,node=*"), null);
      for (ObjectName dsoClientObjectName : dsoClientObjectNames) {
        try {
          L1InfoMBean l1InfoMBean = JMX.newMBeanProxy(mBeanServerConnection, dsoClientObjectName, L1InfoMBean.class);

          byte[] bytes = l1InfoMBean.takeCompressedThreadDump(10000L);
          ThreadDumpEntity threadDumpEntity = unzipThreadDump(bytes);
          threadDumpEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
          threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          threadDumpEntity.setSourceId(dsoClientObjectName.getKeyProperty("node"));
          threadDumpEntities.add(threadDumpEntity);
        } catch (Exception e) {
          ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
          threadDumpEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
          threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          threadDumpEntity.setSourceId(dsoClientObjectName.toString());
          threadDumpEntity.setDump("Unavailable");
          threadDumpEntities.add(threadDumpEntity);
        }
      }

      return threadDumpEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting client stack traces", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public Collection<ThreadDumpEntity> serversThreadDump() throws ServiceExecutionException {
    Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();

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
            jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + jmxHost + ":" + jmxPort);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);
            ThreadDumpEntity threadDumpEntity = threadDump(tcServerInfoMBean);

            threadDumpEntity.setSourceId(jmxHost + ":" + jmxPort);
            threadDumpEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
            threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

            threadDumpEntities.add(threadDumpEntity);
          } catch (Exception e) {
            ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
            threadDumpEntity.setDump("Unavailable");
            threadDumpEntity.setSourceId(jmxHost + ":" + jmxPort);
            threadDumpEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
            threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            threadDumpEntities.add(threadDumpEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return threadDumpEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting remote servers thread dump", e);
    }
  }

  private ServerEntity buildServerEntity(L2Info l2Info) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      ServerEntity serverEntity = new ServerEntity();

      serverEntity.getAttributes().put("Name", l2Info.name());
      serverEntity.getAttributes().put("Host", l2Info.host());
      serverEntity.getAttributes().put("JmxPort", l2Info.jmxPort());
      serverEntity.getAttributes().put("HostAddress", l2Info.safeGetHostAddress());

      jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + l2Info.host() + ":" + l2Info.jmxPort());
      MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();


      AttributeList attributes = mBeanServer.getAttributes(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), SERVER_ENTITY_ATTRIBUTE_NAMES);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        serverEntity.getAttributes().put(attribute.getName(), attribute.getValue());
      }

      return serverEntity;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public Collection<ClientEntity> getClientEntities() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

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

      Collection<ClientEntity> clientEntities = new HashSet<ClientEntity>();

      Set<ObjectName> dsoClientObjectNames = mBeanServerConnection.queryNames(
          new ObjectName("org.terracotta:clients=Clients,name=L1 Info Bean,type=DSO Client,node=*"), null);
      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      Iterator<ObjectName> it = dsoClientObjectNames.iterator();
      for (ObjectName clientObjectName : clientObjectNames) {
        ObjectName dsoClientObjectName = it.next();

        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        clientEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

        clientEntity.getAttributes().put("RemoteAddress", mBeanServerConnection.getAttribute(clientObjectName, "RemoteAddress"));
        ClientID clientId = (ClientID)mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        clientEntity.getAttributes().put("ClientID", "" + clientId.toLong());

        clientEntity.getAttributes().put("Version", mBeanServerConnection.getAttribute(dsoClientObjectName, "Version"));
        clientEntity.getAttributes().put("BuildID", mBeanServerConnection.getAttribute(dsoClientObjectName, "BuildID"));

        clientEntities.add(clientEntity);
      }

      return clientEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
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

        serverGroupEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        serverGroupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        serverGroupEntity.setName(serverGroupInfo.name());
        serverGroupEntity.setId(serverGroupInfo.id());


        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          try {
            ServerEntity serverEntity = buildServerEntity(member);
            serverEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
            serverEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            serverGroupEntity.getServers().add(serverEntity);
          } catch (ServiceExecutionException see) {
            // unable to contact an L2, add a server entity with minimal info
            ServerEntity serverEntity = new ServerEntity();
            serverEntity.getAttributes().put("Name", member.name());
            serverEntity.getAttributes().put("Host", member.host());
            serverEntity.getAttributes().put("JmxPort", member.jmxPort());
            serverEntity.getAttributes().put("HostAddress", member.safeGetHostAddress());
            serverEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
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
  public StatisticsEntity getClientStatistics(String clientId, Set<String> attributesToShow) throws ServiceExecutionException {
    String[] mbeanAttributeNames = CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES;
    if (attributesToShow != null) {
      mbeanAttributeNames = new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);
    }

    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

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

      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(clientId);
      statisticsEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      AttributeList attributes = mBeanServerConnection.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId),
          mbeanAttributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }

      return statisticsEntity;
    } catch (InstanceNotFoundException infe) {
      return null;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public StatisticsEntity getServerStatistics(String serverName, Set<String> attributesToShow) throws ServiceExecutionException {
    String[] mbeanAttributeNames = SERVER_STATS_MBEAN_ATTRIBUTE_NAMES;
    if (attributesToShow != null) {
      mbeanAttributeNames = new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);
    }

    JMXConnector jmxConnector = null;
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      StatisticsEntity statisticsEntity = new StatisticsEntity();
      statisticsEntity.setSourceId(serverName);
      statisticsEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      statisticsEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      L2Info targetServer = null;
      L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

      for (L2Info l2Info : l2Infos) {
        if (serverName.equals(l2Info.name())) {
          targetServer = l2Info;
          break;
        }
      }

      if (targetServer == null) {
        throw new ServiceExecutionException("server with name " + serverName + " not found");
      }

      jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + targetServer.host() + ":" + targetServer.jmxPort());
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      AttributeList attributes = mBeanServerConnection.getAttributes(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"),
          mbeanAttributeNames);
      for (Object attributeObj : attributes) {
        Attribute attribute = (Attribute)attributeObj;
        statisticsEntity.getStatistics().put(attribute.getName(), attribute.getValue());
      }

      return statisticsEntity;
    } catch (InstanceNotFoundException infe) {
      return null;
    } catch (IOException ioe) {
      return null;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public Set<String> getAllClientIds() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      MBeanServerConnection mBeanServerConnection;

      // find the server where L1 Info MBeans are registered
      if (localServerContainsL1MBeans()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findServerContainingL1MBeans();
        if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptySet();
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }

      Set<String> clientNames = new HashSet<String>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(
          new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (ObjectName clientObjectName : clientObjectNames) {
        ClientID clientID = (ClientID)mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        clientNames.add("" + clientID.toLong());
      }

      return clientNames;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public Set<String> getAllServerNames() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      Set<String> serverNames = new HashSet<String>();

      L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

      for (L2Info l2Info : l2Infos) {
        serverNames.add(l2Info.name());
      }

      return serverNames;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public boolean runDgc() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;

    try {
      MBeanServerConnection mBeanServerConnection;
      if (isLocalNodeActive()) {
        mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
      } else {
        jmxConnector = findActiveServer();
        if (jmxConnector == null) {
          // no active node at the moment, DGC cannot run
          return false;
        }
        mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      }
      ObjectManagementMonitorMBean objectManagementMonitorMBean = JMX.newMBeanProxy(mBeanServerConnection,
          new ObjectName("org.terracotta:type=Terracotta Server,subsystem=Object Management,name=ObjectManagement"), ObjectManagementMonitorMBean.class);

      return objectManagementMonitorMBean.runGC();
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  @Override
  public Collection<StatisticsEntity> getDgcStatistics(int maxDgcStatsEntries) throws ServiceExecutionException {

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
        if (count >= maxDgcStatsEntries) {
          break;
        }
      }

      return statisticsEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Set<String> getL2Urls() throws ServiceExecutionException {
    try {
      HashSet<String> urls = new HashSet<String>();
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

      L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

      for (L2Info l2Info : l2Infos) {
        try {
          ServerEntity serverEntity = buildServerEntity(l2Info);
          urls.add("http://" + l2Info.safeGetHostAddress() + ":" + serverEntity.getAttributes().get("DSOGroupPort"));
        } catch (ServiceExecutionException see) {
          urls.add("?");
        }
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
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
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
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOG.warn("error closing JMX connection", ioe);
        }
      }
    }
  }

  private JMXConnector findActiveServer() throws JMException, IOException, InterruptedException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(
        new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

    for (L2Info l2Info : l2Infos) {
      String jmxHost = l2Info.host();
      int jmxPort = l2Info.jmxPort();

      JMXConnector jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + jmxHost + ":" + jmxPort);

      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
      if (serverIsActive(mBeanServerConnection)) {
        return jmxConnector;
      } else {
        jmxConnector.close();
      }
    }
    return null; // no server has any client in the cluster at the moment
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
        JMXConnector jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + jmxHost + ":" + jmxPort);

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
        JMXConnector jmxConnector = jmxConnectorPool.getConnector("service:jmx:jmxmp://" + jmxHost + ":" + jmxPort);

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

  private boolean isLocalNodeActive() throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException,
      AttributeNotFoundException, MBeanException, IOException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    return serverIsActive(mBeanServer);
  }

  private boolean serverIsActive(MBeanServerConnection mBeanServerConnection) throws MalformedObjectNameException,
      InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
    Object state = mBeanServerConnection.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "State");
    return "ACTIVE-COORDINATOR".equals(state);
  }

}
