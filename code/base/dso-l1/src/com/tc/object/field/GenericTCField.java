/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.field;

import com.tc.object.LiteralValues;
import com.tc.object.TCClass;
import com.tc.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author orion
 */
public class GenericTCField implements TCField {

  private static final LiteralValues literalValues = new LiteralValues();

  private final TCClass              tcClass;
  private final boolean              isPortable;
  private final String               fieldName;
  private final boolean              isFinal;
  private final boolean              isArray;
  private final boolean              canBeReference;

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
    this.canBeReference = isReferenceField(field);
  }

  private static boolean isReferenceField(Field field) {
    if (Modifier.isStatic(field.getModifiers())) return false;
    Class type = field.getType();

    if (literalValues.isLiteral(type.getName())) {
      return !type.isPrimitive();
    }

    return true;
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
