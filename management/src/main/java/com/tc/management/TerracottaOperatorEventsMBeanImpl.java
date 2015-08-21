/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class TerracottaOperatorEventsMBeanImpl extends AbstractNotifyingMBean implements TerracottaOperatorEventsMBean {

  public TerracottaOperatorEventsMBeanImpl() throws NotCompliantMBeanException {
    super(TerracottaOperatorEventsMBean.class);
  }

  @Override
  public void fireOperatorEvent(TerracottaOperatorEvent tcEvent) {
    sendNotification(TERRACOTTA_OPERATOR_EVENT, tcEvent, tcEvent);
  }

  @Override
  public void reset() {
    //
  }

  @Override
  public void logOperatorEvent(TerracottaOperatorEvent event) {
    fireOperatorEvent(event);
  }
  
}
