package com.terracotta.management.resource.services.validator;

import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.resource.services.validator.RequestValidator;

import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
public class TSARequestValidatorV2 implements RequestValidator {

  @Override
  public void validateSafe(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  @Override
  public void validate(UriInfo info) {
    validateAgentSegment(info.getPathSegments());
  }

  private void validateAgentSegment(List<PathSegment> pathSegments) {
    String ids = pathSegments.get(0).getMatrixParameters().getFirst("ids");

    if (Utils.trimToNull(ids) != null && !AgentEntityV2.EMBEDDED_AGENT_ID.equals(ids)) {
      throw new ResourceRuntimeException(String.format("Agent ID must be '%s'.", AgentEntityV2.EMBEDDED_AGENT_ID),
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
