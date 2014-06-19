/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.services.AgentServiceV2;

import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.web.utils.TSAConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class TsaAgentServiceImplV2 implements AgentServiceV2 {

  private static final String AGENCY = "TSA";

  private final ServerManagementServiceV2 serverManagementService;
  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final AgentServiceV2 l1Agent;

  public TsaAgentServiceImplV2(ServerManagementServiceV2 serverManagementService, RemoteAgentBridgeService remoteAgentBridgeService, AgentServiceV2 l1Agent) {
    this.serverManagementService = serverManagementService;
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.l1Agent = l1Agent;
  }

  @Override
  public ResponseEntityV2 getAgents(Set<String> ids) throws ServiceExecutionException {
    try {

      ResponseEntityV2 ResponseEntityV2 =  new ResponseEntityV2();
      
      if (ids.isEmpty()) {
        ResponseEntityV2.getEntities().add(buildAgentEntityV2());
        ResponseEntityV2.getEntities().addAll(l1Agent.getAgents(ids).getEntities());
      } else {
        Set<String> l1Nodes = null;
        Set<String> remoteIds = new HashSet<String>();
        for (String id : ids) {
          if (id.equals(AgentEntityV2.EMBEDDED_AGENT_ID)) {
            ResponseEntityV2.getEntities().add(buildAgentEntityV2());
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
          ResponseEntityV2.getEntities().addAll(l1Agent.getAgents(remoteIds).getEntities());
        }
      }

      return ResponseEntityV2;
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public ResponseEntityV2 getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    try {

      ResponseEntityV2 ResponseEntityV2 =  new ResponseEntityV2();
      
      if (ids.isEmpty()) {
        ResponseEntityV2.getEntities().add(buildAgentMetadata());
        ResponseEntityV2.getEntities().addAll(l1Agent.getAgentsMetadata(ids).getEntities());
      } else {
        Set<String> l1Nodes = null;
        Set<String> remoteIds = new HashSet<String>();
        for (String id : ids) {
          if (id.equals(AgentEntityV2.EMBEDDED_AGENT_ID)) {
            ResponseEntityV2.getEntities().add(buildAgentMetadata());
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
          ResponseEntityV2.getEntities().addAll(l1Agent.getAgentsMetadata(remoteIds).getEntities());
        }
      }

      return ResponseEntityV2;
    } catch (ServiceExecutionException see) {
      throw see;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  private AgentMetadataEntityV2 buildAgentMetadata() throws ServiceExecutionException {
    AgentMetadataEntityV2 ame = new AgentMetadataEntityV2();

    ame.setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
    ame.setAgencyOf(AGENCY);
    ame.setProductVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);

    ame.setSecured(TSAConfig.isSslEnabled());
    ame.setSslEnabled(TSAConfig.isSslEnabled());
    ame.setLicensed(serverManagementService.isEnterpriseEdition());
    ame.setNeedClientAuth(false);
    ame.setEnabled(true);

    return ame;
  }

  private AgentEntityV2 buildAgentEntityV2() throws ServiceExecutionException {
    AgentEntityV2 e = new AgentEntityV2();
    e.setAgentId(Representable.EMBEDDED_AGENT_ID);
    e.setAgencyOf(AGENCY);
    e.getRootRepresentables().put("urls", createL2Urls());
    return e;
  }

  private String createL2Urls() throws ServiceExecutionException {
    StringBuilder sb = new StringBuilder();

    Collection<String> l2Urls = serverManagementService.getL2Urls();
    for (String l2Url : l2Urls) {
      sb.append(l2Url).append(",");
    }
    if (sb.indexOf(",") > - 1) {
      sb.deleteCharAt(sb.length() - 1);
    }

    return sb.toString();
  }

}
