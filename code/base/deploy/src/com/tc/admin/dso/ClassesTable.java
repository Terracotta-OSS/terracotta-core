/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.XObjectTable;
import com.tc.stats.DSOClassInfo;

public class ClassesTable extends XObjectTable {
  public ClassesTable(ClassTableModel model) {
    super(model);
    setSortDirection(UP);
    setSortColumn(1 /* instanceCount */);
  }

  public void setClassInfo(DSOClassInfo classInfo[]) {
    ((ClassTableModel) getModel()).setClassInfo(classInfo);
    sort();
  }
}
