/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.objectserver.persistence.db.PersistableCollection;

public interface PersistableObjectState {

  public void setPersistentCollection(PersistableCollection collection);
  
  public PersistableCollection getPersistentCollection();
}
