/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.objectserver.api.GCStats;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventImpl;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Conversion;
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
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

/**
 * @author Ludovic Orban
 */
public class TsaManagementClientServiceImpl implements TsaManagementClientService {

  private static final Logger LOG = LoggerFactory.getLogger(TsaManagementClientServiceImpl.class);

  private static final int ZIP_BUFFER_SIZE = 2048;
  private static final String[]  SERVER_ENTITY_ATTRIBUTE_NAMES      = new String[] { "Version", "BuildID",
      "DescriptionOfCapabilities", "PersistenceMode", "FailoverMode", "DSOListenPort", "DSOGroupPort", "State",
      "StartTime", "ActivateTime", "Restartable", "RestrictedMode" };

  private static final String[]  CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "ObjectFaultRate",
      "ObjectFlushRate", "TransactionRate"                         };

  private static final String[]  SERVER_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "LiveObjectCount",
      "ObjectFaultRate", "ObjectFlushRate", "TransactionRate", "OffheapMaxSize", "OffheapReservedSize",
      "OffheapUsedSize", "EvictionRate", "ExpirationRate"          };

  private final JmxConnectorPool jmxConnectorPool;
  private final boolean secure;

  public TsaManagementClientServiceImpl(JmxConnectorPool jmxConnectorPool, boolean secure) {
    this.jmxConnectorPool = jmxConnectorPool;
    this.secure = secure;
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump(Set<String> clientIds) throws ServiceExecutionException {
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

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (ObjectName clientObjectName : clientObjectNames) {
        String clientId = "" + ((Long)mBeanServerConnection.getAttribute(clientObjectName, "ClientID")).longValue();
        if (clientIds != null && !clientIds.contains(clientId)) {
          continue;
        }

        try {
          ObjectName l1InfoObjectName = (ObjectName)mBeanServerConnection.getAttribute(clientObjectName, "L1InfoBeanName");
          L1InfoMBean l1InfoMBean = JMX.newMBeanProxy(mBeanServerConnection, l1InfoObjectName, L1InfoMBean.class);

          byte[] bytes = l1InfoMBean.takeCompressedThreadDump(10000L);
          ThreadDumpEntity threadDumpEntity = unzipThreadDump(bytes);
          threadDumpEntity.setNodeType(NodeType.CLIENT);
          threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          threadDumpEntity.setSourceId(clientId);
          threadDumpEntities.add(threadDumpEntity);
        } catch (Exception e) {
          ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
          threadDumpEntity.setNodeType(NodeType.CLIENT);
          threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          threadDumpEntity.setSourceId(clientId);
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
  public Collection<ThreadDumpEntity> serversThreadDump(Set<String> serverNames) throws ServiceExecutionException {
    Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();

    try {
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

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);
            ThreadDumpEntity threadDumpEntity = threadDump(tcServerInfoMBean);

            threadDumpEntity.setSourceId(member.name());
            threadDumpEntity.setNodeType(NodeType.SERVER);
            threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

            threadDumpEntities.add(threadDumpEntity);
          } catch (Exception e) {
            ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
            threadDumpEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            threadDumpEntity.setSourceId(member.name());
            threadDumpEntity.setNodeType(NodeType.SERVER);
            threadDumpEntity.setDump("Unavailable");
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

      jmxConnector = jmxConnectorPool.getConnector(l2Info.host(), l2Info.jmxPort());
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

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (ObjectName clientObjectName : clientObjectNames) {
        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

        try {
          ObjectName l1InfoObjectName = (ObjectName)mBeanServerConnection.getAttribute(clientObjectName, "L1InfoBeanName");

          clientEntity.getAttributes().put("RemoteAddress", mBeanServerConnection.getAttribute(clientObjectName, "RemoteAddress"));
          Long clientId = (Long)mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
          clientEntity.getAttributes().put("ClientID", "" + clientId.longValue());
  
          clientEntity.getAttributes().put("Version", mBeanServerConnection.getAttribute(l1InfoObjectName, "Version"));
          clientEntity.getAttributes().put("BuildID", mBeanServerConnection.getAttribute(l1InfoObjectName, "BuildID"));
  
          clientEntities.add(clientEntity);
        } catch (Exception e) {
          /* client must have disconnected */
        }
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

      jmxConnector = jmxConnectorPool.getConnector(targetServer.host(), targetServer.jmxPort());
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
        Long clientID = (Long)mBeanServerConnection.getAttribute(clientObjectName, "ClientID");
        clientNames.add("" + clientID.longValue());
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
          String prefix = "http://";
          if (secure) {
            prefix = "https://";
          }
          urls.add(prefix + l2Info.safeGetHostAddress() + ":" + serverEntity.getAttributes().get("DSOGroupPort"));
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

  @Override
  public Collection<ConfigEntity> getServerConfigs(Set<String> serverNames) throws ServiceExecutionException {
    Collection<ConfigEntity> configEntities = new ArrayList<ConfigEntity>();

    try {
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

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            configEntity.setSourceId(member.name());

            configEntity.getAttributes().put("tcProperties", tcServerInfoMBean.getTCProperties());
            configEntity.getAttributes().put("config", tcServerInfoMBean.getConfig());
            configEntity.getAttributes().put("environment", tcServerInfoMBean.getEnvironment());
            configEntity.getAttributes().put("processArguments", tcServerInfoMBean.getProcessArguments());

            configEntities.add(configEntity);
          } catch (Exception e) {
            ConfigEntity configEntity = new ConfigEntity();
            configEntity.setSourceId(member.name());
            configEntities.add(configEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return configEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers config", e);
    }
  }

  @Override
  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds) throws ServiceExecutionException {
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

      Collection<ConfigEntity> configEntities = new ArrayList<ConfigEntity>();

      ObjectName[] clientObjectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      for (ObjectName clientObjectName : clientObjectNames) {
        String clientId = "" + ((Long)mBeanServerConnection.getAttribute(clientObjectName, "ClientID")).longValue();
        if (clientIds != null && !clientIds.contains(clientId)) {
          continue;
        }

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

          configEntities.add(configEntity);
        } catch (Exception e) {
          ConfigEntity configEntity = new ConfigEntity();
          configEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          configEntity.setSourceId(clientId);
          configEntities.add(configEntity);
        }
      }

      return configEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting clients config", e);
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
  public Collection<BackupEntity> getBackupStatus() throws ServiceExecutionException {
    Collection<BackupEntity> backupEntities = new ArrayList<BackupEntity>();

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

            TCServerInfoMBean tcServerInfoMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), TCServerInfoMBean.class);

            BackupEntity backupEntity = new BackupEntity();
            backupEntity.setSourceId(member.name());

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
            backupEntity.setError(e.getMessage());
            backupEntities.add(backupEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return backupEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers backup status", e);
    }
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

            tcServerInfoMBean.backup(backupName);

            BackupEntity backupEntity = new BackupEntity();
            backupEntity.setSourceId(member.name());
            backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            backupEntities.add(backupEntity);
          } catch (Exception e) {
            BackupEntity backupEntity = new BackupEntity();
            backupEntity.setSourceId(member.name());
            backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            backupEntity.setError(e.getMessage());
            backupEntities.add(backupEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return backupEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error performing servers backup", e);
    }
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, Long sinceWhen) throws ServiceExecutionException {
    Collection<LogEntity> logEntities = new ArrayList<LogEntity>();

    try {
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
              logEntity.setSourceId(member.name());
              logEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
              logEntity.setMessage(logNotification.getMessage());
              logEntity.setTimestamp(logNotification.getTimeStamp());

              logEntities.add(logEntity);
            }
          } catch (Exception e) {
            LogEntity logEntity = new LogEntity();
            logEntity.setSourceId(member.name());
            logEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            logEntity.setMessage(e.getMessage());

            logEntities.add(logEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return logEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting servers logs", e);
    }
  }

  @Override
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, Long sinceWhen, boolean read) throws ServiceExecutionException {
    Collection<OperatorEventEntity> operatorEventEntities = new ArrayList<OperatorEventEntity>();

    try {
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
              operatorEventEntity.setSourceId(member.name());
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
            operatorEventEntity.setSourceId(member.name());
            operatorEventEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            operatorEventEntity.setMessage(e.getMessage());

            operatorEventEntities.add(operatorEventEntity);
          } finally {
            if (jmxConnector != null) {
              jmxConnector.close();
            }
          }
        }
      }

      return operatorEventEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting operator events", e);
    }
  }

  @Override
  public void markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException {
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

          dsoMBean.markOperatorEvent(terracottaOperatorEvent, read);

          return;
        }
      }
      throw new ServiceExecutionException("Unable to find back server to mark operator event as read");
    } catch (Exception e) {
      throw new ServiceExecutionException("error marking operator event as read", e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          throw new ServiceExecutionException("error closing jmx connector", ioe);
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

      JMXConnector jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);

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
