/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum ManagedObjectStateStaticConfig {

  /**
   * Toolkit type root - reuses map managed object state
   */
  TOOLKIT_TYPE_ROOT(ToolkitTypeNames.TOOLKIT_TYPE_ROOT_IMPL, Factory.MAP_TYPE_FACTORY),
  /**
   * Toolkit clusteredList type - reuses list object state
   */
  CLUSTERED_LIST(ToolkitTypeNames.CLUSTERED_LIST_IMPL, Factory.LIST_TYPE_FACTORY),
  /**
   * Toolkit SerializedClusteredObject type - explicit type
   */
  SERIALIZED_CLUSTER_OBJECT(ToolkitTypeNames.SERIALIZED_CLUSTERED_OBJECT_IMPL,
      Factory.SERIALIZED_CLUSTERED_OBJECT_FACTORY),
  /**
   * Toolkit Serializer map - reuses map managed object state
   */
  SERIALIZER_MAP(ToolkitTypeNames.SERIALIZER_MAP_IMPL, Factory.MAP_TYPE_FACTORY),
  /**
   * Toolkit ClusteredObjectStripe config - explicit state factory
   */
  CLUSTERED_OBJECT_STRIPE(ToolkitTypeNames.CLUSTERED_OBJECT_STRIPE_IMPL, Factory.CLUSTERED_OBJECT_STRIPE_TYPE_FACTORY),
  /**
   * ServerMap - explicit state factory
   */
  SERVER_MAP(ToolkitTypeNames.SERVER_MAP_TYPE, Factory.SERVER_MAP_TYPE_FACTORY),
  /**
   * ClusteredNotifier - explicit state factory
   */
  CLUSTERED_NOTIFIER(ToolkitTypeNames.CLUSTERED_NOTIFIER_TYPE, Factory.CLUSTERED_NOTIFIER_TYPE_FACTORY),
  /**
   * SerializedEntry - explicit state factory
   */
  SERIALIZED_ENTRY(ToolkitTypeNames.SERIALIZED_ENTRY_TYPE, Factory.SERIALIZED_ENTRY_TYPE_FACTORY),

  /**
   * CustomeLifespanSerializedEntry - explicit state factory
   */
  CUSTOM_LIFESPAN_SERIALIZED_ENTRY(ToolkitTypeNames.CUSTOM_LIFESPAN_SERIALIZED_ENTRY_TYPE,
      Factory.CUSTOM_LIFESPAN_SERIALIZED_ENTRY_TYPE_FACTORY),

  /**
   * Toolkit clusteredSortedSet type - reuses list object state
   */
  CLUSTERED_SORTED_SET(ToolkitTypeNames.CLUSTERED_SORTED_SET_IMPL, Factory.SORTED_SET_TYPE_FACTORY);

  private static final Map<String, ManagedObjectStateStaticConfig> NAME_TO_CONFIG_MAP = new ConcurrentHashMap<String, ManagedObjectStateStaticConfig>();

  static {
    for (ManagedObjectStateStaticConfig config : ManagedObjectStateStaticConfig.values()) {
      NAME_TO_CONFIG_MAP.put(config.getClientClassName(), config);
    }
  }

  public static ManagedObjectStateStaticConfig getConfigForClientClassName(String className) {
    return NAME_TO_CONFIG_MAP.get(className);
  }

  private final String  clientClassName;
  private final Factory factory;

  private ManagedObjectStateStaticConfig(String clientClassName, Factory factory) {
    this.clientClassName = clientClassName;
    this.factory = factory;
  }

  public String getClientClassName() {
    return clientClassName;
  }

  public Factory getFactory() {
    return factory;
  }

  public byte getStateObjectType() {
    return factory.getStateObjectType();
  }

  public static enum Factory {
    MAP_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.MAP_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        return MapManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new MapManagedObjectState(classId, persistentCollectionFactory.createPersistentMap(oid));
      }

    },
    LIST_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.LIST_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        return ListManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new ListManagedObjectState(classId);
      }

    },
    SERIALIZED_CLUSTERED_OBJECT_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException {
        return SerializedClusterObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new SerializedClusterObjectState(classId);
      }

    },
    CLUSTERED_OBJECT_STRIPE_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        return ClusteredObjectStripeState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new ClusteredObjectStripeState(classId);
      }

    },
    SERVER_MAP_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException {
        return ConcurrentDistributedServerMapManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new ConcurrentDistributedServerMapManagedObjectState(classId,
                                                                    persistentCollectionFactory
                                                                        .createPersistentMap(oid));
      }
    },
    CLUSTERED_NOTIFIER_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException {
        return ClusteredNotifierManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new ClusteredNotifierManagedObjectState(classId);
      }
    },
    SERIALIZED_ENTRY_TYPE_FACTORY() {

      @Override
      protected byte getStateObjectType() {
        return ManagedObjectState.TDC_SERIALIZED_ENTRY;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException {
        return TDCSerializedEntryManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new TDCSerializedEntryManagedObjectState(classId);
      }
    },
    CUSTOM_LIFESPAN_SERIALIZED_ENTRY_TYPE_FACTORY() {

      @Override
      protected byte getStateObjectType() {
        return ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException {
        return TDCCustomLifespanSerializedEntryManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new TDCCustomLifespanSerializedEntryManagedObjectState(classId);
      }
    },
    SORTED_SET_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.SET_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        return SetManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentCollectionFactory persistentCollectionFactory) {
        return new SetManagedObjectState(classId, persistentCollectionFactory.createPersistentSet(oid));
      }

    };

    private static final Map<Byte, Factory> TYPE_TO_FACTORY_MAP = new ConcurrentHashMap<Byte, Factory>();

    static {
      for (Factory factory : Factory.values()) {
        TYPE_TO_FACTORY_MAP.put(factory.getStateObjectType(), factory);
      }
    }

    public static Factory getFactoryForType(byte type) {
      return TYPE_TO_FACTORY_MAP.get(type);
    }

    private final byte implicitStateObjectType;

    private Factory() {
      this.implicitStateObjectType = (byte) (MANAGED_OBJECT_STATE_OFFSET_START + ordinal());
    }

    /**
     * Override to reuse old existing values from ManagedObjectState interface
     */
    protected byte getStateObjectType() {
      return implicitStateObjectType;
    }

    public abstract ManagedObjectState readFrom(ObjectInput objectInput) throws IOException, ClassNotFoundException;

    public abstract ManagedObjectState newInstance(ObjectID oid, long classId,
                                                   PersistentCollectionFactory persistentCollectionFactory);

    static {
      for (Factory type : Factory.values()) {
        byte byteVal = type.implicitStateObjectType;
        if (byteVal < MANAGED_OBJECT_STATE_OFFSET_START || byteVal > Byte.MAX_VALUE) { throw new AssertionError(); }

      }
    }

  }

  // a big enough offset such that no value greater than or equal to this value is defined in ManagedObjectState
  // interface - todo: add test
  private static final byte MANAGED_OBJECT_STATE_OFFSET_START = 0x30;

  public abstract static class ToolkitTypeNames {
    private static final Set<String> CONSTANTS = new HashSet<String>();

    private static String defineConstant(String constant) {
      CONSTANTS.add(constant);
      return constant;
    }

    public static Set<String> values() {
      return Collections.unmodifiableSet(CONSTANTS);
    }

    public final static String TOOLKIT_TYPE_ROOT_IMPL                = defineConstant("com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl");
    public final static String CLUSTERED_LIST_IMPL                   = defineConstant("com.terracotta.toolkit.collections.ClusteredListImpl");
    public final static String SERIALIZED_CLUSTERED_OBJECT_IMPL      = defineConstant("com.terracotta.toolkit.object.serialization.SerializedClusterObjectImpl");
    public final static String SERIALIZER_MAP_IMPL                   = defineConstant("com.terracotta.toolkit.object.serialization.SerializerMapImpl");
    public final static String CLUSTERED_OBJECT_STRIPE_IMPL          = defineConstant("com.terracotta.toolkit.object.ClusteredObjectStripeImpl");
    public final static String SERVER_MAP_TYPE                       = defineConstant("com.terracotta.toolkit.collections.ServerMap");
    public final static String CLUSTERED_NOTIFIER_TYPE               = defineConstant("com.terracotta.toolkit.events.ClusteredNotifierImpl");
    public final static String SERIALIZED_ENTRY_TYPE                 = defineConstant("com.terracotta.toolkit.object.serialization.SerializedEntry");
    public final static String CUSTOM_LIFESPAN_SERIALIZED_ENTRY_TYPE = defineConstant("com.terracotta.toolkit.object.serialization.CustomLifespanSerializedEntry");
    public final static String CLUSTERED_SORTED_SET_IMPL             = defineConstant("com.terracotta.toolkit.collections.ClusteredSortedSetImpl");
  }
}
