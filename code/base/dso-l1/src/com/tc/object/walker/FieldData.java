/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.lang.reflect.Field;

class FieldData implements Comparable {

  private final Field field;
  private boolean     isShadowed;

  FieldData(Field field) {
    this.field = field;
    this.field.setAccessible(true);
  }

  boolean isShadowed() {
    return isShadowed;
  }

  void setShadowed(boolean b) {
    this.isShadowed = b;
  }

  Object getValue(Object instance) {
    try {
      return field.get(instance);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public int compareTo(Object o) {
    if (o == null) { throw new NullPointerException(); }
    if (!(o instanceof FieldData)) { throw new ClassCastException(o.getClass().getName()); }

    FieldData other = (FieldData) o;

    String thisFieldName = field.getName();
    String otherFieldName = other.field.getName();

    int i = thisFieldName.compareTo(otherFieldName);
    if (i == 0) {
      return field.getDeclaringClass().getName().compareTo(other.field.getDeclaringClass().getName());
    } else {
      return i;
    }
  }

  Field getField() {
    return field;
  }
}