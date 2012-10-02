/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.loaders.Namespace;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig.Factory;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.gb.GBPersistentMapFactory;
import com.tc.objectserver.persistence.gb.GBPersistor;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates state for managed objects
 */
public class ManagedObjectStateFactory {

  private static final Map                          classNameToStateMap = new ConcurrentHashMap();
  private final ManagedObjectChangeListenerProvider listenerProvider;

  /**
   * I know singletons are BAD, but this way we save about 16 bytes for every shared object we store in the server and
   * that is huge ! So I can compromise here.
   */
  private static volatile ManagedObjectStateFactory singleton;

  // this is present for tests
  private static boolean                            disableAssertions   = false;

  private final GBPersistentMapFactory mapFactory;

  static {
    // XXX: Support for terracotta toolkit
    classNameToStateMap.put("org.terracotta.collections.quartz.DistributedSortedSet$Storage",
                            Byte.valueOf(ManagedObjectState.SET_TYPE));
  }

  private ManagedObjectStateFactory(final ManagedObjectChangeListenerProvider listenerProvider, GBPersistentMapFactory mapFactory) {
    this.listenerProvider = listenerProvider;
    this.mapFactory = mapFactory;
  }

  /*
   * @see comments above
   */
  public static synchronized ManagedObjectStateFactory createInstance(final ManagedObjectChangeListenerProvider listenerProvider,
                                                                      final GBPersistor persistor) {
    if (singleton != null && !disableAssertions) {
      // not good !!
      throw new AssertionError("This class is singleton. It is not to be instanciated more than once. " + singleton);
    }
    singleton = new ManagedObjectStateFactory(listenerProvider, persistor.getPersistentMapFactory());
    return singleton;
  }

  public static synchronized ManagedObjectStateFactory createInstance(final ManagedObjectChangeListenerProvider listenerProvider,
                                                                      final Persistor persistor) {
    throw new AssertionError();
  }

  // This is provided only for testing
  public static synchronized void disableSingleton(final boolean b) {
    disableAssertions = b;
  }

  // for tests like ObjectMangerTest and ManagedObjectStateSerializationTest
  public static void enableLegacyTypes() {
    // XXX: remove when possible
    classNameToStateMap.put("java.util.HashMap", Byte.valueOf(ManagedObjectState.MAP_TYPE));
    classNameToStateMap.put("java.util.ArrayList", Byte.valueOf(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put("java.util.HashSet", Byte.valueOf(ManagedObjectState.SET_TYPE));
  }

  // This is provided only for testing
  public static synchronized void setInstance(final ManagedObjectStateFactory factory) {
    Assert.assertNotNull(factory);
    singleton = factory;
  }

  /**
   * This method is not synchronized as the creation and access happens sequencially and this is a costly method to
   * synchronize and singleton is a volatile variable
   */
  public static ManagedObjectStateFactory getInstance() {
    Assert.assertNotNull(singleton);
    return singleton;
  }

  public ManagedObjectChangeListener getListener() {
    return this.listenerProvider.getListener();
  }

  public ManagedObjectState createState(final ObjectID oid, final ObjectID parentID, final String className,
                                        final DNACursor cursor) {
    ManagedObjectStateStaticConfig config = ManagedObjectStateStaticConfig.getConfigForClientClassName(className);
    return config.getFactory().newInstance(oid, config.ordinal(), mapFactory);
  }

  public String getClassName(final long classID) {
    return ManagedObjectStateStaticConfig.values()[((int) classID)].getClientClassName();
  }

  public PhysicalManagedObjectState createPhysicalState(final ObjectID parentID, final int classId)
      throws ClassNotFoundException {
    throw new UnsupportedOperationException();
  }

  public ManagedObjectState readManagedObjectStateFrom(final ObjectInput in, final byte type) {
    try {
      switch (type) {
        case ManagedObjectState.PHYSICAL_TYPE:
          return PhysicalManagedObjectState.readFrom(in);
        case ManagedObjectState.MAP_TYPE:
          return MapManagedObjectState.readFrom(in);
        case ManagedObjectState.PARTIAL_MAP_TYPE:
          return PartialMapManagedObjectState.readFrom(in);
        case ManagedObjectState.ARRAY_TYPE:
          return ArrayManagedObjectState.readFrom(in);
        case ManagedObjectState.LITERAL_TYPE:
          return LiteralTypesManagedObjectState.readFrom(in);
        case ManagedObjectState.LIST_TYPE:
          return ListManagedObjectState.readFrom(in);
        case ManagedObjectState.SET_TYPE:
          return SetManagedObjectState.readFrom(in);
        case ManagedObjectState.QUEUE_TYPE:
          return QueueManagedObjectState.readFrom(in);
      }

      Factory factory = ManagedObjectStateStaticConfig.Factory.getFactoryForType(type);
      if (factory != null) { return factory.readFrom(in); }

      // Unreachable!
      throw new AssertionError("Unknown type : " + type + " : Dont know how to deserialize this type !");

    } catch (final IOException e) {
      e.printStackTrace();
      throw new TCRuntimeException(e);
    } catch (final ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public ManagedObjectState recreateState(final ObjectID id, final ObjectID pid, final String className,
                                          final DNACursor cursor, final ManagedObjectState oldState) {
    throw new UnsupportedOperationException();
  }
}
