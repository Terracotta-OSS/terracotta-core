/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.management.exposed.SessionsProductMBean;
import com.tc.management.opentypes.adapters.ClassCreationCount;

public class SessionsProductPanel extends XContainer {
  private SessionsProductTable   m_sessionsProductTable;
  private XObjectTable           m_classCreationTable;
  private SessionsProductMBean   m_bean;
  private SessionsProductWrapper m_wrapper;
  
  public SessionsProductPanel(SessionsProductMBean bean) {
    super();
  
    m_wrapper = new SessionsProductWrapper(m_bean = bean);
    
    AdminClientContext cntx = AdminClient.getContext();
    load((ContainerResource)cntx.topRes.getComponent("SessionsProductPanel"));
  }
  
  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    
    m_sessionsProductTable = (SessionsProductTable)findComponent("SessionsProductTable");
    m_sessionsProductTable.setBean(m_wrapper);
    
    m_classCreationTable = (XObjectTable)findComponent("ClassCreationTable");
    m_classCreationTable.setModel(new ClassCreationTableModel());
  }
  
  public SessionsProductMBean getBean() {
    return m_bean;
  }
  
  public SessionsProductWrapper getWrapper() {
    return m_wrapper;
  }
  
  public void refresh() {
    m_wrapper = new SessionsProductWrapper(m_bean);
    m_sessionsProductTable.setBean(m_wrapper);
    
    XObjectTableModel    model = (XObjectTableModel)m_classCreationTable.getModel();
    ClassCreationCount[] ccc   = getWrapper().getClassCreationCount();
    
    model.set(ccc);
    m_classCreationTable.sort();
  }
  
  class ClassCreationTableModel extends XObjectTableModel {
    public ClassCreationTableModel() {
      super(ClassCreationCount.class,
            new String[] {"ClassName", "Count"},
            new String[] {"Class name", "Count"},
            getWrapper().getClassCreationCount());
    }
  }
  
  public void tearDown() {
    super.tearDown();
  }
}
