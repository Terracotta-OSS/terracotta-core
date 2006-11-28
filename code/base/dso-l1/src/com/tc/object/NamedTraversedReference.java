/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

public class NamedTraversedReference implements TraversedReference {

  private final String className;
  private final String fieldName;
  private final Object value;

  public NamedTraversedReference(String fullyQualifiedFieldname, Object value) {
    this.className = null;
    this.fieldName = fullyQualifiedFieldname;
    this.value = value;
  }

  public NamedTraversedReference(String className, String fieldName, Object value) {
    this.className = className;
    this.fieldName = fieldName;
    this.value = value;
  }
  
  public Object getValue() {
    return this.value;
  }

  public boolean isAnonymous() {
    return false;
  }

  public String getFullyQualifiedReferenceName() {
    return this.className == null ? fieldName : className + "." + fieldName;
  }

}
