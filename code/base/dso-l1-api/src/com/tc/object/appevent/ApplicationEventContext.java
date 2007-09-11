/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import java.io.Serializable;

import javax.swing.tree.DefaultTreeModel;

public interface ApplicationEventContext extends Serializable {
  Object getPojo();

  DefaultTreeModel getTreeModel();

  void setTreeModel(DefaultTreeModel treeModel);
  
  String getProjectName();
}
