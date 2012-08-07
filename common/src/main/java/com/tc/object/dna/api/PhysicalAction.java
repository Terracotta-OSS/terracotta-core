/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  /**
   * Construct a physical action representing a new value for an entire array
   * 
   * @param value The new array
   */
  public PhysicalAction(Object value) {
    this(null, -1, value, false, ENTIRE_ARRAY);
  }

  /**
   * Construct a physical action representing a new subarray
   * 
   * @param value The new subarray
   * @param startPos The starting position for the new subarray
   */
  public PhysicalAction(Object value, int startPos) {
    this(null, startPos, value, false, SUB_ARRAY);
  }

  /**
   * Construct a physical action representing a single array element change.
   * 
   * @param index The index in the array parent
   * @param value The new value for the array element
   * @param isReference Whether the new value is a reference
   */
  public PhysicalAction(int index, Object value, boolean isReference) {
    this(null, index, value, isReference, ARRAY_ELEMENT);
  }

  /**
   * Construct a physical action that consists of a field, a new value, and whether the new value is a reference.
   */
  public PhysicalAction(String field, Object value, boolean isReference) {
    this(field, -1, value, isReference, TRUE_PHYSICAL);
  }

  /**
   * Main internal constructor with all parameters.
   * 
   * @param field Field name, will be null for all but true physical
   * @param index Array index or -1 if not an array index
   * @param value Value
   * @param isReference True if reference
   * @param type Internal type flag, not publicly exposed
   */
  private PhysicalAction(String field, int index, Object value, boolean isReference, int type) {
    this.field = field;
    this.index = index;
    this.value = value;
    this.isReference = isReference;
    this.type = type;
  }

  /**
   * Get field name, only valid if this physical action is a true physical field change.
   * 
   * @return Field name, never null
   * @throws IllegalStateException If called on an action that returns false for {@link #isTruePhysical()}
   */
  public String getFieldName() {
    if (type != TRUE_PHYSICAL) { throw new IllegalStateException(String.valueOf(type)); }

    return this.field;
  }

  /**
   * Get object value
   * 
   * @return Object value
   */
  public Object getObject() {
    return this.value;
  }

  /**
   * Is the object a reference?
   * 
   * @return True if reference
   */
  public boolean isReference() {
    return isReference;
  }

  /**
   * If this is an array element, the index of the element. If this is a subarray, the starting position of the new
   * subarray. Otherwise, an error.
   * 
   * @return The array index
   * @throws IllegalStateException If not an array element or subarray
   */
  public int getArrayIndex() {
    if (!((type == ARRAY_ELEMENT) || (type == SUB_ARRAY))) { throw new IllegalStateException(String.valueOf(type)); }
    return this.index;
  }

  /**
   * @return True if this is a true physical field change
   */
  public boolean isTruePhysical() {
    return type == TRUE_PHYSICAL;
  }

  /**
   * @return True if this is an array element change
   */
  public boolean isArrayElement() {
    return type == ARRAY_ELEMENT;
  }

  /**
   * @return True if this is an entire array change
   */
  public boolean isEntireArray() {
    return type == ENTIRE_ARRAY;
  }

  /**
   * @return True if this is a subarray change
   */
  public boolean isSubArray() {
    return type == SUB_ARRAY;
  }

  @Override
  public String toString() {
    return "PhysicalAction [field=" + field + ", value=" + value + ", index=" + index + ", isReference=" + isReference
           + ", type=" + type + "]";
  }

}