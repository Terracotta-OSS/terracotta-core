/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean.domain;

public class PersistentObj {
  private Integer id = null;
  private PersistentObj child = null;
  private String strFld = null;
  
  public PersistentObj getChild() {
    return child;
  }
  public void setChild(PersistentObj child) {
    this.child = child;
  }
  public Integer getId() {
    return id;
  }
  private void setId(Integer id) {
    this.id = id;
  }
  public String getStrFld() {
    return strFld;
  }
  public void setStrFld(String strFld) {
    this.strFld = strFld;
  }
}
