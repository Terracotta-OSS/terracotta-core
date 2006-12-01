/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.management.beans.sessions.SessionMonitorMBean;

public class SessionMonitorTable extends PropertyTable {
  private SessionMonitorMBean m_bean;
  
  public SessionMonitorTable() {
    super();
  }
  
  void setBean(SessionMonitorMBean bean) {
    if(bean == null) {
      return;
    }
    
    String[] fields = {
      "RequestCount",
      "RequestRatePerSecond",
      "CreatedSessionCount",
      "SessionCreationRatePerMinute",
      "DestroyedSessionCount",
      "SessionDestructionRatePerMinute"
    };
    SessionMonitorWrapper wrapper = new SessionMonitorWrapper(m_bean = bean);
    PropertyTableModel model = new PropertyTableModel(wrapper, fields);
    setModel(model);
  }
  
  public void refresh() {
    PropertyTableModel model = (PropertyTableModel)getModel();
    model.setInstance(new SessionMonitorWrapper(m_bean));
  }
}
