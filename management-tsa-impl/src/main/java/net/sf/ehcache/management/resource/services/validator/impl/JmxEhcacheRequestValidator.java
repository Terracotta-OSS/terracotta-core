/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services.validator.impl;

import net.sf.ehcache.management.resource.services.validator.AbstractEhcacheRequestValidator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
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

  private final MBeanServerConnection mBeanServerConnection;

  private static final ThreadLocal<String> tlNode = new ThreadLocal<String>();

  public JmxEhcacheRequestValidator(MBeanServerConnection mBeanServerConnection) {
    this.mBeanServerConnection = mBeanServerConnection;
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

    if (ids != null) {
      if (ids.split(",").length > 1) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Only a single agent id can be used.")).build());
      }

      Set<String> nodes = getNodes();
      if (!nodes.contains(ids)) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(String.format("Agent ID must be in '%s'.", nodes)).build());
      }

      tlNode.set(ids);
    }
  }

  private Set<String> getNodes() {
    try {
      Set<String> nodes = new HashSet<String>();
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null);
      for (ObjectName objectName : objectNames) {
        String node = objectName.getKeyProperty("node");
        nodes.add(node);
      }
      return nodes;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getValidatedNode() {
    return tlNode.get();
  }

  public void setValidatedNode(String node) {
    tlNode.set(node);
  }

}
