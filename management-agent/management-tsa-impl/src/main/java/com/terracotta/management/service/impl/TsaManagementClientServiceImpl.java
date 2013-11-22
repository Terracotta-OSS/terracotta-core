/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.VersionedEntity;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.license.ProductID;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;
import com.tc.management.beans.object.EnterpriseTCServerMbean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.net.util.TSASSLSocketFactory;
import com.tc.objectserver.api.BackupManager.BackupStatus;
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
import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.resource.OperatorEventEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.ThreadDumpEntity.NodeType;
import com.terracotta.management.resource.TopologyReloadStatusEntity;
import com.terracotta.management.security.KeychainInitializationException;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipInputStream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.Subject;

/**
 * @author Ludovic Orban
 */
public class TsaManagementClientServiceImpl implements TsaManagementClientService, RemoteAgentBridgeService {

  private static final Logger LOG = LoggerFactory.getLogger(TsaManagementClientServiceImpl.class);

  private static final int ZIP_BUFFER_SIZE = 2048;
  private static final String[]  SERVER_ENTITY_ATTRIBUTE_NAMES      = new String[] { "Version", "BuildID",
      "DescriptionOfCapabilities", "PersistenceMode", "FailoverMode", "TSAListenPort", "TSAGroupPort", "State",
      "StartTime", "ActivateTime", "Restartable", "ResourceState" };

  private static final String[]  CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "ReadRate", "WriteRate" };

  private static final String[]   SERVER_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "LiveObjectCount",
      "ReadOperationRate", "WriteOperationRate", "OffheapMaxSize", "OffheapReservedSize", "OffheapUsedSize",
      "EvictionRate", "ExpirationRate", "StorageStats"              };

  private final JmxConnectorPool jmxConnectorPool;
  private final boolean secure;
  private final ExecutorService executorService;
  private final long defaultL1BridgeTimeout;
  private final ThreadLocal<Long> l1BridgeTimeoutTl = new ThreadLocal<Long>();

  public TsaManagementClientServiceImpl(JmxConnectorPool jmxConnectorPool, boolean secure, ExecutorService executorService, long defaultL1BridgeTimeoutInMs) {
    this.jmxConnectorPool = jmxConnectorPool;
    this.secure = secure;
    this.executorService = executorService;
    this.defaultL1BridgeTimeout = defaultL1BridgeTimeoutInMs;
  }

  @Override
  public Collection<ThreadDumpEntity> clusterThreadDump(Set<ProductID> clientProductIds) throws ServiceExecutionException {
    List<Future<ThreadDumpEntity>> futures = new ArrayList<Future<ThreadDumpEntity>>();

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
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
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting remote servers thread dump", e);
    }

    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();

      if (jmxConnector != null) {
        final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        Collection<ObjectName> clientObjectNames = fetchClientObjectNames(mBeanServerConnection, clientProductIds);

        for (final ObjectName clientObjectName : clientObjectNames) {
          final String clientId = mBeanServerConnection.getAttribute(clientObjectName, "ClientID").toString();

          Future<ThreadDumpEntity> future = executorService.submit(new Callable<ThreadDumpEntity>() {
            @Override
            public ThreadDumpEntity call() throws Exception {
              return clientThreadDump(mBeanServerConnection, clientObjectName, clientId);
            }
          });
          futures.add(future);
        }
      }
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting client stack traces", e);
    } finally {
      closeConnector(jmxConnector);
    }

    try {
      return collectEntitiesFromFutures(futures);
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting cluster thread dump", e);
    }
  }

  @Override
  public Collection<ThreadDumpEntity> clientsThreadDump(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
          // there is no connected client
          return Collections.emptyList();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      List<Future<ThreadDumpEntity>> futures = new ArrayList<Future<ThreadDumpEntity>>();

      Collection<ObjectName> clientObjectNames = fetchClientObjectNames(mBeanServerConnection, clientProductIds);

      for (final ObjectName clientObjectName : clientObjectNames) {
        final String clientId = mBeanServerConnection.getAttribute(clientObjectName, "ClientID").toString();
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

      return collectEntitiesFromFutures(futures);
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

      return collectEntitiesFromFutures(futures);
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
  public Collection<ClientEntity> getClientEntities(Set<ProductID> clientProductIds) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return Collections.emptySet();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      List<Future<ClientEntity>> futures = new ArrayList<Future<ClientEntity>>();

      Collection<ObjectName> clientObjectNames = fetchClientObjectNames(mBeanServerConnection, clientProductIds);

      for (final ObjectName clientObjectName : clientObjectNames) {
        Future<ClientEntity> future = executorService.submit(new Callable<ClientEntity>() {
          @Override
          public ClientEntity call() throws Exception {
            return getClientEntity(mBeanServerConnection, clientObjectName);
          }
        });
        futures.add(future);
      }

      return collectEntitiesFromFutures(futures);
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
  public Collection<StatisticsEntity> getClientsStatistics(Set<String> clientIds, Set<String> attributesToShow, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    final String[] mbeanAttributeNames = (attributesToShow == null) ?
        CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return Collections.emptySet();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      Collection<ObjectName> clientObjectNames = fetchClientObjectNames(mBeanServerConnection, clientProductIds);

      List<Future<StatisticsEntity>> futures = new ArrayList<Future<StatisticsEntity>>();

      for (ObjectName clientObjectName : clientObjectNames) {
        final String clientId = mBeanServerConnection.getAttribute(clientObjectName, "ClientID").toString();
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

      return collectEntitiesFromFutures(futures);
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

      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId + ",productId=*"), null);
      if (objectNames.size() != 1) {
        throw new RuntimeException("there should only be 1 client at org.terracotta:type=Terracotta Server,name=DSO,channelID=" + clientId + ",productId=*");
      }
      ObjectName clientObjectName = objectNames.iterator().next();

      AttributeList attributes = mBeanServerConnection.getAttributes(clientObjectName, mbeanAttributeNames);
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

      return collectEntitiesFromFutures(futures);
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
  public boolean dumpClusterState(Set<String> serverNames) throws ServiceExecutionException {
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

            L2DumperMBean l2DumperMBean = JMX.newMBeanProxy(mBeanServerConnection,
                new ObjectName("org.terracotta.internal:type=Terracotta Server,name=L2Dumper"), L2DumperMBean.class);

            l2DumperMBean.dumpClusterState();
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
  public Set<String> getRemoteAgentNodeNames() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        LOG.debug("There is no connected L1");
        // there is no connected client
        return Collections.emptySet();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      Set<String> nodeNames = new HashSet<String>();
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("*:type=" + RemoteAgentEndpoint.IDENTIFIER + ",*"), null);
      if (LOG.isDebugEnabled()) {
        LOG.debug("local server contains {} Ehcache MBeans", objectNames.size());
        Set<ObjectName> ehcacheObjectNames = mBeanServerConnection.queryNames(new ObjectName("*:*"), null);
        LOG.debug("server found {} ehcache MBeans", ehcacheObjectNames.size());
        for (ObjectName ehcacheObjectName : ehcacheObjectNames) {
          LOG.debug("  {}", ehcacheObjectName);
        }
      }
      for (ObjectName objectName : objectNames) {
        String node = objectName.getKeyProperty("node");
        LOG.debug("Ehcache node name: {}", node);
        nodeNames.add(node);
      }
      return nodeNames;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  @Override
  public Map<String, Map<String, String>> getRemoteAgentNodeDetails() throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return Collections.emptyMap();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();


      Map<String, Map<String, String>> nodes = new HashMap<String, Map<String, String>>();
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("*:type=" + RemoteAgentEndpoint.IDENTIFIER + ",*"), null);
      for (final ObjectName objectName : objectNames) {
        try {
          Map<String, String> attributes = callWithTimeout(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
              String version = (String)mBeanServerConnection.getAttribute(objectName, "Version");
              String agency = (String)mBeanServerConnection.getAttribute(objectName, "Agency");

              Map<String, String> props = new HashMap<String, String>();
              props.put("Version", version);
              props.put("Agency", agency);
              return props;
            }
          }, objectName.toString());

          if (attributes != null) {
            nodes.put(objectName.getKeyProperty("node"), attributes);
          }
        } catch (ExecutionException ee) {
          LOG.warn("error collecting L1 agent node details of " + objectName, ee);
        }
      }
      return nodes;
    } catch (ServiceExecutionException see) {
      throw see;
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
  public byte[] invokeRemoteMethod(String nodeName, final RemoteCallDescriptor remoteCallDescriptor) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithRemoteAgentBridgeMBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return null;
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();


      ObjectName objectName = null;
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("*:type=" + RemoteAgentEndpoint.IDENTIFIER + ",*"), null);
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

      final ObjectName finalObjectName = objectName;
      return callWithTimeout(new Callable<byte[]>() {
        @Override
        public byte[] call() throws Exception {
          RemoteAgentEndpoint proxy = JMX.newMBeanProxy(mBeanServerConnection, finalObjectName, RemoteAgentEndpoint.class);
          return proxy.invoke(remoteCallDescriptor);
        }
      }, finalObjectName.toString());
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (ExecutionException ee) {
      throw new ServiceExecutionException("Error making remote L1 call", filterException(ee));
    } catch (Exception e) {
      throw new ServiceExecutionException("Error making remote L1 call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private static Throwable filterException(Throwable throwable) {
    if (throwable instanceof ExecutionException || throwable instanceof UndeclaredThrowableException) {
      return filterException(throwable.getCause());
    }
    return throwable;
  }

  @Override
  public long getCallTimeout() {
    Long timeout = l1BridgeTimeoutTl.get();
    if (timeout == null || timeout < 1) {
      timeout = defaultL1BridgeTimeout;
    }
    return timeout;
  }

  @Override
  public void setCallTimeout(long timeout) {
    l1BridgeTimeoutTl.set(timeout);
  }

  @Override
  public void clearCallTimeout() {
    l1BridgeTimeoutTl.remove();
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

      return collectEntitiesFromFutures(futures);
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
  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return Collections.emptySet();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      List<Future<ConfigEntity>> futures = new ArrayList<Future<ConfigEntity>>();

      Collection<ObjectName> clientObjectNames = fetchClientObjectNames(mBeanServerConnection, clientProductIds);

      for (final ObjectName clientObjectName : clientObjectNames) {
        final String clientId = mBeanServerConnection.getAttribute(clientObjectName, "ClientID").toString();
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

      return collectEntitiesFromFutures(futures);
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

      return collectEntitiesCollectionFromFutures(futures);
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
            String status = backups.get(name);
            BackupEntity backupEntity = new BackupEntity();
            backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
            backupEntity.setSourceId(sourceId);
            backupEntity.setName(name);
            backupEntity.setStatus(status);
            if ("FAILED".equals(status)) {
              backupEntity.setError(tcServerInfoMBean.getBackupFailureReason(name));
            }
            backupEntities.add(backupEntity);
          }
        } catch (Exception e) {
          BackupEntity backupEntity = new BackupEntity();
          backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
          backupEntity.setSourceId(sourceId);
          backupEntity.setError(e.toString());
          backupEntities.add(backupEntity);
        }
      }
    } catch (Exception e) {
      LOG.error("Connecting to server at '" + jmxHost + ":" + jmxPort + "'", e);
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
                backupEntity.setName(backupName);
                backupEntity.setSourceId(member.name());
                backupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
                backupEntity.setStatus(BackupStatus.FAILED.name());
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

      return collectEntitiesCollectionFromFutures(futures);
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
        logEntity.setThrowableStringRep((String[]) logNotification.getUserData());

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
  public Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, final Long sinceWhen, final Set<String> acceptableTypes, final boolean read) throws ServiceExecutionException {
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
              return getOperatorEventsByMember(sinceWhen, acceptableTypes, read, sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      return collectEntitiesCollectionFromFutures(futures);
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting operator events", e);
    }
  }

  private Collection<OperatorEventEntity> getOperatorEventsByMember(Long sinceWhen, Set<String> acceptableTypes, boolean read, String sourceId, int jmxPort, String jmxHost) {
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
        if (acceptableTypes != null) {
          // filter out event types
          if (!acceptableTypes.contains(operatorEvent.getEventTypeAsString())) {
            continue;
          }
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
        try {
          Map<String, Integer> serverResult = future.get();
          for (String key : serverResult.keySet()) {
            Integer value = result.get(key);
            Integer serverValue = serverResult.get(key);

            value = value + serverValue;
            result.put(key, value);
          }
        } catch (Exception e) {
          // ignore, we just don't get figures for that node
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

        TSASSLSocketFactory tsaSslSocketFactory = new TSASSLSocketFactory();
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
        byte[] secret = TSAConfig.getKeyChain().retrieveSecret(new URIKeyName("jmx:net.sf.ehcache:type=" + RemoteAgentEndpoint.IDENTIFIER));
        if (secret == null) {
          errors.add("Missing keychain entry for Ehcache URI [jmx:net.sf.ehcache:type=" + RemoteAgentEndpoint.IDENTIFIER + "]");
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

  @Override
  public Collection<MBeanEntity> queryMBeans(Set<String> serverNames, final String query) throws ServiceExecutionException {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(
          new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      List<Future<Collection<MBeanEntity>>> futures = new ArrayList<Future<Collection<MBeanEntity>>>();
      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        L2Info[] members = serverGroupInfo.members();
        for (final L2Info member : members) {
          if (serverNames != null && !serverNames.contains(member.name())) {
            continue;
          }

          final int jmxPort = member.jmxPort();
          final String jmxHost = member.host();
          final String sourceId = member.name();

          Future<Collection<MBeanEntity>> future = executorService.submit(new Callable<Collection<MBeanEntity>>() {
            @Override
            public Collection<MBeanEntity> call() throws Exception {
              return queryNodeMBeans(query, sourceId, jmxPort, jmxHost);
            }
          });
          futures.add(future);
        }
      }

      return collectEntitiesCollectionFromFutures(futures);
    } catch (Exception e) {
      throw new ServiceExecutionException("error getting operator events", e);
    }
  }

  private Collection<MBeanEntity> queryNodeMBeans(String query, String sourceId, int jmxPort, String jmxHost) {
    Collection<MBeanEntity> mbeanEntities = new ArrayList<MBeanEntity>();
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);
      MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

      if (query == null) {
        query = "*:*";
      }
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName(query), null);
      for (ObjectName objectName : objectNames) {
        List<MBeanEntity.AttributeEntity> attributeEntities = new ArrayList<MBeanEntity.AttributeEntity>();
        MBeanAttributeInfo[] attributes = mBeanServerConnection.getMBeanInfo(objectName).getAttributes();
        for (MBeanAttributeInfo attribute : attributes) {
          MBeanEntity.AttributeEntity attributeEntity = new MBeanEntity.AttributeEntity();
          attributeEntity.setType(attribute.getType());
          attributeEntity.setName(attribute.getName());
          attributeEntities.add(attributeEntity);
        }

        MBeanEntity mBeanEntity = new MBeanEntity();
        mBeanEntity.setSourceId(sourceId);
        mBeanEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        mBeanEntity.setObjectName(objectName.toString());
        mBeanEntity.setAttributes(attributeEntities.toArray(new MBeanEntity.AttributeEntity[0]));

        mbeanEntities.add(mBeanEntity);
      }
    } catch (Exception e) {
      //
    } finally {
      closeConnector(jmxConnector);
    }
    return mbeanEntities;
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


  // find the server where L1 Info MBeans are registered
  private JMXConnector getJMXConnectorWithL1MBeans() throws IOException, JMException, InterruptedException {
    ObjectName objectName = new ObjectName("org.terracotta:clients=Clients,name=L1 Info Bean,*");
    return getJmxConnectorWithMBeans(objectName);
  }

  // find the server where Agent MBeans are registered
  private JMXConnector getJMXConnectorWithRemoteAgentBridgeMBeans() throws IOException, JMException, InterruptedException {
    ObjectName objectName = new ObjectName("*:type=" + RemoteAgentEndpoint.IDENTIFIER + ",*");
    return getJmxConnectorWithMBeans(objectName);
  }

  private JMXConnector getJmxConnectorWithMBeans(final ObjectName objectName) throws JMException, IOException, InterruptedException {
    if (localServerContainsMBeans(objectName)) {
      LOG.debug("local server contains MBeans : {}", objectName);
      return new LocalJMXConnector();
    } else {
      JMXConnector jmxConnector = findServerContainingMBeans(objectName);
      if (jmxConnector == null) {
        LOG.debug("no server contains MBeans : {}", objectName);
        // there is no connected client
        return null;
      }
      LOG.debug("a remote server contains MBeans : {}", objectName);
      return jmxConnector;
    }
  }

  private JMXConnector findServerContainingMBeans(ObjectName objectName) throws JMException, InterruptedException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    L2Info[] l2Infos = (L2Info[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "L2Info");

    for (L2Info l2Info : l2Infos) {
      String jmxHost = l2Info.host();
      int jmxPort = l2Info.jmxPort();
      LOG.debug("querying server {}:{}", jmxHost, jmxPort);

      try {
        JMXConnector jmxConnector = jmxConnectorPool.getConnector(jmxHost, jmxPort);

        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        Set<ObjectName> dsoClientObjectNames = mBeanServerConnection.queryNames(objectName, null);
        if (LOG.isDebugEnabled()) {
          LOG.debug("server found {} MBeans", dsoClientObjectNames.size());
          for (ObjectName dsoClientObjectName : dsoClientObjectNames) {
            LOG.debug("  {}", dsoClientObjectName);
          }
          Set<ObjectName> terracottaObjectNames = mBeanServerConnection.queryNames(new ObjectName("org.terracotta:*"), null);
          LOG.debug("server found {} terracotta MBeans", terracottaObjectNames.size());
          for (ObjectName terracottaObjectName : terracottaObjectNames) {
            LOG.debug("  {}", terracottaObjectName);
          }
        }
        if (!dsoClientObjectNames.isEmpty()) {
          LOG.debug("server {}:{} contains MBeans", jmxHost, jmxPort);
          return jmxConnector;
        } else {
          LOG.debug("server {}:{} does NOT contain MBeans", jmxHost, jmxPort);
          jmxConnector.close();
        }
      } catch (IOException ioe) {
        LOG.debug("server {}:{} failed to answer", jmxHost, jmxPort);
        // cannot connect to this L2, it might be down, just skip it
      }
    }
    LOG.debug("no server has any of the searched objects in the cluster");
    return null; // no server has any of the searched objects in the cluster at the moment
  }

  private boolean localServerContainsMBeans(ObjectName objectName) throws JMException, IOException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectName> dsoClientObjectNames = mBeanServer.queryNames(objectName, null);
    if (LOG.isDebugEnabled()) {
      LOG.debug("local server found {} MBeans", dsoClientObjectNames.size());
      for (ObjectName dsoClientObjectName : dsoClientObjectNames) {
        LOG.debug("  {}", dsoClientObjectName);
      }
      Set<ObjectName> terracottaObjectNames = mBeanServer.queryNames(new ObjectName("org.terracotta:*"), null);
      LOG.debug("local server found {} terracotta MBeans", terracottaObjectNames.size());
      for (ObjectName terracottaObjectName : terracottaObjectNames) {
        LOG.debug("  {}", terracottaObjectName);
      }
    }
    return !dsoClientObjectNames.isEmpty();
  }

  /**
   * When expectedProductIds is null, only non-internal product ID client object names are returned.
   */
  private static Collection<ObjectName> fetchClientObjectNames(MBeanServerConnection mBeanServerConnection, Set<ProductID> expectedProductIds)
      throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException, MalformedObjectNameException {
    ObjectName[] objectNames = (ObjectName[])mBeanServerConnection.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

    List<ObjectName> result = new ArrayList<ObjectName>();
    for (ObjectName objectName : objectNames) {
      String productIdName = objectName.getKeyProperty("productId");

      if (expectedProductIds == null && !ProductID.valueOf(productIdName).isInternal()) {
        result.add(objectName);
      }
      if (expectedProductIds != null && expectedProductIds.contains(ProductID.valueOf(productIdName))) {
        result.add(objectName);
      }
    }
    return result;
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

  private <T> T callWithTimeout(Callable<T> callable, String objectName) throws ExecutionException, ServiceExecutionException {
    Future<T> future = executorService.submit(callable);
    try {
      return future.get(getCallTimeout(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ie) {
      future.cancel(true);
      throw new ServiceExecutionException("L1 call to " + objectName + " interrupted");
    } catch (TimeoutException te) {
      future.cancel(true);
      throw new ServiceExecutionException("L1 call to " + objectName + " timed out");
    }
  }

  private <T extends VersionedEntity> Collection<T> collectEntitiesFromFutures(List<Future<T>> futures) throws InterruptedException, ExecutionException {
    Collection<T> entities = new ArrayList<T>();
    for (Future<T> future : futures) {
      T entity = future.get();
      entities.add(entity);
    }
    return entities;
  }

  private <T extends VersionedEntity> Collection<T> collectEntitiesCollectionFromFutures(List<Future<Collection<T>>> futures) throws InterruptedException, ExecutionException {
    Collection<T> allEntities = new ArrayList<T>();
    for (Future<Collection<T>> future : futures) {
      Collection<T> entities = future.get();
      allEntities.addAll(entities);
    }
    return allEntities;
  }

  // A JMXConnector that returns the platform MBeanServer when getMBeanServerConnection is called
  private final static class LocalJMXConnector implements JMXConnector {
    @Override
    public void connect() throws IOException {
    }

    @Override
    public void connect(final Map<String, ?> env) throws IOException {
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
      return ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
      return null;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void addConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    }

    @Override
    public void removeConnectionNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    }

    @Override
    public void removeConnectionNotificationListener(final NotificationListener l, final NotificationFilter f, final Object handback) throws ListenerNotFoundException {
    }

    @Override
    public String getConnectionId() throws IOException {
      return null;
    }
  }

}
