/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;
import com.tc.stats.api.DSOClassInfo;

public class ClassTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "ClassName", "InstanceCount" };

  private static final String[] HEADERS = { "dso.classes.className", "dso.classes.instanceCount" };

  public ClassTableModel(ApplicationContext appContext) {
    super(DSOClassInfo.class, FIELDS, appContext.getMessages(HEADERS));
  }

  public void setClassInfo(DSOClassInfo classInfo[]) {
    set(classInfo);
  }
}
