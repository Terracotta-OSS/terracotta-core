/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

/**
 * A physical object change action
 */
public class PhysicalAction {
  private final String     field;
  private final Object     value;
  private final int        index;
  private final boolean    isReference;
  private final int        type;

  private static final int TRUE_PHYSICAL = 1;
  private static final int ARRAY_ELEMENT = 2;
  private static final int ENTIRE_ARRAY  = 3;
  private static final int SUB_ARRAY     = 4;

  // TODO: These 3 constructors would probably be easier to use if they were static factory methods instead

  public PhysicalAction(Object value) {
    this(null, -1, value, false, ENTIRE_ARRAY);
  }
  
  public PhysicalAction(Object value, int startPos) {
    this(null, startPos, value, false, SUB_ARRAY);
  }

  public PhysicalAction(int index, Object value, boolean isReference) {
    this(null, index, value, isReference, ARRAY_ELEMENT);
  }

  public PhysicalAction(String field, Object value, boolean isReference) {
    this(field, -1, value, isReference, TRUE_PHYSICAL);
  }

  private PhysicalAction(String field, int index, Object value, boolean isReference, int type) {
    this.field = field;
    this.index = index;
    this.value = value;
    this.isReference = isReference;
    this.type = type;
  }

  public String getFieldName() {
    if (type != TRUE_PHYSICAL) { throw new IllegalStateException(String.valueOf(type)); }

    return this.field;
  }

  public Object getObject() {
    return this.value;
  }

  public boolean isReference() {
    return isReference;
  }

  public int getArrayIndex() {
    if (!((type == ARRAY_ELEMENT) || (type == SUB_ARRAY))) { throw new IllegalStateException(String.valueOf(type)); }
    return this.index;
  }

  public boolean isTruePhysical() {
    return type == TRUE_PHYSICAL;
  }

  public boolean isArrayElement() {
    return type == ARRAY_ELEMENT;
  }

  public boolean isEntireArray() {
    return type == ENTIRE_ARRAY;
  }
  
  public boolean isSubArray() {
    return type == SUB_ARRAY;
  }

}