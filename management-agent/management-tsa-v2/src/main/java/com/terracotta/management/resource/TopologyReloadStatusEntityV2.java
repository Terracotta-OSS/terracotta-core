/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a topology reload status
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class TopologyReloadStatusEntityV2 extends AbstractTsaEntityV2 {

  private String sourceId;
  private String status;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
