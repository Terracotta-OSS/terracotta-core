/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

public class NamedTraversedReference implements TraversedReference {

  private final String className;
  private final String fieldName;
  private final Object value;

  public NamedTraversedReference(String fullyQualifiedFieldname, Object value) {
    this(null, fullyQualifiedFieldname, value);
  }

  public NamedTraversedReference(String className, String fieldName, Object value) {
    this.className = className;
    this.fieldName = fieldName;
    this.value = value;
  }
  
  @Override
  public Object getValue() {
    return this.value;
  }

  @Override
  public boolean isAnonymous() {
    return false;
  }

  @Override
  public String getFullyQualifiedReferenceName() {
    return this.className == null ? fieldName : className + "." + fieldName;
  }

}
