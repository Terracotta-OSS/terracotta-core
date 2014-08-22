package com.terracotta.management.service.impl;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.service.L1AgentIdRetrievalServiceV2;
import com.terracotta.management.service.RemoteAgentBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Anthony Dahanne
 */
public class L1AgentIdRetrievalServiceImplV2 implements L1AgentIdRetrievalServiceV2 {

  private static final Logger LOG = LoggerFactory.getLogger(L1AgentIdRetrievalServiceImplV2.class);
  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final ClientManagementServiceV2 clientManagementService;


  public L1AgentIdRetrievalServiceImplV2(RemoteAgentBridgeService remoteAgentBridgeService, ClientManagementServiceV2 clientManagementService) {
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public String getAgentIdFromRemoteAddress(String remoteAddress) throws ServiceExecutionException {
    //by default, keep the remoteAddress as the agentId
    String agentId = remoteAddress;

    remoteAddress = remoteAddress.replaceAll("_",":");

    ResponseEntityV2<ClientEntityV2> clients = clientManagementService.getClients(null, null);
    Collection<ClientEntityV2> entities = clients.getEntities();
    String clientUUID = null;
    for (ClientEntityV2 entity : entities) {
      if (entity.getAttributes().get("RemoteAddress").equals(remoteAddress)) {
        clientUUID = (String) entity.getAttributes().get("ClientUUID");
        break;
      }
    }
    if (clientUUID == null) {
      LOG.warn("Could not determine clientUUID for remoteAddress " + remoteAddress);
    } else {
      Set<String> remoteAgentNodeNames = remoteAgentBridgeService.getRemoteAgentNodeNames();
      for (String remoteAgentNodeName : remoteAgentNodeNames) {

        Map<String, String> remoteAgentNodeDetails = remoteAgentBridgeService.getRemoteAgentNodeDetails(remoteAgentNodeName);
        String clientUUIDs = remoteAgentNodeDetails.get("ClientUUIDs");
        if (clientUUIDs != null) {
          String[] split = clientUUIDs.split(",");
          for (String s : split) {
            if (s.equals(clientUUID)) {
              agentId = remoteAgentNodeName;
              break;
            }
          }
        }
      }
    }
    return agentId;
  }

}
