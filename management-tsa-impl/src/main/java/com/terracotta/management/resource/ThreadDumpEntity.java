/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a TSA server or client
 * thread dump from the management API.
 *
 * @author Ludovic Orban
 */
public class ThreadDumpEntity extends AbstractTsaEntity {

  private String sourceId;

  private String dump;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getDump() {
    return dump;
  }

  public void setDump(String dump) {
    this.dump = dump;
  }
}
