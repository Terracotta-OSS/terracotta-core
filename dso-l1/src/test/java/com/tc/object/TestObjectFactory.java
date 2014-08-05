/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.platform.PlatformService;

public class TestObjectFactory implements TCObjectFactory {

  public TCObject tcObject;
  public Object   peerObject;

  @Override
  public void setObjectManager(ClientObjectManager objectManager) {
    return;
  }

  @Override
  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew) {
    return tcObject;
  }

  public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew) {
    return tcObject;
  }

  @Override
  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, SecurityException {
    return peerObject;
  }

  @Override
  public Object getNewPeerObject(TCClass type, DNA dna, PlatformService platformService) {
    return peerObject;
  }

  @Override
  public void initClazzIfRequired(Class clazz, TCObjectSelf tcObjectSelf) {
    throw new ImplementMe();

  }

}
