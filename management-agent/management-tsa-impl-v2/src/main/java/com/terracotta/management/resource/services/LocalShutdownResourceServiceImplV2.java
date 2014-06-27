/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracotta.management.resource.ForceStopEntityV2;
import com.terracotta.management.resource.ServerEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.service.ShutdownServiceV2;
import com.terracotta.management.service.TopologyServiceV2;
import com.terracotta.management.service.impl.util.LocalManagementSource;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing local server shutdown.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/local/shutdown")
public class LocalShutdownResourceServiceImplV2 {

  private static final TCLogger LOG = TCLogging.getLogger(LocalShutdownResourceServiceImplV2.class);

  private final ShutdownServiceV2 shutdownService;
  private final TopologyServiceV2 topologyService;
  private final LocalManagementSource localManagementSource = new LocalManagementSource();

  public LocalShutdownResourceServiceImplV2() {
    this.shutdownService = ServiceLocator.locate(ShutdownServiceV2.class);
    this.topologyService = ServiceLocator.locate(TopologyServiceV2.class);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void shutdown(@Context UriInfo info, ForceStopEntityV2 force) {
    LOG.info(String.format("Invoking shutdown: %s", info.getRequestUri()));

    try {
      if (force != null && !force.isForceStop() && !isPassiveStandbyAvailable() && localManagementSource.isLegacyProductionModeEnabled()) {
        String errorMessage = "No passive server available in Standby mode. Use force option to stop the server.";
        LOG.debug(errorMessage);
        throw new ResourceRuntimeException(errorMessage, Response.Status.BAD_REQUEST.getStatusCode());
      }

      shutdownService.shutdown(Collections.singleton(localManagementSource.getLocalServerName()));
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to shutdown TSA", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
    
  }

  private boolean isPassiveStandbyAvailable() throws ServiceExecutionException {
    ServerGroupEntityV2 currentServerGroup = getCurrentServerGroup();
    if(currentServerGroup == null){
      return false;
    }
    for (ServerEntityV2 serverEntity : currentServerGroup.getServers()) {
      if ("PASSIVE-STANDBY".equals(serverEntity.getAttributes().get("State"))) {
        return true;
      }
    }
    return false;
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
