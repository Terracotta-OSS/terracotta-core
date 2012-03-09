/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.api.StringIndex;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.loaders.Namespace;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.Persistor;
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
  private final StringIndex                         stringIndex;
  private final PhysicalManagedObjectStateFactory   physicalMOFactory;

  /**
   * I know singletons are BAD, but this way we save about 16 bytes for every shared object we store in the server and
   * that is huge ! So I can compromise here.
   */
  private static volatile ManagedObjectStateFactory singleton;

  // this is present for tests
  private static boolean                            disableAssertions   = false;

  private final PersistentCollectionFactory         persistentCollectionFactory;

  static {
    // XXX: Support for CDM, CDSM in terracotta-toolkit
    classNameToStateMap.put("com.terracotta.toolkit.collections.ConcurrentDistributedMapDso",
                            Byte.valueOf(ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE));
    classNameToStateMap.put("com.terracotta.toolkit.collections.ConcurrentDistributedServerMapDso",
                            Byte.valueOf(ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE));
    // XXX: Support for Ehcache entry type
    classNameToStateMap.put(TDCSerializedEntryManagedObjectState.SERIALIZED_ENTRY,
                            Byte.valueOf(ManagedObjectState.TDC_SERIALIZED_ENTRY));
    classNameToStateMap.put(TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_SERIALIZED_ENTRY,
                            Byte.valueOf(ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY));
    // XXX: Support for terracotta toolkit
    classNameToStateMap.put("org.terracotta.async.ProcessingBucketItems", Byte.valueOf(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put("org.terracotta.collections.ConcurrentBlockingQueue",
                            Byte.valueOf(ManagedObjectState.QUEUE_TYPE));
    classNameToStateMap.put("org.terracotta.collections.TerracottaList", Byte.valueOf(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put("org.terracotta.collections.quartz.DistributedSortedSet$Storage",
                            Byte.valueOf(ManagedObjectState.SET_TYPE));
  }

  private ManagedObjectStateFactory(final ManagedObjectChangeListenerProvider listenerProvider,
                                    final StringIndex stringIndex,
                                    final PhysicalManagedObjectStateFactory physicalMOFactory,
                                    final PersistentCollectionFactory factory) {
    this.listenerProvider = listenerProvider;
    this.stringIndex = stringIndex;
    this.physicalMOFactory = physicalMOFactory;
    this.persistentCollectionFactory = factory;
  }

  /*
   * @see comments above
   */
  public static synchronized ManagedObjectStateFactory createInstance(final ManagedObjectChangeListenerProvider listenerProvider,
                                                                      final Persistor persistor) {
    if (singleton != null && !disableAssertions) {
      // not good !!
      throw new AssertionError("This class is singleton. It is not to be instanciated more than once. " + singleton);
    }
    singleton = new ManagedObjectStateFactory(listenerProvider, persistor.getStringIndex(),
                                              new PhysicalManagedObjectStateFactory(persistor.getClassPersistor()),
                                              persistor.getPersistentCollectionFactory());
    return singleton;
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

  public StringIndex getStringIndex() {
    return this.stringIndex;
  }

  public ManagedObjectChangeListener getListener() {
    return this.listenerProvider.getListener();
  }

  public ManagedObjectState createState(final ObjectID oid, final ObjectID parentID, final String className,
                                        final DNACursor cursor) {
    final byte type = getStateObjectTypeFor(className);

    if (type == ManagedObjectState.LITERAL_TYPE) { return new LiteralTypesManagedObjectState(); }

    final long classID = getClassID(className);

    if (type == ManagedObjectState.PHYSICAL_TYPE) { return this.physicalMOFactory.create(classID, oid, parentID,
                                                                                         className, cursor); }
    switch (type) {
      case ManagedObjectState.ARRAY_TYPE:
        return new ArrayManagedObjectState(classID);
      case ManagedObjectState.MAP_TYPE:
        return new MapManagedObjectState(classID, this.persistentCollectionFactory.createPersistentMap(oid));
      case ManagedObjectState.PARTIAL_MAP_TYPE:
        return new PartialMapManagedObjectState(classID, this.persistentCollectionFactory.createPersistentMap(oid));
      case ManagedObjectState.SET_TYPE:
        return new SetManagedObjectState(classID, this.persistentCollectionFactory.createPersistentSet(oid));
      case ManagedObjectState.LIST_TYPE:
        return new ListManagedObjectState(classID);
      case ManagedObjectState.QUEUE_TYPE:
        return new QueueManagedObjectState(classID);
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE:
        return new ConcurrentDistributedMapManagedObjectState(classID,
                                                              this.persistentCollectionFactory.createPersistentMap(oid));
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
        return new ConcurrentDistributedServerMapManagedObjectState(classID,
                                                                    this.persistentCollectionFactory
                                                                        .createPersistentMap(oid));
      case ManagedObjectState.TDC_SERIALIZED_ENTRY:
        return new TDCSerializedEntryManagedObjectState(classID);
      case ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY:
        return new TDCCustomLifespanSerializedEntryManagedObjectState(classID);
    }
    // Unreachable
    throw new AssertionError("Type : " + type + " is unknown !");
  }

  private long getClassID(final String className) {
    return getStringIndex().getOrCreateIndexFor(className);
  }

  public String getClassName(final long classID) {
    return getStringIndex().getStringFor(classID);
  }

  private byte getStateObjectTypeFor(String className) {
    final String logicalExtendingClassName = Namespace.parseLogicalNameIfNeceesary(className);
    if (logicalExtendingClassName != null) {
      final Byte t = (Byte) classNameToStateMap.get(logicalExtendingClassName);
      if (t != null) { return t.byteValue(); }

      className = Namespace.parseClassNameIfNecessary(className);
    }

    if (className.startsWith("[")) { return ManagedObjectState.ARRAY_TYPE; }

    final Byte type = (Byte) classNameToStateMap.get(className);
    if (type != null) { return type.byteValue(); }
    if (LiteralValues.isLiteral(className)) { return ManagedObjectState.LITERAL_TYPE; }
    return ManagedObjectState.PHYSICAL_TYPE;
  }

  public PhysicalManagedObjectState createPhysicalState(final ObjectID parentID, final int classId)
      throws ClassNotFoundException {
    return this.physicalMOFactory.create(parentID, classId);
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
        case ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE:
          return ConcurrentDistributedMapManagedObjectState.readFrom(in);
        case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
          return ConcurrentDistributedServerMapManagedObjectState.readFrom(in);
        case ManagedObjectState.TDC_SERIALIZED_ENTRY:
          return TDCSerializedEntryManagedObjectState.readFrom(in);
        case ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY:
          return TDCCustomLifespanSerializedEntryManagedObjectState.readFrom(in);
        default:
          throw new AssertionError("Unknown type : " + type + " : Dont know how to deserialize this type !");
      }
    } catch (final IOException e) {
      e.printStackTrace();
      throw new TCRuntimeException(e);
    } catch (final ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public ManagedObjectState recreateState(final ObjectID id, final ObjectID pid, final String className,
                                          final DNACursor cursor, final ManagedObjectState oldState) {
    Assert.assertEquals(ManagedObjectState.PHYSICAL_TYPE, oldState.getType());
    final long classID = getClassID(className);
    return this.physicalMOFactory.recreate(classID, pid, className, cursor, (PhysicalManagedObjectState) oldState);
  }
}
