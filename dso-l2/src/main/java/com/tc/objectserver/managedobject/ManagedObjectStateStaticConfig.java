/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.PersistentObjectFactory;

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
  TOOLKIT_TYPE_ROOT(ToolkitTypeNames.TOOLKIT_TYPE_ROOT_IMPL, Factory.TOOLKIT_TYPE_ROOT_TYPE_FACTORY),
  /**
   * Toolkit clusteredList type - reuses list object state
   */
  TOOLKIT_LIST(ToolkitTypeNames.TOOLKIT_LIST_IMPL, Factory.LIST_TYPE_FACTORY),
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
  TOOLKIT_OBJECT_STRIPE(ToolkitTypeNames.TOOLKIT_OBJECT_STRIPE_IMPL, Factory.TOOLKIT_OBJECT_STRIPE_TYPE_FACTORY),
  /**
   * ServerMap - explicit state factory
   */
  SERVER_MAP(ToolkitTypeNames.SERVER_MAP_TYPE, Factory.SERVER_MAP_TYPE_FACTORY),
  /**
   * ClusteredNotifier - explicit state factory
   */
  TOOLKIT_NOTIFIER(ToolkitTypeNames.TOOLKIT_NOTIFIER_TYPE, Factory.TOOLKIT_NOTIFIER_TYPE_FACTORY),
  /**
   * SerializedEntry - explicit state factory
   */
  SERIALIZED_MAP_VALUE(ToolkitTypeNames.SERIALIZED_MAP_VALUE_TYPE, Factory.SERIALIZED_CLUSTERED_OBJECT_FACTORY),

  /**
   * CustomLifespanSerializedEntry - explicit state factory
   */
  CUSTOM_LIFESPAN_SERIALIZED_MAP_VALUE(ToolkitTypeNames.CUSTOM_LIFESPAN_SERIALIZED_MAP_VALUE_TYPE,
      Factory.SERIALIZED_CLUSTERED_OBJECT_FACTORY),

  /**
   * Toolkit Map Type
   */
  TOOLKIT_MAP(ToolkitTypeNames.TOOLKIT_MAP_IMPL, Factory.MAP_TYPE_FACTORY),
  /**
   * Toolkit Sorted Map Type
   */
  TOOLKIT_SORTED_MAP(ToolkitTypeNames.TOOLKIT_SORTED_MAP_IMPL, Factory.MAP_TYPE_FACTORY);

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
    TOOLKIT_TYPE_ROOT_TYPE_FACTORY() {
      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.TOOLKIT_TYPE_ROOT_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException {
        return ToolkitTypeRootManagedObjectState.readFrom(objectInput, objectFactory);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new ToolkitTypeRootManagedObjectState(classId, oid, objectFactory);
      }

    },
    MAP_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.MAP_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory)
          throws IOException, ClassNotFoundException {
        return MapManagedObjectState.readFrom(objectInput, objectFactory);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new MapManagedObjectState(classId, oid, objectFactory);
      }

    },
    LIST_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.LIST_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException, ClassNotFoundException {
        return ListManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new ListManagedObjectState(classId);
      }

    },
    SERIALIZED_CLUSTERED_OBJECT_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException {
        return SerializedClusterObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new SerializedClusterObjectState(classId);
      }

    },
    TOOLKIT_OBJECT_STRIPE_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException, ClassNotFoundException {
        return ToolkitObjectStripeState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new ToolkitObjectStripeState(classId);
      }

    },
    SERVER_MAP_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException {
        return ConcurrentDistributedServerMapManagedObjectState.readFrom(objectInput, objectFactory);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new ConcurrentDistributedServerMapManagedObjectState(classId, oid, objectFactory);
      }
    },
    TOOLKIT_NOTIFIER_TYPE_FACTORY() {

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException {
        return ToolkitNotifierManagedObjectState.readFrom(objectInput);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new ToolkitNotifierManagedObjectState(classId);
      }
    },
    SET_TYPE_FACTORY() {

      @Override
      public byte getStateObjectType() {
        return ManagedObjectState.SET_TYPE;
      }

      @Override
      public ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException {
        return SetManagedObjectState.readFrom(objectInput, objectFactory);
      }

      @Override
      public ManagedObjectState newInstance(ObjectID oid, long classId,
                                            PersistentObjectFactory objectFactory) {
        return new SetManagedObjectState(classId, oid, objectFactory);
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

    public abstract ManagedObjectState readFrom(ObjectInput objectInput, PersistentObjectFactory objectFactory) throws IOException, ClassNotFoundException;

    public abstract ManagedObjectState newInstance(ObjectID oid, long classId,
                                                   PersistentObjectFactory objectFactory);

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

    public final static String TOOLKIT_TYPE_ROOT_IMPL                    = defineConstant("com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl");
    public final static String TOOLKIT_LIST_IMPL                         = defineConstant("com.terracotta.toolkit.collections.ToolkitListImpl");
    public final static String SERIALIZED_CLUSTERED_OBJECT_IMPL          = defineConstant("com.terracotta.toolkit.object.serialization.SerializedClusterObjectImpl");
    public final static String SERIALIZER_MAP_IMPL                       = defineConstant("com.terracotta.toolkit.object.serialization.SerializerMapImpl");
    public final static String TOOLKIT_OBJECT_STRIPE_IMPL                = defineConstant("com.terracotta.toolkit.object.ToolkitObjectStripeImpl");
    public final static String SERVER_MAP_TYPE                           = defineConstant("com.terracotta.toolkit.collections.map.ServerMap");
    public final static String TOOLKIT_NOTIFIER_TYPE                     = defineConstant("com.terracotta.toolkit.events.ToolkitNotifierImpl");
    public final static String SERIALIZED_MAP_VALUE_TYPE                 = defineConstant("com.terracotta.toolkit.object.serialization.SerializedMapValue");
    public final static String CUSTOM_LIFESPAN_SERIALIZED_MAP_VALUE_TYPE = defineConstant("com.terracotta.toolkit.object.serialization.CustomLifespanSerializedMapValue");
    public final static String TOOLKIT_MAP_IMPL                          = defineConstant("com.terracotta.toolkit.collections.map.ToolkitMapImpl");
    public final static String TOOLKIT_SORTED_MAP_IMPL                   = defineConstant("com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl");
  }
}
