/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.util.UnsafeUtil;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author orion
 */
public class TCObjectFactoryImpl implements TCObjectFactory {
  private final static Object[]   EMPTY_OBJECT_ARRAY   = new Object[] {};

  private ClientObjectManager     objectManager;
  private final TCClassFactory    clazzFactory;

  public TCObjectFactoryImpl(TCClassFactory clazzFactory) {
    this.clazzFactory = clazzFactory;
  }

  public void setObjectManager(ClientObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz) {
    TCClass tcc = clazzFactory.getOrCreate(clazz, objectManager);
    TCObject rv = tcc.createTCObject(id, peer);

    if (peer instanceof Manageable) {
      ((Manageable) peer).__tc_managed(rv);
    }

    return rv;
  }

  public TCObject getNewInstance(ObjectID id, Class clazz) {
    return getNewInstance(id, null, clazz);
  }

  public Object getNewPeerObject(TCClass type, DNA dna) throws IOException, ClassNotFoundException {
    return type.getNewInstanceFromNonDefaultConstructor(dna);
  }

  public Object getNewPeerObject(TCClass type, Object parent) throws IllegalArgumentException, SecurityException,
      InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    // This one is for non-static inner classes
    return getNewPeerObject(type.getConstructor(), type, parent);
  }

  public Object getNewArrayInstance(TCClass type, int size) {
    return Array.newInstance(type.getComponentType(), size);
  }

  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Constructor ctor = type.getConstructor();
    if (ctor == null) throw new AssertionError("type:" + type.getName());
    return getNewPeerObject(ctor, EMPTY_OBJECT_ARRAY);
  }

  private Object getNewPeerObject(Constructor ctor, Object[] args) throws IllegalArgumentException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    final Object rv;

    // XXX: hack to workaround issue with commons logging dependence on context loader
    Thread thread = Thread.currentThread();
    ClassLoader cl = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(ctor.getDeclaringClass().getClassLoader());
      rv = ctor.newInstance(args);
    } finally {
      thread.setContextClassLoader(cl);
    }
    return rv;
  }

  private Object getNewPeerObject(Constructor ctor, TCClass type, Object parent) throws IllegalArgumentException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    final Object rv;

    // XXX: hack to workaround issue with commons logging dependence on context loader
    Thread thread = Thread.currentThread();
    ClassLoader cl = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(ctor.getDeclaringClass().getClassLoader());
      rv = ctor.newInstance(EMPTY_OBJECT_ARRAY);
      UnsafeUtil.setField(rv, type.getParentField(), parent);
    } finally {
      thread.setContextClassLoader(cl);
    }
    return rv;
  }

}