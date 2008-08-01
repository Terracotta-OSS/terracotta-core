/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.AbstractTerracottaMBean;

import javax.management.NotCompliantMBeanException;

public class L1Dumper extends AbstractTerracottaMBean implements L1DumperMBean {

  private final TCDumper dumper;

  public L1Dumper(TCDumper client) throws NotCompliantMBeanException {
    super(L1DumperMBean.class, false);
    this.dumper = client;
  }

  public void reset() {
    //
  }

  public void dump() {
    dumper.dump();
  }

}
