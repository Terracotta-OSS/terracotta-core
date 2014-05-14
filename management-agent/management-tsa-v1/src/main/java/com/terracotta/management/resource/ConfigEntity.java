/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a configuration
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class ConfigEntity extends AbstractTsaEntity {

  private String sourceId;

  private Map<String, Object> attributes = new HashMap<String, Object>();

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(final String sourceId) {
    this.sourceId = sourceId;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

}
