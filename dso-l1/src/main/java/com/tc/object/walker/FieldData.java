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

  @Override
  public boolean equals(Object o) {
    if (o instanceof FieldData) {
      FieldData other = (FieldData) o;
      return getField().getName().equals(other.getField().getName())
             && getField().getDeclaringClass().getName().equals(other.getField().getDeclaringClass().getName());
    } else {
      return false;
    }
  }

  Field getField() {
    return field;
  }
}