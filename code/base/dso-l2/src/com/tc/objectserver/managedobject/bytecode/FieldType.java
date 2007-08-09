/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject.bytecode;

import com.tc.object.LiteralValues;

public class FieldType {

  public static final LiteralValues literalValues = (LiteralValues) new LiteralValues();

  private final String localFieldName;
  private final String fullName;
  private final int type;
  private final boolean isReference;

  public FieldType(String name, String fullName, int type, boolean isReference) {
    this.localFieldName = name;
    this.fullName = fullName;
    this.type = type;
    this.isReference = isReference;
  }

  public static FieldType create(String fullyQualifiedName, Object value, boolean isReference, int id) {
    String localName = getLocalFieldName(fullyQualifiedName , id);
    if(value == null) {
      return new FieldType(localName, fullyQualifiedName, LiteralValues.OBJECT, isReference);
    }
    return new FieldType(localName, fullyQualifiedName, literalValues.valueFor(value), isReference);
  }

  // TODO:: field Names are currently fully qualified unnecessarily.
  public static String getLocalFieldName(String fullyQualifiedName, int id) {
    int lastIdx = fullyQualifiedName.lastIndexOf('.');
    if(lastIdx < 0 ) {
      return fullyQualifiedName + "_" + id;
    } else if(lastIdx < fullyQualifiedName.length() - 1) {
      return fullyQualifiedName.substring(lastIdx + 1) + "_" + id;
    } else {
      throw new AssertionError("Field Name " + fullyQualifiedName + " is not a valid Field Name !");
    }

  }

  public String getLocalFieldName() {
    return localFieldName;
  }

  public String getQualifiedName() {
    return fullName;
  }

  public int getType() {
    if(isReference) return LiteralValues.OBJECT_ID;
    return type;
  }

}
