/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Ludovic Orban
 */
public class RemoteAgentServiceV2 implements AgentServiceV2 {

  private final L1MBeansSource l1MBeansSource;
  private final RemoteCallerV2 remoteCaller;

  public RemoteAgentServiceV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService,
                              ExecutorService executorService, RequestTicketMonitor ticketMonitor,
                              UserService userService, TimeoutService timeoutService, L1MBeansSource l1MBeansSource) {
    this.l1MBeansSource = l1MBeansSource;
    this.remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
  }

  @Override
  public ResponseEntityV2<AgentMetadataEntityV2> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    if (!l1MBeansSource.containsJmxMBeans()) {
      // cannot handle the request on this server, find another one to do the job
      l1MBeansSource.proxyClientRequest();
      return null;
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
    if (!l1MBeansSource.containsJmxMBeans()) {
      // cannot handle the request on this server, find another one to do the job
      l1MBeansSource.proxyClientRequest();
      return null;
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
      entityV2.getRootRepresentables().putAll(props);
      entityV2.getRootRepresentables().remove("Agency");
      result.getEntities().add(entityV2);
    }

    return result;
  }

}
