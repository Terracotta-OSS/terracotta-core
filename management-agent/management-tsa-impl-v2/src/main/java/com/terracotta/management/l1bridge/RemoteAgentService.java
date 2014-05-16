/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntityCollectionV2;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.services.AgentServiceV2;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentService implements AgentServiceV2 {

  private final RemoteCaller remoteCaller;

  public RemoteAgentService(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService,
                            ExecutorService executorService, RequestTicketMonitor ticketMonitor,
                            UserService userService, TimeoutService timeoutService) {
    this.remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  @Override
  public Collection<AgentMetadataEntityV2> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    Set<String> nodes = remoteCaller.getRemoteAgentNodeNames();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> unknownIds = new HashSet<String>(ids);
    unknownIds.removeAll(nodes);
    if (!unknownIds.isEmpty()) { throw new ServiceExecutionException("Unknown agent IDs : " + unknownIds); }

    try {
      return remoteCaller.fanOutCollectionCall(null, nodes, AgentServiceV2.class.getName(), AgentServiceV2.class.getMethod("getAgentsMetadata", Set.class), new Object[] {Collections.emptySet()});
    } catch (NoSuchMethodException nsme) {
      throw new ServiceExecutionException("Error executing remote call", nsme);
    }
  }

  @Override
  public AgentEntityCollectionV2 getAgents(Set<String> idSet) throws ServiceExecutionException {
    AgentEntityCollectionV2 result = new AgentEntityCollectionV2();

    Map<String, Map<String, String>> nodes = remoteCaller.getRemoteAgentNodeDetails();
    if (idSet.isEmpty()) {
      idSet = nodes.keySet();
    }

    for (String id : idSet) {
      if (!nodes.keySet().contains(id)) { throw new ServiceExecutionException("Unknown agent ID : " + id); }
      Map<String, String> props = nodes.get(id);

      AgentEntityV2 e = new AgentEntityV2();
      e.setAgentId(id);
      e.setAgencyOf(props.get("Agency"));
      e.setVersion(props.get("Version"));
      result.getAgentEntities().add(e);
    }

    return result;
  }

}
