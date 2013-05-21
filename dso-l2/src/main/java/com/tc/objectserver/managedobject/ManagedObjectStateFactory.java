/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.google.common.eventbus.EventBus;
import com.tc.exception.TCRuntimeException;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig.Factory;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * Creates state for managed objects
 */
public class ManagedObjectStateFactory {

  private final ManagedObjectChangeListenerProvider listenerProvider;

  /**
   * I know singletons are BAD, but this way we save about 16 bytes for every shared object we store in the server and
   * that is huge ! So I can compromise here.
   */
  private static volatile ManagedObjectStateFactory singleton;

  // this is present for tests
  private static boolean                            disableAssertions   = false;

  private final PersistentObjectFactory objectFactory;

  private final EventBus operationEventBus;

  private ManagedObjectStateFactory(final ManagedObjectChangeListenerProvider listenerProvider,
                                    final PersistentObjectFactory objectFactory,
                                    final EventBus operationEventBus) {
    this.listenerProvider = listenerProvider;
    this.objectFactory = objectFactory;
    this.operationEventBus = operationEventBus;
  }

  /*
   * @see comments above
   */
  public static synchronized ManagedObjectStateFactory createInstance(final ManagedObjectChangeListenerProvider listenerProvider,
                                                                      final Persistor persistor) {
    return createInstance(listenerProvider, persistor.getPersistentObjectFactory());
  }

  /*
  * @see comments above
  */
  public static synchronized ManagedObjectStateFactory createInstance(final ManagedObjectChangeListenerProvider listenerProvider,
                                                                      final PersistentObjectFactory persistentObjectFactory) {
    if (singleton != null && !disableAssertions) {
      // not good !!
      throw new AssertionError("This class is singleton. It is not to be instantiated more than once. " + singleton);
    }
    singleton = new ManagedObjectStateFactory(listenerProvider, persistentObjectFactory, new EventBus("main-event-bus"));
    return singleton;
  }

  // This is provided only for testing
  public static synchronized void disableSingleton(final boolean b) {
    disableAssertions = b;
  }

  // for tests like ObjectMangerTest and ManagedObjectStateSerializationTest
  public static void enableLegacyTypes() {
    throw new UnsupportedOperationException("Legacy types not supported");
  }

  // This is provided only for testing
  public static synchronized void setInstance(final ManagedObjectStateFactory factory) {
    Assert.assertNotNull(factory);
    singleton = factory;
  }

  /**
   * This method is not synchronized as the creation and access happens sequentially and this is a costly method to
   * synchronize and singleton is a volatile variable
   */
  public static ManagedObjectStateFactory getInstance() {
    Assert.assertNotNull(singleton);
    return singleton;
  }

  public ManagedObjectChangeListener getListener() {
    return this.listenerProvider.getListener();
  }

  public EventBus getOperationEventBus() {
    return operationEventBus;
  }

  public ManagedObjectState createState(final ObjectID oid, final ObjectID parentID, final String className,
                                        final DNACursor cursor) {
    ManagedObjectStateStaticConfig config = ManagedObjectStateStaticConfig.getConfigForClientClassName(className);
    if (config == null) {
      throw new IllegalArgumentException("'" + className + "' is not a supported managed object type.");
    }
    return config.getFactory().newInstance(oid, config.ordinal(), objectFactory);
  }

  public String getClassName(final long classID) {
    return ManagedObjectStateStaticConfig.values()[((int) classID)].getClientClassName();
  }

  public ManagedObjectState readManagedObjectStateFrom(final ObjectInput in, final byte type) throws IOException {
    try {
      Factory factory = ManagedObjectStateStaticConfig.Factory.getFactoryForType(type);
      if (factory != null) { return factory.readFrom(in, objectFactory); }

      // Unreachable!
      throw new AssertionError("Unknown type : " + type + " : Dont know how to deserialize this type !");
    } catch (final ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }
}
