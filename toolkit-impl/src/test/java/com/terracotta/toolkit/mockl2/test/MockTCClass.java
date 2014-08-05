package com.terracotta.toolkit.mockl2.test;
/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */


import com.tc.exception.ImplementMe;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;

import java.lang.reflect.Constructor;

public class MockTCClass implements TCClass {
  private final String  name = MockTCClass.class.getName();

  public MockTCClass() {
    //
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    throw new ImplementMe();
  }

  @Override
  public Constructor getConstructor() {
    throw new ImplementMe();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
    throw new ImplementMe();
  }

  @Override
  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    throw new ImplementMe();
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object peer, final boolean isNew) {
    throw new ImplementMe();
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return false;
  }

  @Override
  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna, PlatformService platformService) {
    throw new ImplementMe();
  }

  @Override
  public Class getPeerClass() {
    return getClass();
  }

  @Override
  public ClientObjectManager getObjectManager() {
    throw new ImplementMe();
  }

}
