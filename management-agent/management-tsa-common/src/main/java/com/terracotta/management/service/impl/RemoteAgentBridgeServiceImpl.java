/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;

import com.tc.config.schema.L2Info;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.JMException;
import javax.management.JMX;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentBridgeServiceImpl implements RemoteAgentBridgeService {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteAgentBridgeServiceImpl.class);

  private final JmxConnectorPool jmxConnectorPool;

  public RemoteAgentBridgeServiceImpl(JmxConnectorPool jmxConnectorPool) {
    this.jmxConnectorPool = jmxConnectorPool;
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
        LOG.debug("local server contains {} RemoteAgentEndpoint MBeans", objectNames.size());
        Set<ObjectName> remoteAgentEndpointObjectNames = mBeanServerConnection.queryNames(new ObjectName("*:*"), null);
        LOG.debug("server found {} RemoteAgentEndpoint MBeans", remoteAgentEndpointObjectNames.size());
        for (ObjectName remoteAgentEndpointObjectName : remoteAgentEndpointObjectNames) {
          LOG.debug("  {}", remoteAgentEndpointObjectName);
        }
      }
      for (ObjectName objectName : objectNames) {
        String node = objectName.getKeyProperty("node");
        LOG.debug("RemoteAgentEndpoint node name: {}", node);
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
  public Map<String, String> getRemoteAgentNodeDetails(String nodeName) throws ServiceExecutionException {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = getJMXConnectorWithL1MBeans();
      if (jmxConnector == null) {
        // there is no connected client
        return Collections.emptyMap();
      }
      final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();


      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("*:type=" + RemoteAgentEndpoint.IDENTIFIER + ",*"), null);
      if (objectNames.isEmpty()) {
        return Collections.emptyMap();
      }
      ObjectName objectName = objectNames.iterator().next();

      Map<String, String> attributes = new HashMap<String, String>();
      String version = (String)mBeanServerConnection.getAttribute(objectName, "Version");
      String agency = (String)mBeanServerConnection.getAttribute(objectName, "Agency");
      attributes.put("Version", version);
      attributes.put("Agency", agency);
      return attributes;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    } finally {
      closeConnector(jmxConnector);
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

      RemoteAgentEndpoint proxy = JMX.newMBeanProxy(mBeanServerConnection, objectName, RemoteAgentEndpoint.class);
      return proxy.invoke(remoteCallDescriptor);
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("Error making remote L1 call", e);
    } finally {
      closeConnector(jmxConnector);
    }
  }

  private static void closeConnector(JMXConnector jmxConnector) {
    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (IOException ioe) {
        LOG.warn("error closing JMX connection", ioe);
      }
    }
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
