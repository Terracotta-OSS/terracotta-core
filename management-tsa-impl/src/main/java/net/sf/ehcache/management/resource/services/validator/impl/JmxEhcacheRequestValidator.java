/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator.impl;

import net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidator;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;

import com.terracotta.management.service.TsaManagementClientService;

import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * <p/>
 * {@inheritDoc}
 *
 * @author Ludovic Orban
 */
public final class JmxEhcacheRequestValidator extends AbstractEhcacheRequestValidator {

  private final TsaManagementClientService tsaManagementClientService;

  private static final ThreadLocal<String> tlNode = new ThreadLocal<String>();

  public JmxEhcacheRequestValidator(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void validateSafe(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateAgentSegment(List<PathSegment> pathSegments) {
    String ids = pathSegments.get(0).getMatrixParameters().getFirst("ids");

    if (ids == null) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Only a single agent id can be used.")).build());
    } else {
      if (ids.split(",").length > 1) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Only a single agent id can be used.")).build());
      }

      try {
        Set<String> nodes = tsaManagementClientService.getL1Nodes();
        if (!nodes.contains(ids) && !AgentEntity.EMBEDDED_AGENT_ID.equals(ids)) {
          throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
              .entity(String.format("Agent ID must be in '%s'.", nodes)).build());
        }
      } catch (ServiceExecutionException see) {
        throw new RuntimeException(see);
      }

      setValidatedNode(ids);
    }
  }

  public String getValidatedNode() {
    return tlNode.get();
  }

  public void setValidatedNode(String node) {
    tlNode.set(node);
  }

}
