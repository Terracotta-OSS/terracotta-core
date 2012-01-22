/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;

public interface IObject {
  Object getFacade();

  ObjectID getObjectID();

  String getName();

  IObject getParent();

  IObject getRoot();

  int getBatchSize();

  void setBatchSize(int limit);
}
