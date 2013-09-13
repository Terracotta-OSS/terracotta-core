/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.io.Serializable;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a server MBean
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class MBeanEntity extends AbstractTsaEntity {

  private String sourceId;
  private String objectName;
  private AttributeEntity[] attributes;

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

  public AttributeEntity[] getAttributes() {
    return attributes;
  }

  public void setAttributes(AttributeEntity[] attributes) {
    this.attributes = attributes;
  }

  public static class AttributeEntity implements Serializable {
    private String name;
    private String type;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

}
