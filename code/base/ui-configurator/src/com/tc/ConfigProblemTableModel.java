/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.xmlbeans.XmlError;

import com.tc.admin.common.XObjectTableModel;

import java.util.List;

public class ConfigProblemTableModel extends XObjectTableModel {
  private static final String[] FIELDS = {"Message", "Line", "Column"};
  
  public ConfigProblemTableModel() {
    super(XmlError.class, FIELDS, FIELDS);
  }
  
  public ConfigProblemTableModel(List errorList) {
    this();
    setErrors(errorList);
  }
  
  public void setErrors(List errorList) {
    set(errorList.iterator());
  }
  
  public XmlError getError(int row) {
    return (XmlError)getObjectAt(row);
  }
}
