/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import javax.swing.tree.DefaultTreeModel;

public abstract class AbstractApplicationEventContext implements ApplicationEventContext {

  private static final long      serialVersionUID = 4788562594133534828L;

  private final transient Object pojo;
  private final String           threadName;
  private final String           clientId;
  private final String           targetClassName;
  private DefaultTreeModel       treeModel;

  private String                 projectName;                            // optional: client Eclipse

  // project name

  public AbstractApplicationEventContext(Object pojo, String threadName, String clientId) {
    this.pojo = pojo;
    this.targetClassName = pojo.getClass().getName();
    this.threadName = threadName;
    this.clientId = clientId;
    this.projectName = System.getProperty("project.name");
  }

  public Object getPojo() {
    return pojo;
  }

  public String getTargetClassName() {
    return targetClassName;
  }

  public String getThreadName() {
    return threadName;
  }

  public String getClientId() {
    return clientId;
  }

  public void setTreeModel(DefaultTreeModel treeModel) {
    this.treeModel = treeModel;
  }

  public DefaultTreeModel getTreeModel() {
    return treeModel;
  }

  public void setProjectName(String name) {
    projectName = name;
  }

  public String getProjectName() {
    return projectName;
  }
}
