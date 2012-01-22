/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object in the object graph held in a non-portable event context.
 */
public class NonPortableObjectState implements Serializable {
  private String   label, fieldName, typeName;
  private boolean  isPortable, isTransient, neverPortable, isPreInstrumented, isRepeated, isSystemType, isNull;
  private String   explaination;
  private NonPortableReason nonPortableReason;
  private List requiredBootTypes;
  private List requiredIncludeTypes;

  /**
   * Construct with all default values.
   */
  public NonPortableObjectState() {
    this(null, null, null, false, false, false, false, false, false, false);
  }

  /**
   * Construct with all values
   * @param label Label
   * @param fieldName Field name
   * @param typeName Type name
   * @param isPortable Portable flag
   * @param isTransient Transient flag
   * @param neverPortable Never portable flag
   * @param isPreInstrumented Is pre-instrumented flag
   * @param isRepeated Is repeated  flag
   * @param isSystemType Is system type
   * @param isNull Is null
   */
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

  /**
   * Set explanation
   * @param explaination Explanation
   */
  public void setExplaination(String explaination) {
    this.explaination = explaination;
  }
  
  /**
   * @return Explanation
   */
  public String getExplaination() {
    return this.explaination;
  }
  
  /**
   * Set reason why this object is non portable
   * @param nonPortableReason The reason
   */
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
  
  /**
   * @return Reason why this object is non portable
   */
  public NonPortableReason getNonPortableReason() {
    return this.nonPortableReason;
  }
  
  /**
   * @return Label
   */
  public String getLabel() {
    return this.label;
  }

  /**
   * @param label label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @return Field name 
   */
  public String getFieldName() {
    return this.fieldName;
  }

  /**
   * @poram fieldName Field name
   */
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  
  /**
   * Get type name for this node's state
   */
  public String getTypeName() {
    return this.typeName;
  }

  public String toString() {
    return getLabel();
  }

  /**
   * @return True if portable
   */
  public boolean isPortable() {
    return this.isPortable;
  }

  /**
   * @return True if this is transient
   */
  public boolean isTransient() {
    return this.isTransient;
  }

  /**
   * @return True if this type is never portable
   */
  public boolean isNeverPortable() {
    return this.neverPortable;
  }

  /**
   * @return True if this is pre-instrumented
   */
  public boolean isPreInstrumented() {
    return this.isPreInstrumented;
  }

  /**
   * @return True if this is a system type
   */
  public boolean isSystemType() {
    return this.isSystemType;
  }

  /**
   * @return True if repeated 
   */
  public boolean isRepeated() {
    return this.isRepeated;
  }

  /**
   * @return True if null
   */
  public boolean isNull() {
    return this.isNull;
  }

  /**
   * @return List of types that should be included in the boot jar
   */
  public List getRequiredBootTypes() {
    return requiredBootTypes;
  }

  /**
   * @return List of types that should be included in the config
   */
  public List getRequiredIncludeTypes() {
    return requiredIncludeTypes;
  }
  
  /**
   * @return List of all super classes that are non-portable objects
   */
  public List getNonPortableBaseTypes() {
    return nonPortableReason != null ? nonPortableReason.getErroneousSuperClasses() : null;
  }

  /**
   * @return True if class extends a logically managed type
   */
  public boolean extendsLogicallyManagedType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS;
    }
    return false;
  }
  
  /**
   * @return True if class is required in boot jar
   */
  public boolean isRequiredBootJarType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.CLASS_NOT_IN_BOOT_JAR;
    }
    return false;
  }

  /**
   * @return True if class was not included in the config
   */
  public boolean isRequiredIncludeType() {
    if(nonPortableReason != null) {
      return nonPortableReason.getReason() == NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG;
    }
    return false;
  }
  
  /**
   * @return Get summary of this node's state
   */
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
