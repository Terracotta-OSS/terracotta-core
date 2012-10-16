/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ServerEntity;
import com.terracotta.management.resource.ServerGroupEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.service.TopologyService;
import com.terracotta.management.service.TsaManagementClientService;

import java.lang.management.ManagementFactory;
import java.util.Collection;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class TopologyServiceImpl implements TopologyService {

  private final TsaManagementClientService tsaManagementClientService;

  public TopologyServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public TopologyEntity getTopology() throws ServiceExecutionException {
    return buildTopologyEntity();
  }


  private TopologyEntity buildTopologyEntity() throws ServiceExecutionException {
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
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
          try {
            ServerEntity serverEntity = tsaManagementClientService.buildServerEntity(member);
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

        topologyEntity.getServerGroupEntities().add(serverGroupEntity);
      }

      return topologyEntity;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Collection<ClientEntity> getClients() throws ServiceExecutionException {
    return tsaManagementClientService.buildClientEntities();
  }

}
