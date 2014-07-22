/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.AgentMetadataEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.services.AgentServiceV2;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.ActiveServerSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.ActiveServerSourceUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentServiceV2 implements AgentServiceV2 {

  private final ActiveServerSource activeServerSource;
  private final RemoteCallerV2 remoteCaller;

  public RemoteAgentServiceV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService,
                              ExecutorService executorService, RequestTicketMonitor ticketMonitor,
                              UserService userService, TimeoutService timeoutService, ActiveServerSource activeServerSource) {
    this.activeServerSource = activeServerSource;
    this.remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  @Override
  public ResponseEntityV2<AgentMetadataEntityV2> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    if (!activeServerSource.isCurrentServerActive()) {
      // cannot handle the request on this server, find an active to do the job
      ActiveServerSourceUtils.proxyClientRequest(activeServerSource.getActiveL2Urls());
    }

    ResponseEntityV2<AgentMetadataEntityV2> result = new ResponseEntityV2<AgentMetadataEntityV2>();
    
    Set<String> nodes = remoteCaller.getRemoteAgentNodeNames();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> unknownIds = new HashSet<String>(ids);
    unknownIds.removeAll(nodes);
    if (!unknownIds.isEmpty()) { throw new ServiceExecutionException("Unknown agent IDs : " + unknownIds); }

    try {
      ResponseEntityV2<AgentMetadataEntityV2> fanOutCollectionCall = remoteCaller.fanOutResponseCall(null, nodes, AgentServiceV2.class
          .getName(), AgentServiceV2.class.getMethod("getAgentsMetadata", Set.class), new Object[] { Collections.emptySet() });
      result.getEntities().addAll(fanOutCollectionCall.getEntities());
      result.getExceptionEntities().addAll(fanOutCollectionCall.getExceptionEntities());
    } catch (NoSuchMethodException nsme) {
      throw new ServiceExecutionException("Error executing remote call", nsme);
    }
    
    return result;
  }

  @Override
  public ResponseEntityV2<AgentEntityV2> getAgents(Set<String> idSet) throws ServiceExecutionException {
    if (!activeServerSource.isCurrentServerActive()) {
      // cannot handle the request on this server, find an active to do the job
      ActiveServerSourceUtils.proxyClientRequest(activeServerSource.getActiveL2Urls());
    }

    ResponseEntityV2<AgentEntityV2> result = new ResponseEntityV2<AgentEntityV2>();

    Map<String, Map<String, String>> nodes = remoteCaller.getRemoteAgentNodeDetails();
    if (idSet.isEmpty()) {
      idSet = nodes.keySet();
    }

    for (String id : idSet) {
      if (!nodes.keySet().contains(id)) { throw new ServiceExecutionException("Unknown agent ID : " + id); }
      Map<String, String> props = nodes.get(id);

      AgentEntityV2 entityV2 = new AgentEntityV2();
      entityV2.setAgentId(id);
      entityV2.setAgencyOf(props.get("Agency"));
      result.getEntities().add(entityV2);
    }

    return result;
  }

}
