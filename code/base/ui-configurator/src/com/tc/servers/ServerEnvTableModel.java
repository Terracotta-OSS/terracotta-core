/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.servers;

import com.tc.admin.common.XObjectTableModel;

public class ServerEnvTableModel extends XObjectTableModel {
  static String[] FIELDS = {"Name", "Value"};
  
  public ServerEnvTableModel() {
    super(ServerProperty.class, FIELDS, FIELDS);
  }
  
  public ServerProperty getServerPropertyAt(int row) {
    return (ServerProperty)getObjectAt(row);
  }
  
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex == 1;
  }
}
