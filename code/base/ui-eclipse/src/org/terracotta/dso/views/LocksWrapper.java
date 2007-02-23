/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.Autolock;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Locks;
import com.terracottatech.config.NamedLock;

public class LocksWrapper {
  private DsoApplication fApp;
  
  LocksWrapper(DsoApplication app) {
    fApp = app;
  }
  
  int sizeOfAutolockArray() {
    Locks locks = fApp.getLocks();
    return locks != null ? locks.sizeOfAutolockArray() : 0;
  }
  
  Autolock getAutolockArray(int i) {
    Locks locks = fApp.getLocks();
    return locks != null ? locks.getAutolockArray(i) : null;
  }
  
  void removeAutolock(int i) {
    Locks locks = fApp.getLocks();
    if(locks != null) {
      locks.removeAutolock(i);
    }
  }
  
  int sizeOfNamedLockArray() {
    Locks locks = fApp.getLocks();
    return locks != null ? locks.sizeOfNamedLockArray() : 0;
  }
  
  NamedLock getNamedLockArray(int i) {
    Locks locks = fApp.getLocks();
    return locks != null ? locks.getNamedLockArray(i) : null;
  }
  
  void removeNamedLock(int i) {
    Locks locks = fApp.getLocks();
    if(locks != null) {
      locks.removeNamedLock(i);
    }
  }
}
