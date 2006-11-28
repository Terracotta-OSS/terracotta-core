/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;

public class SessionsProductTable extends PropertyTable {
  public SessionsProductTable() {
    super();
  }
  
  void setBean(SessionsProductWrapper bean) {
    String[] fields = {
      "RequestCount",
      "RequestCountPerSecond",
      "SessionsCreatedPerMinute",
      "SessionsExpiredPerMinute",
      "SessionWritePercentage",
    };
    PropertyTableModel model = new PropertyTableModel(bean, fields);
    setModel(model);
  }
}
