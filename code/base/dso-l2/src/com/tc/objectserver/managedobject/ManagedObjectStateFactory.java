/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

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

/**
 * Creates state for managed objects
 */
public class ManagedObjectStateFactory {

  private static final LiteralValues                literalValues       = new LiteralValues();
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
    classNameToStateMap.put(java.util.IdentityHashMap.class.getName(), new Byte(ManagedObjectState.MAP_TYPE));
    classNameToStateMap.put(java.util.Hashtable.class.getName(), new Byte(ManagedObjectState.PARTIAL_MAP_TYPE));
    classNameToStateMap.put(java.util.Properties.class.getName(), new Byte(ManagedObjectState.PARTIAL_MAP_TYPE));
    classNameToStateMap.put(gnu.trove.THashMap.class.getName(), new Byte(ManagedObjectState.MAP_TYPE));
    classNameToStateMap.put(java.util.HashMap.class.getName(), new Byte(ManagedObjectState.PARTIAL_MAP_TYPE));
    classNameToStateMap
        .put(java.util.Collections.EMPTY_MAP.getClass().getName(), new Byte(ManagedObjectState.MAP_TYPE));
    classNameToStateMap.put(java.util.LinkedHashMap.class.getName(), new Byte(ManagedObjectState.LINKED_HASHMAP_TYPE));
    classNameToStateMap.put(java.util.TreeMap.class.getName(), new Byte(ManagedObjectState.TREE_MAP_TYPE));
    classNameToStateMap.put(gnu.trove.THashSet.class.getName(), new Byte(ManagedObjectState.SET_TYPE));
    classNameToStateMap.put(java.util.HashSet.class.getName(), new Byte(ManagedObjectState.SET_TYPE));
    classNameToStateMap.put(java.util.LinkedHashSet.class.getName(), new Byte(ManagedObjectState.SET_TYPE));
    classNameToStateMap
        .put(java.util.Collections.EMPTY_SET.getClass().getName(), new Byte(ManagedObjectState.SET_TYPE));
    classNameToStateMap.put(java.util.TreeSet.class.getName(), new Byte(ManagedObjectState.TREE_SET_TYPE));
    classNameToStateMap.put(java.util.LinkedList.class.getName(), new Byte(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put(java.util.ArrayList.class.getName(), new Byte(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put(java.util.Vector.class.getName(), new Byte(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put(java.util.Stack.class.getName(), new Byte(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put(java.util.Collections.EMPTY_LIST.getClass().getName(),
                            new Byte(ManagedObjectState.LIST_TYPE));
    classNameToStateMap.put(java.util.Date.class.getName(), new Byte(ManagedObjectState.DATE_TYPE));
    classNameToStateMap.put(java.sql.Date.class.getName(), new Byte(ManagedObjectState.DATE_TYPE));
    classNameToStateMap.put(java.sql.Time.class.getName(), new Byte(ManagedObjectState.DATE_TYPE));
    classNameToStateMap.put(java.sql.Timestamp.class.getName(), new Byte(ManagedObjectState.DATE_TYPE));
    // These 1.5 classes needs to be compiled in 1.4 !!
    classNameToStateMap.put("java.util.concurrent.LinkedBlockingQueue", new Byte(ManagedObjectState.QUEUE_TYPE));
    classNameToStateMap.put("java.util.concurrent.ConcurrentHashMap",
                            new Byte(ManagedObjectState.CONCURRENT_HASHMAP_TYPE));
  }

  private ManagedObjectStateFactory(ManagedObjectChangeListenerProvider listenerProvider, StringIndex stringIndex,
                                    PhysicalManagedObjectStateFactory physicalMOFactory,
                                    PersistentCollectionFactory factory) {
    this.listenerProvider = listenerProvider;
    this.stringIndex = stringIndex;
    this.physicalMOFactory = physicalMOFactory;
    this.persistentCollectionFactory = factory;
  }

  /*
   * @see comments above
   */
  public static synchronized ManagedObjectStateFactory createInstance(
                                                                      ManagedObjectChangeListenerProvider listenerProvider,
                                                                      Persistor persistor) {
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
  public static synchronized void disableSingleton(boolean b) {
    disableAssertions = b;
  }

  // This is provided only for testing
  public static synchronized void setInstance(ManagedObjectStateFactory factory) {
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
    return stringIndex;
  }

  public ManagedObjectChangeListener getListener() {
    return listenerProvider.getListener();
  }

  public ManagedObjectState createState(ObjectID oid, ObjectID parentID, String className, String loaderDesc,
                                        DNACursor cursor) {
    byte type = getStateObjectTypeFor(className);

    if (type == ManagedObjectState.LITERAL_TYPE) { return new LiteralTypesManagedObjectState(); }

    final long classID = getClassID(className, loaderDesc);

    if (type == ManagedObjectState.PHYSICAL_TYPE) { return physicalMOFactory.create(classID, parentID, className,
                                                                                    loaderDesc, cursor); }
    switch (type) {
      case ManagedObjectState.ARRAY_TYPE:
        return new ArrayManagedObjectState(classID);
      case ManagedObjectState.MAP_TYPE:
        return new MapManagedObjectState(classID, persistentCollectionFactory.createPersistentMap(oid));
      case ManagedObjectState.PARTIAL_MAP_TYPE:
        return new PartialMapManagedObjectState(classID, persistentCollectionFactory.createPersistentMap(oid));
      case ManagedObjectState.LINKED_HASHMAP_TYPE:
        return new LinkedHashMapManagedObjectState(classID);
      case ManagedObjectState.TREE_MAP_TYPE:
        return new TreeMapManagedObjectState(classID);
      case ManagedObjectState.SET_TYPE:
        return new SetManagedObjectState(classID);
      case ManagedObjectState.TREE_SET_TYPE:
        return new TreeSetManagedObjectState(classID);
      case ManagedObjectState.LIST_TYPE:
        return new ListManagedObjectState(classID);
      case ManagedObjectState.QUEUE_TYPE:
        return new QueueManagedObjectState(classID);
      case ManagedObjectState.DATE_TYPE:
        return new DateManagedObjectState(classID);
      case ManagedObjectState.CONCURRENT_HASHMAP_TYPE:
        return new ConcurrentHashMapManagedObjectState(classID);
    }
    // Unreachable
    throw new AssertionError("Type : " + type + " is unknown !");
  }

  private long getClassID(String className, String loaderDesc) {
    return getStringIndex().getOrCreateIndexFor(loaderDesc + Namespace.getClassNameAndLoaderSeparator() + className);
  }

  public String getClassName(long classID) {
    String s = null;
    try {
      String separator = Namespace.getClassNameAndLoaderSeparator();
      s = getStringIndex().getStringFor(classID);
      return s.substring(s.indexOf(separator) + separator.length());
    } catch (Exception ex) {
      throw new AssertionError("loaderDesc://:ClassName string for classId  " + classID + " not in the right format : "
                               + s);
    }
  }

  public String getLoaderDescription(long classID) {
    String s = null;
    try {
      String separator = Namespace.getClassNameAndLoaderSeparator();
      s = getStringIndex().getStringFor(classID);
      return s.substring(0, s.indexOf(separator));
    } catch (Exception ex) {
      throw new AssertionError("loaderDesc://:ClassName string for classId  " + classID + " not in the right format : "
                               + s);
    }
  }

  private byte getStateObjectTypeFor(String className) {
    String logicalExtendingClassName = Namespace.parseLogicalNameIfNeceesary(className);
    if (logicalExtendingClassName != null) {
      Byte t = (Byte) classNameToStateMap.get(logicalExtendingClassName);
      if (t != null) { return t.byteValue(); }

      className = Namespace.parseClassNameIfNecessary(className);
    }

    if (className.startsWith("[")) { return ManagedObjectState.ARRAY_TYPE; }
    Byte type = (Byte) classNameToStateMap.get(className);
    if (type != null) { return type.byteValue(); }
    if (literalValues.isLiteral(className)) { return ManagedObjectState.LITERAL_TYPE; }
    return ManagedObjectState.PHYSICAL_TYPE;
  }

  public PhysicalManagedObjectState createPhysicalState(ObjectID parentID, int classId) throws ClassNotFoundException {
    return physicalMOFactory.create(parentID, classId);
  }

  public ManagedObjectState readManagedObjectStateFrom(ObjectInput in, byte type) {
    try {
      switch (type) {
        case ManagedObjectState.PHYSICAL_TYPE:
          return PhysicalManagedObjectState.readFrom(in);
        case ManagedObjectState.MAP_TYPE:
          return MapManagedObjectState.readFrom(in);
        case ManagedObjectState.PARTIAL_MAP_TYPE:
          return PartialMapManagedObjectState.readFrom(in);
        case ManagedObjectState.LINKED_HASHMAP_TYPE:
          return LinkedHashMapManagedObjectState.readFrom(in);
        case ManagedObjectState.ARRAY_TYPE:
          return ArrayManagedObjectState.readFrom(in);
        case ManagedObjectState.DATE_TYPE:
          return DateManagedObjectState.readFrom(in);
        case ManagedObjectState.LITERAL_TYPE:
          return LiteralTypesManagedObjectState.readFrom(in);
        case ManagedObjectState.LIST_TYPE:
          return ListManagedObjectState.readFrom(in);
        case ManagedObjectState.SET_TYPE:
          return SetManagedObjectState.readFrom(in);
        case ManagedObjectState.TREE_SET_TYPE:
          return TreeSetManagedObjectState.readFrom(in);
        case ManagedObjectState.TREE_MAP_TYPE:
          return TreeMapManagedObjectState.readFrom(in);
        case ManagedObjectState.CONCURRENT_HASHMAP_TYPE:
          return ConcurrentHashMapManagedObjectState.readFrom(in);
        case ManagedObjectState.QUEUE_TYPE:
          return QueueManagedObjectState.readFrom(in);
        default:
          throw new AssertionError("Unknown type : " + type + " : Dont know how to deserialize this type !");
      }
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public ManagedObjectState recreateState(ObjectID id, ObjectID pid, String className, String loaderDesc,
                                          DNACursor cursor, ManagedObjectState oldState) {
    Assert.assertEquals(ManagedObjectState.PHYSICAL_TYPE, oldState.getType());
    final long classID = getClassID(className, loaderDesc);
    return physicalMOFactory.recreate(classID, pid, className, loaderDesc, cursor,
                                      (PhysicalManagedObjectState) oldState);
  }
}
