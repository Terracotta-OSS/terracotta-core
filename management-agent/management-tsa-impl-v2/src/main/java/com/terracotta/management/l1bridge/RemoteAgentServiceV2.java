/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AbstractEntityV2;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;
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
public class RemoteAgentServiceV2 implements AgentServiceV2 {

  private final RemoteCaller remoteCaller;

  public RemoteAgentServiceV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService,
                            ExecutorService executorService, RequestTicketMonitor ticketMonitor,
                            UserService userService, TimeoutService timeoutService) {
    this.remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  @Override
  public ResponseEntityV2 getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    ResponseEntityV2 result = new ResponseEntityV2();
    
    Set<String> nodes = remoteCaller.getRemoteAgentNodeNames();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> unknownIds = new HashSet<String>(ids);
    unknownIds.removeAll(nodes);
    if (!unknownIds.isEmpty()) { throw new ServiceExecutionException("Unknown agent IDs : " + unknownIds); }

    try {
      Collection<? extends AbstractEntityV2> fanOutCollectionCall = remoteCaller.fanOutCollectionCall(null, nodes, AgentServiceV2.class.getName(), AgentServiceV2.class.getMethod("getAgentsMetadata", Set.class), new Object[] {Collections.emptySet()});
      result.getEntities().addAll(fanOutCollectionCall);
    } catch (NoSuchMethodException nsme) {
      throw new ServiceExecutionException("Error executing remote call", nsme);
    }
    
    return result;
  }

  @Override
  public ResponseEntityV2 getAgents(Set<String> idSet) throws ServiceExecutionException {
    ResponseEntityV2 result = new ResponseEntityV2();

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
      result.getEntities().add(e);
    }

    return result;
  }

}
