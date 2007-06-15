/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NonPortableObjectState implements Serializable {
  private String   label, fieldName, typeName;
  private boolean  isPortable, isTransient, neverPortable, isPreInstrumented, isRepeated, isSystemType, isNull;
  private String   explaination;
  private NonPortableReason nonPortableReason;
  private List requiredBootTypes;
  private List requiredIncludeTypes;

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

  public void setExplaination(String explaination) {
    this.explaination = explaination;
  }
  
  public String getExplaination() {
    return this.explaination;
  }
  
  public void setNonPortableReason(NonPortableReason nonPortableReason) {
    this.nonPortableReason = nonPortableReason;
    requiredBootTypes = new ArrayList(nonPortableReason.getErroneousBootJarSuperClasses());
    if(isRequiredBootJarType()) {
      requiredBootTypes.add(getTypeName());
    }
    requiredIncludeTypes = new ArrayList(nonPortableReason.getErroneousSuperClasses());
    if(isRequiredIncludeType()) {
      requiredIncludeTypes.add(getTypeName());
    }
  }
  
  public NonPortableReason getNonPortableReason() {
    return this.nonPortableReason;
  }
  
  public String getLabel() {
    return this.label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  public String getTypeName() {
    return this.typeName;
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

  public List getRequiredBootTypes() {
    return requiredBootTypes;
  }

  public List getRequiredIncludeTypes() {
    return requiredIncludeTypes;
  }
  
  public List getNonPortableBaseTypes() {
    return nonPortableReason != null ? nonPortableReason.getErroneousSuperClasses() : null;
  }

  public boolean extendsLogicallyManagedType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS;
    }
    return false;
  }
  
  public boolean isRequiredBootJarType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.CLASS_NOT_IN_BOOT_JAR;
    }
    return false;
  }

  public boolean isRequiredIncludeType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG;
    }
    return false;
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
