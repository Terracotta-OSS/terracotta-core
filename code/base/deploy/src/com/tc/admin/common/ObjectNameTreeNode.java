package com.tc.admin.common;

import javax.management.ObjectName;

public class ObjectNameTreeNode extends XTreeNode {
  public ObjectNameTreeNode(ObjectName objectName) {
    super(objectName);
  }

  public void setObjectName(ObjectName objectName) {
    setUserObject(objectName);
  }

  public ObjectName getObjectName() {
    return (ObjectName)getUserObject();
  }
}

