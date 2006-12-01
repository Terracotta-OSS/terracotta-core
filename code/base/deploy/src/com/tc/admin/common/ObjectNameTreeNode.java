/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
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

