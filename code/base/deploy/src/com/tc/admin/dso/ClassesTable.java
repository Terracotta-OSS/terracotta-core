/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;

import com.tc.stats.DSOClassInfo;

public class ClassesTable extends XObjectTable {
  public ClassesTable() {
    super(new ClassTableModel());
    
    setSortColumn(1 /*instanceCount*/);
    setSortDirection(UP);
  }

  public void setClassInfo(DSOClassInfo classInfo[]) {
    ((ClassTableModel)getModel()).setClassInfo(classInfo);
    sort();
  }
}

class ClassTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = {
    "ClassName",
    "InstanceCount"
  };

  private static final String[] HEADERS =
    AdminClient.getContext().getMessages(
      new String[] {
        "dso.classes.className",
        "dso.classes.instanceCount"
      });
  
  public ClassTableModel() {
    super(DSOClassInfo.class, FIELDS, HEADERS);
  }

  public void setClassInfo(DSOClassInfo classInfo[]) {
    set(classInfo);
  }
}
