/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.services.AgentService;

import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.web.utils.TSAConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class TsaAgentServiceImpl implements AgentService {

  private static final String AGENCY = "TSA";

  private final TsaManagementClientService tsaManagementClientService;
  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final AgentService l1Agent;

  public TsaAgentServiceImpl(TsaManagementClientService tsaManagementClientService, RemoteAgentBridgeService remoteAgentBridgeService, AgentService l1Agent) {
    this.tsaManagementClientService = tsaManagementClientService;
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.l1Agent = l1Agent;
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> ids) throws ServiceExecutionException {
    try {
      Collection<AgentEntity> agentEntities = new ArrayList<AgentEntity>();

      if (ids.isEmpty()) {
        agentEntities.add(buildAgentEntity());
        agentEntities.addAll(l1Agent.getAgents(ids));
      } else {
        Set<String> l1Nodes = null;
        Set<String> remoteIds = new HashSet<String>();
        for (String id : ids) {
          if (id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
            agentEntities.add(buildAgentEntity());
          } else {
            if (l1Nodes == null) {
              l1Nodes = remoteAgentBridgeService.getRemoteAgentNodeNames();
            }
            if (l1Nodes.contains(id)) {
              remoteIds.add(id);
            } else {
              throw new ServiceExecutionException("Unknown agent ID : " + id);
            }
          }
        }
        if (!remoteIds.isEmpty()) {
          agentEntities.addAll(l1Agent.getAgents(remoteIds));
        }
      }

      return agentEntities;
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    try {
      Collection<AgentMetadataEntity> agentMetadataEntities = new ArrayList<AgentMetadataEntity>();

      if (ids.isEmpty()) {
        AgentMetadataEntity agentMetadataEntity = buildAgentMetadata();
        agentMetadataEntities.addAll(l1Agent.getAgentsMetadata(ids));
        agentMetadataEntities.add(agentMetadataEntity);
      } else {
        Set<String> l1Nodes = null;
        Set<String> remoteIds = new HashSet<String>();
        for (String id : ids) {
          if (id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
            agentMetadataEntities.add(buildAgentMetadata());
          } else {
            if (l1Nodes == null) {
              l1Nodes = remoteAgentBridgeService.getRemoteAgentNodeNames();
            }
            if (l1Nodes.contains(id)) {
              remoteIds.add(id);
            } else {
              throw new ServiceExecutionException("Unknown agent ID : " + id);
            }
          }
        }
        if (!remoteIds.isEmpty()) {
          agentMetadataEntities.addAll(l1Agent.getAgentsMetadata(remoteIds));
        }
      }

      return agentMetadataEntities;
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  private AgentMetadataEntity buildAgentMetadata() throws ServiceExecutionException {
    AgentMetadataEntity ame = new AgentMetadataEntity();

    ame.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    ame.setAgencyOf(AGENCY);
    ame.setVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);

    ame.setSecured(TSAConfig.isSslEnabled());
    ame.setSslEnabled(TSAConfig.isSslEnabled());
    ame.setLicensed(tsaManagementClientService.isEnterpriseEdition());
    ame.setNeedClientAuth(false);
    ame.setEnabled(true);

    return ame;
  }

  private AgentEntity buildAgentEntity() throws ServiceExecutionException {
    AgentEntity e = new AgentEntity();
    e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    e.setAgencyOf(AGENCY);
    e.setVersion(this.getClass().getPackage().getImplementationVersion());
    e.getRootRepresentables().put("urls", createL2Urls());
    return e;
  }

  private String createL2Urls() throws ServiceExecutionException {
    StringBuilder sb = new StringBuilder();

    Collection<String> l2Urls = tsaManagementClientService.getL2Urls();
    for (String l2Url : l2Urls) {
      sb.append(l2Url).append(",");
    }
    if (sb.indexOf(",") > - 1) {
      sb.deleteCharAt(sb.length() - 1);
    }

    return sb.toString();
  }

}
