/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.lang.TCThreadGroup;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.platform.PlatformService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author orion
 */
public class TCObjectFactoryImpl implements TCObjectFactory {
  private final static Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

  private ClientObjectManager   objectManager;
  private final TCClassFactory  clazzFactory;

  public TCObjectFactoryImpl(TCClassFactory clazzFactory) {
    this.clazzFactory = clazzFactory;
  }

  @Override
  public void setObjectManager(ClientObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  @Override
  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew) {
    TCClass tcc = clazzFactory.getOrCreate(clazz, objectManager);
    TCObject rv = tcc.createTCObject(id, peer, isNew);

    if (peer instanceof Manageable) {
      ((Manageable) peer).__tc_managed(rv);
    }

    return rv;
  }

  @Override
  public void initClazzIfRequired(Class clazz, TCObjectSelf tcObjectSelf) {
    TCClass tcc = clazzFactory.getOrCreate(clazz, objectManager);
    tcObjectSelf.initClazzIfRequired(tcc);
  }

  // public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew) {
  // return getNewInstance(id, null, clazz, isNew);
  // }

  @Override
  public Object getNewPeerObject(TCClass type, DNA dna, PlatformService platformService) throws IOException,
      ClassNotFoundException {
    return type.getNewInstanceFromNonDefaultConstructor(dna, platformService);
  }

  @Override
  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Constructor ctor = type.getConstructor();
    if (ctor == null) throw new AssertionError("type:" + type.getName());
    return getNewPeerObject(ctor, EMPTY_OBJECT_ARRAY, type);
  }

  private Object getNewPeerObject(Constructor ctor, Object[] args, TCClass type)
      throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    final Object rv;

    // XXX: hack to workaround issue with commons logging dependence on context loader
    final Thread thread = Thread.currentThread();
    final ClassLoader prevLoader = thread.getContextClassLoader();
    final boolean adjustTCL = TCThreadGroup.currentThreadInTCThreadGroup();

    if (adjustTCL) {
      ClassLoader newTcl = type.getPeerClass().getClassLoader();
      if (newTcl == null) {
        // XXX: workaround jboss bug: http://jira.jboss.com/jira/browse/JBAS-4437
        newTcl = ClassLoader.getSystemClassLoader();
      }
      thread.setContextClassLoader(newTcl);
    }

    try {
      rv = ctor.newInstance(args);
    } finally {
      if (adjustTCL) thread.setContextClassLoader(prevLoader);
    }
    return rv;
  }
}
