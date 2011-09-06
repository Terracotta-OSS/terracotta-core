/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.management.ObjectName;

public class PolledAttribute {
  private final ObjectName objectName;
  private final String     attribute;
  private int              hashCode;

  public PolledAttribute(ObjectName objectName, String attribute) {
    this.objectName = objectName;
    this.attribute = attribute;
    hashCode = new HashCodeBuilder().append(objectName).append(attribute).toHashCode();
  }

  public ObjectName getObjectName() {
    return objectName;
  }

  public String getAttribute() {
    return attribute;
  }

  public int hashCode() {
    return hashCode;
  }

  public boolean equals(Object object) {
    if (!(object instanceof PolledAttribute)) return false;
    PolledAttribute other = (PolledAttribute) object;
    return StringUtils.equals(getAttribute(), other.getAttribute()) && getObjectName().equals(other.getObjectName());
  }
}
