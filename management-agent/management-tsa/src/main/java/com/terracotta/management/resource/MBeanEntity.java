/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a server MBean
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class MBeanEntity extends AbstractTsaEntity {

  private String sourceId;
  private String objectName;

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }
}
