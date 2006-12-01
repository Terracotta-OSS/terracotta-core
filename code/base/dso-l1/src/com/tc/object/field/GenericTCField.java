/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.field;

import com.tc.object.TCClass;
import com.tc.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author orion
 */
public class GenericTCField implements TCField {
  private final TCClass tcClass;
  private final boolean isPortable;
  private final String  fieldName;
  private final boolean isFinal;
  private final boolean isArray;
  private final boolean canBeReference;

  protected GenericTCField(TCClass tcClass, Field field, boolean portable) {
    Assert.eval(tcClass != null);
    Assert.eval(field != null);
    this.tcClass = tcClass;
    this.fieldName = (tcClass.getName() + "." + field.getName()).intern();

    // special case for synthetic parent field
    portable = portable || fieldName.equals(tcClass.getParentFieldName());
    this.isPortable = portable && !field.getType().getName().startsWith("com.tc.");

    this.isFinal = Modifier.isFinal(field.getModifiers());
    this.isArray = field.getType().isArray();
    this.canBeReference = TCFieldFactory.isReferenceField(field);
  }

  public TCClass getDeclaringTCClass() {
    return tcClass;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isPortable() {
    return isPortable;
  }

  public boolean isArray() {
    return this.isArray;
  }

  public String getName() {
    return fieldName;
  }

  public boolean canBeReference() {
    return this.canBeReference;
  }
  
  public String toString() {
    return getClass().getName() + "(" + getName() + ")";
  }
}
