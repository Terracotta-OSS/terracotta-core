/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator.impl;

import net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidator;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.service.TsaManagementClientService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  private static final ThreadLocal<Set<String>> tlNode = new ThreadLocal<Set<String>>();

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
      setValidatedNodes(Collections.<String>emptySet());
    } else {
      String[] idsArray = ids.split(",");

      try {
        Set<String> nodes = tsaManagementClientService.getL1Nodes().keySet();
        for (String id : idsArray) {
          if (!nodes.contains(id) && !AgentEntity.EMBEDDED_AGENT_ID.equals(id)) {
            throw new ResourceRuntimeException(
                String.format("Agent IDs must be in '%s' or '%s'.", nodes, AgentEntity.EMBEDDED_AGENT_ID),
                Response.Status.BAD_REQUEST.getStatusCode());
          }
        }

        setValidatedNodes(new HashSet<String>(Arrays.asList(idsArray)));
      } catch (ServiceExecutionException see) {
        throw new ResourceRuntimeException(
            "Unexpected error validating request.",
            see,
            Response.Status.BAD_REQUEST.getStatusCode());
      }
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
