/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import java.io.Serializable;

public class NonPortableObjectState implements Serializable {
  private String label, fieldName, typeName;
  boolean        isPortable, isTransient, neverPortable, isPreInstrumented, isRepeated, isSystemType, isNull;
  String[]       requiredBootTypes;
  
  public NonPortableObjectState() {
    this(null, null, null, false, false, false, false, false, false, false);
  }

  public NonPortableObjectState(String label, String fieldName, String typeName, boolean isPortable,
                                boolean isTransient, boolean neverPortable, boolean isPreInstrumented,
                                boolean isRepeated, boolean isSystemType, boolean isNull) {
    this.label = label;
    this.fieldName = fieldName;
    this.typeName = typeName;
    this.isPortable = isPortable;
    this.isTransient = isTransient;
    this.neverPortable = neverPortable;
    this.isPreInstrumented = isPreInstrumented;
    this.isRepeated = isRepeated;
    this.isSystemType = isSystemType;
    this.isNull = isNull;
  }

  public String getLabel() {
    return this.label;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String toString() {
    return getLabel();
  }

  public boolean isPortable() {
    return this.isPortable;
  }

  public boolean isTransient() {
    return this.isTransient;
  }

  public boolean isNeverPortable() {
    return this.neverPortable;
  }

  public boolean isPreInstrumented() {
    return this.isPreInstrumented;
  }

  public boolean isSystemType() {
    return this.isSystemType;
  }
  
  public boolean isRepeated() {
    return this.isRepeated;
  }

  public boolean isNull() {
    return this.isNull;
  }
  
  public void setRequiredBootTypes(String[] requiredBootTypes) {
    this.requiredBootTypes = requiredBootTypes;
  }
  
  public String[] getRequiredBootTypes() {
    return this.requiredBootTypes;
  }
  
  public String summary() {
    StringBuffer sb = new StringBuffer();
    String l = getLabel();

    if (l == null) return "NonPortableTreeRoot";
    sb.append(l);
    if (isNeverPortable()) {
      sb.append(" Never portable");
    } else {
      sb.append(isPortable() ? " portable" : " not portable");
    }
    if (isTransient()) {
      sb.append(", transient");
    }
    if (isPreInstrumented()) {
      sb.append(", pre-instrumented");
    }
    if (isRepeated()) {
      sb.append(", repeated");
    }
    if (isSystemType()) {
      sb.append(", system type");
    }

    return sb.toString();
  }
}
