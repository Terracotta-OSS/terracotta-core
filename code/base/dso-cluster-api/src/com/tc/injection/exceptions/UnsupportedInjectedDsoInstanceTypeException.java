/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.injection.exceptions;

/**
 * This exception is thrown when DSO doesn't support the injection type that is specified for a field.
 *
 * @since 3.0.0
 */
public class UnsupportedInjectedDsoInstanceTypeException extends RuntimeException {

  private final String containingClassName;
  private final String fieldName;
  private final String type;

  public UnsupportedInjectedDsoInstanceTypeException(final String containingClassName, final String fieldName,
                                                     final String type) {
    super("Unsupported injection type '" + type + "' for field '" + fieldName + "' of class '" + containingClassName + "'");

    this.containingClassName = containingClassName;
    this.fieldName = fieldName;
    this.type = type;
  }

  /**
   * Returns the name of the class that contains the field with an unsupported injection type.
   *
   * @return the name of the field's class
   */
  public String getContainingClassName() {
    return containingClassName;
  }

  /**
   * Returns the name the field with an unsupported injection type.
   *
   * @return the name of the field
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Returns the unsupported injection type.
   *
   * @return the unsupported injection type
   */
  public String getUnsupportedType() {
    return type;
  }
}