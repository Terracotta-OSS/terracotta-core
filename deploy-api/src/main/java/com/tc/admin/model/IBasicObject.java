/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

public interface IBasicObject extends IObject {
  public static final IBasicObject[] NULL_SET = {};

  int getFieldCount();

  boolean isArray();

  boolean isCollection();

  boolean isSet();

  boolean isMap();

  boolean isList();

  boolean isCycle();

  IObject getCycleRoot();

  boolean isComplete();

  String getType();

  Object getValue();

  String[] getFieldNames();

  String getFieldName(int index);

  IObject getField(int index);

  boolean isValid();

  void initFields();

  void refresh();

  IBasicObject newCopy();
}
