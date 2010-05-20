/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.NullTCObject;
import com.tc.object.dna.api.DNA;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface TCObjectFactory {

  public final static TCObject NULL_TC_OBJECT = NullTCObject.INSTANCE;

  public void setObjectManager(ClientObjectManager objectManager);

  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew);

  public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew);

  public Object getNewPeerObject(TCClass type, Object parent) throws IllegalArgumentException, SecurityException,
      InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

  public Object getNewArrayInstance(TCClass type, int size);

  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException;

  public Object getNewPeerObject(TCClass type, DNA dna) throws IOException, ClassNotFoundException;

}