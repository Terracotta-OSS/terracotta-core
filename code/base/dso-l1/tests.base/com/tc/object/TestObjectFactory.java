/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;

public class TestObjectFactory implements TCObjectFactory {

  public TCObject tcObject;
  public Object peerObject;

  public void setObjectManager(ClientObjectManager objectManager) {
    return;
  }

  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew) {
    return tcObject;
  }

  public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew) {
    return tcObject;
  }

  public Object getNewPeerObject(TCClass type, Object parent) throws IllegalArgumentException, SecurityException {
   return peerObject;
  }

  public Object getNewArrayInstance(TCClass type, int size) {
    throw new ImplementMe();
  }

  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, SecurityException {
   return peerObject;
  }

  public Object getNewPeerObject(TCClass type, DNA dna) {
    return peerObject;
  }

}
