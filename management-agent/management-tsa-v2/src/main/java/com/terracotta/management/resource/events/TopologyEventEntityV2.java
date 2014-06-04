/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.events;

import com.terracotta.management.resource.AbstractTsaEntityV2;

/**
 * A {@link org.terracotta.management.resource.VersionedEntityV2} representing a topology event
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class TopologyEventEntityV2 extends AbstractTsaEntityV2 {

  private String sourceId;
  private String event;
  private String targetId;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }
}
