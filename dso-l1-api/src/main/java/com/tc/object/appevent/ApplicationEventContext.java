/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import java.io.Serializable;

import javax.swing.tree.DefaultTreeModel;

/**
 * Event context information
 */
public interface ApplicationEventContext extends Serializable {
  
  /**
   * @return Object that the event is related to
   */
  Object getPojo();

  /**
   * @return A tree model defining the object hierarchy of interest for this event
   */
  DefaultTreeModel getTreeModel();

  /**
   * @param treeModel Set the tree model 
   */
  void setTreeModel(DefaultTreeModel treeModel);
  
  /**
   * Optional - specify Eclipse project
   * @return Eclipse project name
   */
  String getProjectName();
}
