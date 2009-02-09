/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.injection.exceptions;


public class UnsupportedInjectedDsoInstanceTypeException extends RuntimeException {

  private final String containingClassName;
  private final String fieldName;
  private final String typeName;

  public UnsupportedInjectedDsoInstanceTypeException(final String containingClassName, final String fieldName, final String typeName) {
    super("Unsupported injection type '"+typeName+"' for field '"+fieldName+"' of class '"+containingClassName+"'");

    this.containingClassName = containingClassName;
    this.fieldName = fieldName;
    this.typeName = typeName;
  }

  public String getContainingClassName() {
    return containingClassName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getUnsupportedTypeName() {
    return typeName;
  }
}