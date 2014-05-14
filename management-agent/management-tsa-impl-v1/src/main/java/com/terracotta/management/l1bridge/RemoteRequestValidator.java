/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
public final class RemoteRequestValidator implements RequestValidator {

  private final RemoteAgentBridgeService remoteAgentBridgeService;

  private static final ThreadLocal<Set<String>> tlNode = new ThreadLocal<Set<String>>();

  public RemoteRequestValidator(RemoteAgentBridgeService remoteAgentBridgeService) {
    this.remoteAgentBridgeService = remoteAgentBridgeService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void validateSafe(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  @Override
  public void validate(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  protected void validateAgentSegment(List<PathSegment> pathSegments) {
    String ids = pathSegments.get(0).getMatrixParameters().getFirst("ids");

    try {
      Set<String> nodes = remoteAgentBridgeService.getRemoteAgentNodeNames();
      if (ids == null) {
        setValidatedNodes(nodes);
      } else {
        String[] idsArray = ids.split(",");

        for (String id : idsArray) {
          if (!nodes.contains(id) && !AgentEntity.EMBEDDED_AGENT_ID.equals(id)) {
            throw new ResourceRuntimeException(
                    String.format("Agent IDs must be in '%s' or '%s'.", nodes, AgentEntity.EMBEDDED_AGENT_ID),
                    Response.Status.BAD_REQUEST.getStatusCode());
          }
        }

        setValidatedNodes(new HashSet<String>(Arrays.asList(idsArray)));
      }
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException(
              "Unexpected error validating request.",
              see,
              Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  public Set<String> getValidatedNodes() {
    return tlNode.get();
  }

  public String getSingleValidatedNode() {
    if (tlNode.get().size() != 1) {
      throw new RuntimeException("A single node ID must be specified, got: " + tlNode.get());
    }
    return tlNode.get().iterator().next();
  }

  public void setValidatedNodes(Set<String> node) {
    tlNode.set(node);
  }

}
