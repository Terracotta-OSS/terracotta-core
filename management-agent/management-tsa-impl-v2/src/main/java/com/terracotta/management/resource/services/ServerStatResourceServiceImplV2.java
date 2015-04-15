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
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.resource.ServerEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.ServerStatEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.service.TopologyServiceV2;
import com.terracotta.management.service.impl.util.LocalManagementSource;

import java.util.Collection;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing local server shutdown.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/local/stat")
public class ServerStatResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(ServerStatResourceServiceImplV2.class);

  private final TopologyServiceV2 topologyService;
  private final LocalManagementSource localManagementSource = new LocalManagementSource();

  public ServerStatResourceServiceImplV2() {
    this.topologyService = ServiceLocator.locate(TopologyServiceV2.class);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ServerStatEntityV2 shutdown(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ServerStatResourceServiceImplV2.shutdown: %s", info.getRequestUri()));

    try {
      ServerGroupEntityV2 currentServerGroup = getCurrentServerGroup();
      ServerEntityV2 currentServer = getCurrentServer(currentServerGroup);

      String health = "OK";
      String role =  null;
      String state = null;
      if(currentServer != null) {
        role = (currentServer.getAttributes().get("State").equals("ACTIVE-COORDINATOR") ? "ACTIVE" : "PASSIVE");
        state = (String) currentServer.getAttributes().get("State");
      }
      String managementPort = currentServer.getAttributes().get("ManagementPort").toString();
      String serverGroupName = currentServerGroup.getName();

      return new ServerStatEntityV2(health, role, state, managementPort, serverGroupName,
          localManagementSource.getLocalServerName());
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to shutdown TSA", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  private ServerEntityV2 getCurrentServer(ServerGroupEntityV2 currentServerGroup) throws ServiceExecutionException {
    String localServerName = localManagementSource.getLocalServerName();
    for (ServerEntityV2 server : currentServerGroup.getServers()) {
      if (server.getAttributes().get("Name").equals(localServerName)) {
        return server;
      }
    }
    return null;
  }

  private ServerGroupEntityV2 getCurrentServerGroup() throws ServiceExecutionException {
    String localServerName = localManagementSource.getLocalServerName();
    Collection<TopologyEntityV2> serverTopologies = topologyService.getServerTopologies(null).getEntities();
    for (TopologyEntityV2 serverTopology : serverTopologies) {
      Set<ServerGroupEntityV2> serverGroups = serverTopology.getServerGroupEntities();
      for (ServerGroupEntityV2 serverGroup : serverGroups) {
        Set<ServerEntityV2> servers = serverGroup.getServers();
        for (ServerEntityV2 server : servers) {
          if (server.getAttributes().get("Name").equals(localServerName)) {
            return serverGroup;
          }
        }
      }
    }
    return null;
  }

}
