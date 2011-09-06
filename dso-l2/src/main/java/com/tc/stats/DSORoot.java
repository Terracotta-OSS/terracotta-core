/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.stats.api.DSORootMBean;

import javax.management.NotCompliantMBeanException;

public class DSORoot extends AbstractTerracottaMBean implements DSORootMBean {
  private final ObjectID           objectID;
  private final String             rootName;
  private final ObjectManagerMBean objMgr;

  public DSORoot(ObjectID rootID, ObjectManagerMBean objMgr, String name) throws NotCompliantMBeanException {
    super(DSORootMBean.class, false);

    this.objectID = rootID;
    this.objMgr = objMgr;
    this.rootName = name;
  }

  public String getRootName() {
    return this.rootName;
  }

  /**
   * NOTE: this only works in a non-AA cluster. Leaving it here because RootTool uses it.
   */
  @Deprecated
  public ManagedObjectFacade lookupFacade(int limit) throws NoSuchObjectException {
    return this.objMgr.lookupFacade(this.objectID, limit);
  }

  public ObjectID getObjectID() {
    return objectID;
  }

  public void reset() {
    /**/
  }
}
