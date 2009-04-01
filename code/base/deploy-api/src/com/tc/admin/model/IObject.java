/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
