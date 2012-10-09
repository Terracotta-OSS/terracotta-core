/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.net.ClientID;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.service.TopologyService;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class TopologyServiceImpl implements TopologyService {

  @Override
  public TopologyEntity getTopology() throws ServiceExecutionException {
    return buildTopologyEntity();
  }


  private TopologyEntity buildTopologyEntity() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      //TODO: these attributes are taken from the local L2. They should be identical on all servers in theory but in practice it sometimes is a different story
      final String version = (String)mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "Version");
      final String buildId = (String)mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "BuildID");
      final String descriptionOfCapabilities = (String)mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "DescriptionOfCapabilities");
      final String persistenceMode = (String)mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "PersistenceMode");
      final String failoverMode = (String)mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "FailoverMode");

      ServerGroupInfo[] serverGroupInfos = (ServerGroupInfo[])mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ServerGroupInfo");

      TopologyEntity topologyEntity = new TopologyEntity();
      topologyEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
      topologyEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

      for (ServerGroupInfo serverGroupInfo : serverGroupInfos) {
        ServerGroupEntity serverGroupEntity = new ServerGroupEntity();

        serverGroupEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
        serverGroupEntity.setVersion(this.getClass().getPackage().getImplementationVersion());
        serverGroupEntity.setName(serverGroupInfo.name());
        serverGroupEntity.setId(serverGroupInfo.id());


        L2Info[] members = serverGroupInfo.members();
        for (L2Info member : members) {
          ServerEntity serverEntity = new ServerEntity();

          serverEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
          serverEntity.setVersion(this.getClass().getPackage().getImplementationVersion());

          serverEntity.getAttributes().put("Version", version);
          serverEntity.getAttributes().put("BuildID", buildId);
          serverEntity.getAttributes().put("DescriptionOfCapabilities", descriptionOfCapabilities);
          serverEntity.getAttributes().put("PersistenceMode", persistenceMode);
          serverEntity.getAttributes().put("FailoverMode", failoverMode);

          serverEntity.getAttributes().put("Name", member.name());
          serverEntity.getAttributes().put("Host", member.host());
          serverEntity.getAttributes().put("JmxPort", member.jmxPort());
          serverEntity.getAttributes().put("HostAddress", member.safeGetHostAddress());
          //TODO: add missing DSO port

          serverGroupEntity.getServers().add(serverEntity);
        }

        topologyEntity.getServerGroupEntities().add(serverGroupEntity);
      }

      return topologyEntity;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Collection<ClientEntity> getClients() throws ServiceExecutionException {
    return buildClientEntities();
  }

  private Collection<ClientEntity> buildClientEntities() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      Collection<ClientEntity> clientEntities = new HashSet<ClientEntity>();


      Set<ObjectName> dsoClientObjectNames = mBeanServer.queryNames(new ObjectName("org.terracotta:clients=Clients,name=L1 Info Bean,type=DSO Client"), null);
      ObjectName[] clientObjectNames = (ObjectName[])mBeanServer.getAttribute(new ObjectName("org.terracotta:type=Terracotta Server,name=DSO"), "Clients");

      Iterator<ObjectName> it = dsoClientObjectNames.iterator();
      for (ObjectName clientObjectName : clientObjectNames) {
        ObjectName dsoClientObjectName = it.next();

        ClientEntity clientEntity = new ClientEntity();
        clientEntity.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);

        clientEntity.getAttributes().put("RemoteAddress", mBeanServer.getAttribute(clientObjectName, "RemoteAddress"));
        ClientID clientId = (ClientID)mBeanServer.getAttribute(clientObjectName, "ClientID");
        clientEntity.getAttributes().put("ClientID", "" + clientId.toLong());

        clientEntity.getAttributes().put("Version", mBeanServer.getAttribute(dsoClientObjectName, "Version"));
        clientEntity.getAttributes().put("BuildID", mBeanServer.getAttribute(dsoClientObjectName, "BuildID"));

        clientEntities.add(clientEntity);
      }

      return clientEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

}
