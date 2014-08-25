/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.services.AgentService;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentService implements AgentService {

  private final L1MBeansSource l1MBeansSource;
  private final RemoteCaller remoteCaller;

  public RemoteAgentService(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService,
                            ExecutorService executorService, RequestTicketMonitor ticketMonitor,
                            UserService userService, TimeoutService timeoutService, L1MBeansSource l1MBeansSource) {
    this.l1MBeansSource = l1MBeansSource;
    this.remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    if (!l1MBeansSource.containsJmxMBeans()) {
      // cannot handle the request on this server, find an active to do the job
      l1MBeansSource.proxyClientRequest();
      return null;
    }

    Set<String> nodes = remoteCaller.getRemoteAgentNodeNames();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> unknownIds = new HashSet<String>(ids);
    unknownIds.removeAll(nodes);
    if (!unknownIds.isEmpty()) { throw new ServiceExecutionException("Unknown agent IDs : " + unknownIds); }

    try {
      return remoteCaller.fanOutCollectionCall(null, nodes, AgentService.class.getName(), AgentService.class.getMethod("getAgentsMetadata", Set.class), new Object[] {Collections.emptySet()});
    } catch (NoSuchMethodException nsme) {
      throw new ServiceExecutionException("Error executing remote call", nsme);
    }
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> idSet) throws ServiceExecutionException {
    if (!l1MBeansSource.containsJmxMBeans()) {
      // cannot handle the request on this server, find an active to do the job
      l1MBeansSource.proxyClientRequest();
      return null;
    }

    Collection<AgentEntity> result = new ArrayList<AgentEntity>();

    Map<String, Map<String, String>> nodes = remoteCaller.getRemoteAgentNodeDetails();
    if (idSet.isEmpty()) {
      idSet = nodes.keySet();
    }

    for (String id : idSet) {
      if (!nodes.keySet().contains(id)) { throw new ServiceExecutionException("Unknown agent ID : " + id); }
      Map<String, String> props = nodes.get(id);

      AgentEntity e = new AgentEntity();
      e.setAgentId(id);
      e.setAgencyOf(props.get("Agency"));
      e.setVersion(props.get("Version"));
      e.getRootRepresentables().putAll(props);
      e.getRootRepresentables().remove("Agency");
      e.getRootRepresentables().remove("Version");
      result.add(e);
    }

    return result;
  }

}
