package com.tc.stats;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public class DSORoot implements DSORootMBean {
  private final ObjectID           objectID;
  private final String             rootName;
  private final ObjectManagerMBean objMgr;

  public DSORoot(ObjectID rootID, ObjectManagerMBean objMgr, String name) {
    this.objectID = rootID;
    this.objMgr = objMgr;
    this.rootName = name;
  }
  
  public String getRootName() {
    return this.rootName;
  }

  public ManagedObjectFacade lookupFacade(int limit) throws NoSuchObjectException {
    return this.objMgr.lookupFacade(this.objectID, limit);
  }

}
