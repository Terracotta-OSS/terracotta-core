/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a topology server
 * or client's statistics from the management API.
 *
 * @author Ludovic Orban
 */
public class StatisticsEntityV2 extends AbstractTsaEntityV2 {

  private String sourceId;

  private Map<String, Object> statistics = new HashMap<String, Object>();

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public Map<String, Object> getStatistics() {
    return statistics;
  }

}
