/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Events;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ConcurrentDistributedServerMapManagedObjectState extends PartialMapManagedObjectState implements
    EvictableMap {

  private static final TCLogger LOGGER                         = TCLogging
                                                                   .getLogger(ConcurrentDistributedServerMapManagedObjectState.class);
  private static final boolean  ENABLE_DELETE_VALUE_ON_REMOVE  = TCPropertiesImpl
                                                                   .getProperties()
                                                                   .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_ENABLED,
                                                                               true);

  public static final String    CACHE_NAME_FIELDNAME           = "cacheName";
  public static final String    INVALIDATE_ON_CHANGE_FIELDNAME = "invalidateOnChange";
  public static final String    LOCK_TYPE_FIELDNAME            = "lockType";
  public static final String    LOCAL_CACHE_ENABLED_FIELDNAME  = "localCacheEnabled";
  public static final String    MAX_TTI_SECONDS_FIELDNAME      = "maxTTISeconds";
  public static final String    MAX_TTL_SECONDS_FIELDNAME      = "maxTTLSeconds";
  public static final String    MAX_COUNT_IN_CLUSTER_FIELDNAME = "maxCountInCluster";
  public static final String    COMPRESSION_ENABLED_FIELDNAME  = "compressionEnabled";
  public static final String    COPY_ON_READ_ENABLED_FIELDNAME = "copyOnReadEnabled";

  protected int                 dsoLockType;

  private static final double   OVERSHOOT                      = getOvershoot();

  static {
    LOGGER.info("Eviction overshoot threshold is " + OVERSHOOT);
  }

  enum EvictionStatus {
    NOT_INITIATED, INITIATED, SAMPLED
  }

  // This is a transient field tracking the status of the eviction for this CDSM
  private EvictionStatus evictionStatus = EvictionStatus.NOT_INITIATED;
  private Iterator<Object> evictionIterator = null;

  private boolean        invalidateOnChange;
  private int            maxTTISeconds;
  private int            maxTTLSeconds;
  private int            targetMaxTotalCount;
  private String         cacheName;
  private boolean        localCacheEnabled;
  private boolean        compressionEnabled;
  private boolean        copyOnReadEnabled;

  protected ConcurrentDistributedServerMapManagedObjectState(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in, factory);
    this.dsoLockType = in.readInt();
    this.maxTTISeconds = in.readInt();
    this.maxTTLSeconds = in.readInt();
    this.targetMaxTotalCount = in.readInt();
    this.invalidateOnChange = in.readBoolean();
    this.cacheName = in.readUTF();
    this.localCacheEnabled = in.readBoolean();
    this.compressionEnabled = in.readBoolean();
    this.copyOnReadEnabled = in.readBoolean();
  }

  protected ConcurrentDistributedServerMapManagedObjectState(final long classId, ObjectID id, PersistentObjectFactory factory) {
    super(classId, id, factory);
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType();
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    // Nothing to add since nothing is required to be faulted in the L1
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    if (type == DNAType.L2_SYNC) {
      // Write entire state info
      dehydrateFields(objectID, writer);
    super.dehydrate(objectID, writer, type);
    } else if (type == DNAType.L1_FAULT) {
      // Don't fault the references
      dehydrateFields(objectID, writer);
    }
  }

  protected void dehydrateFields(final ObjectID objectID, final DNAWriter writer) {
    writer.addPhysicalAction(LOCK_TYPE_FIELDNAME, dsoLockType);
    writer.addPhysicalAction(MAX_TTI_SECONDS_FIELDNAME, this.maxTTISeconds);
    writer.addPhysicalAction(MAX_TTL_SECONDS_FIELDNAME, this.maxTTLSeconds);
    writer.addPhysicalAction(MAX_COUNT_IN_CLUSTER_FIELDNAME, this.targetMaxTotalCount);
    writer.addPhysicalAction(INVALIDATE_ON_CHANGE_FIELDNAME, this.invalidateOnChange);
    writer.addPhysicalAction(CACHE_NAME_FIELDNAME, cacheName);
    writer.addPhysicalAction(LOCAL_CACHE_ENABLED_FIELDNAME, localCacheEnabled);
    writer.addPhysicalAction(COMPRESSION_ENABLED_FIELDNAME, compressionEnabled);
    writer.addPhysicalAction(COPY_ON_READ_ENABLED_FIELDNAME, copyOnReadEnabled);
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo)
      throws IOException {
    boolean broadcast = false;
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        applyPhysicalAction((PhysicalAction)action, objectID, applyInfo);
      } else { // LogicalAction
        // DEV-8737. Notify subscribers about the map mutation.
        getOperationEventBus().post(Events.operationCountIncrementEvent());

        final LogicalAction logicalAction = (LogicalAction)action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyLogicalAction(objectID, applyInfo, method, params);

        if (method == SerializationUtil.CLEAR || method == SerializationUtil.CLEAR_LOCAL_CACHE
            || method == SerializationUtil.DESTROY) {
          // clear needs to be broadcasted so local caches can be cleared elsewhere
          broadcast = true;
        }
      }
    }
    if (!broadcast) {
      applyInfo.ignoreBroadcastFor(objectID);
    }
  }

  @Override
  protected void applyPhysicalAction(final PhysicalAction action, final ObjectID objectID, final ApplyTransactionInfo info) {
    final String fieldName = action.getFieldName();
    if (fieldName.equals(LOCK_TYPE_FIELDNAME)) {
      this.dsoLockType = ((Integer)action.getObject());
    } else if (fieldName.equals(MAX_TTI_SECONDS_FIELDNAME)) {
      this.maxTTISeconds = ((Integer)action.getObject());
    } else if (fieldName.equals(MAX_TTL_SECONDS_FIELDNAME)) {
      this.maxTTLSeconds = ((Integer)action.getObject());
    } else if (fieldName.equals(MAX_COUNT_IN_CLUSTER_FIELDNAME)) {
      this.targetMaxTotalCount = ((Integer)action.getObject());
    } else if (fieldName.equals(INVALIDATE_ON_CHANGE_FIELDNAME)) {
      this.invalidateOnChange = ((Boolean)action.getObject());
    } else if (fieldName.equals(CACHE_NAME_FIELDNAME)) {
      Object value = action.getObject();
      String name;
      if (value instanceof UTF8ByteDataHolder) {
        name = ((UTF8ByteDataHolder)value).asString();
      } else {
        name = (String)value;
      }
      this.cacheName = name;
    } else if (fieldName.equals(LOCAL_CACHE_ENABLED_FIELDNAME)) {
      this.localCacheEnabled = (Boolean)action.getObject();
    } else if (COMPRESSION_ENABLED_FIELDNAME.equals(action.getFieldName())) {
      this.compressionEnabled = (Boolean)action.getObject();
    } else if (COPY_ON_READ_ENABLED_FIELDNAME.equals(action.getFieldName())) {
      this.copyOnReadEnabled = (Boolean)action.getObject();
    } else {
      throw new AssertionError("unexpected field name: " + fieldName);
    }
  }

  @Override
  protected void applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                                    final Object[] params) {
    switch (method) {
      case SerializationUtil.INT_FIELD_CHANGED:
        final String name;
        Object nameValue = params[0];
        int intValue = (Integer) params[1];
        if (nameValue instanceof UTF8ByteDataHolder) {
          name = ((UTF8ByteDataHolder) nameValue).asString();
        } else {
          name = (String) nameValue;
        }
        if (MAX_TTI_SECONDS_FIELDNAME.equals(name)) {
          this.maxTTISeconds = intValue;
        } else if (MAX_TTL_SECONDS_FIELDNAME.equals(name)) {
          this.maxTTLSeconds = intValue;
        } else if (MAX_COUNT_IN_CLUSTER_FIELDNAME.equals(name)) {
          this.targetMaxTotalCount = intValue;
        } else {
          throw new AssertionError("Unknown int field changed for oid: " + objectID + " - name: " + name + ", value: "
                                   + intValue);
        }
        break;
      case SerializationUtil.REMOVE_IF_VALUE_EQUAL:
        applyRemoveIfValueEqual(objectID, applyInfo, params);
        break;
      case SerializationUtil.PUT_IF_ABSENT:
        applyPutIfAbsent(objectID, applyInfo, params);
        break;
      case SerializationUtil.REPLACE_IF_VALUE_EQUAL:
        applyReplaceIfValueEqual(objectID, applyInfo, params);
        break;
      case SerializationUtil.REPLACE:
        applyReplace(objectID, applyInfo, params);
        break;
      case SerializationUtil.EVICTION_COMPLETED:
        evictionCompleted();
        break;
      case SerializationUtil.CLEAR_LOCAL_CACHE:
        break;
      case SerializationUtil.DESTROY:
        this.references.clear();
        break;
      default:
        super.applyLogicalAction(objectID, applyInfo, method, params);
    }
    if (applyInfo.isActiveTxn() && method == SerializationUtil.PUT && this.targetMaxTotalCount > 0
        && this.references.size() > this.targetMaxTotalCount * (1D + (OVERSHOOT / 100D))) {
        if ( startEviction() ) {
          applyInfo.initiateEvictionFor(objectID);
        }
    }
  }



  @Override
  protected void removedValueFromMap(final ObjectID mapID, ApplyTransactionInfo applyInfo, ObjectID old) {
    if (invalidateOnChange) {
      applyInfo.invalidate(mapID, old);
    }
    if (ENABLE_DELETE_VALUE_ON_REMOVE) {
      applyInfo.deleteObject(old);
    }
  }

  @Override
  protected void addKeyPresentForValue(ApplyTransactionInfo applyInfo, ObjectID value) {
    if (applyInfo.isSearchEnabled()) applyInfo.addKeyPresentForValue(value);
  }

  @Override
  protected void clearedMap(ApplyTransactionInfo applyInfo, Collection values) {
    // Does not need to be batched here since deletion batching will happen in the lower layers.
    if (ENABLE_DELETE_VALUE_ON_REMOVE) {
      for (Object o : values) {
        if (o instanceof ObjectID) {
          applyInfo.deleteObject((ObjectID) o);
        }
      }
    }
  }

  private void applyRemoveIfValueEqual(final ObjectID mapID, ApplyTransactionInfo applyInfo, final Object[] params) {
    final Object key = getKey(params);
    final Object value = getValue(params);
    final Object valueInMap = this.references.get(key);
    if (value.equals(valueInMap)) {
      this.references.remove(key);
      if (valueInMap instanceof ObjectID) {
        removedValueFromMap(mapID, applyInfo, (ObjectID) valueInMap);
      }
    }
  }

  private void applyReplaceIfValueEqual(final ObjectID mapID, ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = params[0];
    final Object current = params[1];
    final Object newValue = params[2];
    final Object valueInMap = this.references.get(key);
    if (current.equals(valueInMap)) {
      this.references.put(key, newValue);
      if (valueInMap instanceof ObjectID) {
        removedValueFromMap(mapID, applyInfo, (ObjectID) valueInMap);
      }
    } else if (newValue instanceof ObjectID) {
      // Invalidate the newValue so that the VM that initiated this call can remove it from the local cache.
      removedValueFromMap(mapID, applyInfo, (ObjectID) newValue);
    }
  }

  private void applyReplace(final ObjectID mapID, ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = params[0];
    final Object newValue = params[1];
    final Object valueInMap = this.references.get(key);
    if (valueInMap != null) {
      this.references.put(key, newValue);
    }
    if (valueInMap instanceof ObjectID) {
      removedValueFromMap(mapID, applyInfo, (ObjectID) valueInMap);
    }
  }

  private void applyPutIfAbsent(final ObjectID mapID, ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = getKey(params);
    final Object value = getValue(params);
    final Object valueInMap = this.references.get(key);
    if (valueInMap == null) {
      this.references.put(key, value);
    } else if (value instanceof ObjectID) {
      // Invalidate the value so that the VM that initiated this call can remove it from the local cache.
      removedValueFromMap(mapID, applyInfo, (ObjectID) value);
      addKeyPresentForValue(applyInfo, (ObjectID) value);
    }
  }

  @Override
  protected void basicWriteTo(final ObjectOutput out) throws IOException {
    super.basicWriteTo(out);
    out.writeInt(this.dsoLockType);
    out.writeInt(this.maxTTISeconds);
    out.writeInt(this.maxTTLSeconds);
    out.writeInt(this.targetMaxTotalCount);
    out.writeBoolean(this.invalidateOnChange);
    out.writeUTF(this.cacheName);
    out.writeBoolean(localCacheEnabled);
    out.writeBoolean(compressionEnabled);
    out.writeBoolean(copyOnReadEnabled);
  }

  public Object getValueForKey(final Object portableKey) {
    return this.references.get(portableKey);
  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    if (!(o instanceof ConcurrentDistributedServerMapManagedObjectState)) { return false; }
    final ConcurrentDistributedServerMapManagedObjectState mmo = (ConcurrentDistributedServerMapManagedObjectState) o;
    return super.basicEquals(o) && this.dsoLockType == mmo.dsoLockType && this.maxTTISeconds == mmo.maxTTISeconds
           && this.maxTTLSeconds == mmo.maxTTLSeconds && this.invalidateOnChange == mmo.invalidateOnChange
           && this.targetMaxTotalCount == mmo.targetMaxTotalCount && this.localCacheEnabled == mmo.localCacheEnabled
           && this.compressionEnabled == mmo.compressionEnabled && this.copyOnReadEnabled == mmo.copyOnReadEnabled;
  }

  static MapManagedObjectState readFrom(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    return new ConcurrentDistributedServerMapManagedObjectState(in, factory);
  }

  /****************************************************************************
   * EvictableMap interface
   */

  @Override
  public int getMaxTotalCount() {
    return this.targetMaxTotalCount;
  }

  @Override
  public int getSize() {
    return (int)this.references.size();
  }

  public Set getAllKeys() {
    return new HashSet(this.references.keySet());
  }

  @Override
  public int getTTISeconds() {
    return this.maxTTISeconds;
  }

  @Override
  public int getTTLSeconds() {
    return this.maxTTLSeconds;
  }
 //  locked by ManagedObject checkout 
  @Override
  public boolean startEviction() {
    if ( this.evictionStatus != EvictionStatus.NOT_INITIATED ) {
        return false;
    }
    this.evictionStatus = EvictionStatus.INITIATED;
    return true;
  }    
  
  @Override
  public boolean isEvicting() {
      return this.evictionStatus != EvictionStatus.NOT_INITIATED;
  }
 //  locked by ManagedObject checkout 
  @Override
  public void evictionCompleted() {
    this.evictionStatus = EvictionStatus.NOT_INITIATED;
  }

  @Override
  public Map<Object, ObjectID> getRandomSamples(final int count, final ClientObjectReferenceSet serverMapEvictionClientObjectRefSet) {
      if ( this.evictionStatus == EvictionStatus.NOT_INITIATED ) {
          throw new AssertionError(this.evictionStatus);
      } else {
 //     it's locked.  go for it
        this.evictionStatus = EvictionStatus.SAMPLED;
      }
    final Map<Object, ObjectID> samples = new HashMap<Object, ObjectID>(count);
    final Set<Object> ignored = new HashSet<Object>(count);
    final Random r = new Random();
    final int size = getSize();
    final int chance = count > size ? 100 : Math.max(10, (count / size) * 100);
    for (int i = 0; samples.size() < count && i < size; i++) {
      if (evictionIterator == null || !evictionIterator.hasNext()) {
        evictionIterator = references.keySet().iterator();
      }
      final Object k = evictionIterator.next();
      if ( k == null ) {
          throw new AssertionError("key is not null");
      }
      if (r.nextInt(100) < chance) {
        Object value = references.get(k);
        if (value == null || serverMapEvictionClientObjectRefSet.contains(value)) {
          continue;
        }
        samples.put(k, (ObjectID)value);
      } else {
        ignored.add(k);
      }
    }
    if (samples.size() < count) {
      for (final Iterator<Object> i = ignored.iterator(); samples.size() < count && i.hasNext();) {
        final Object k = i.next();
        final Object v = references.get(k);
        if (v == null || serverMapEvictionClientObjectRefSet.contains(v)) {
          continue;
        }
        samples.put(k, (ObjectID)v);
      }
    }
    return samples;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((cacheName == null) ? 0 : cacheName.hashCode());
//    result = prime * result + ((evictionStatus == null) ? 0 : evictionStatus.hashCode());
    result = prime * result + (invalidateOnChange ? 1231 : 1237);
    result = prime * result + (localCacheEnabled ? 1231 : 1237);
    result = prime * result + maxTTISeconds;
    result = prime * result + maxTTLSeconds;
    result = prime * result + targetMaxTotalCount;
    result = prime * result + (compressionEnabled ? 1231 : 1237);
    result = prime * result + (copyOnReadEnabled ? 1231 : 1237);
    return result;
  }

  public static void init() {
    // no-op for eager loading done at server startup (init constants in particular)
  }

  private static double getOvershoot() {
    final float MIN = 0;
    final float MAX = 100;

    float propVal = TCPropertiesImpl.getProperties()
        .getFloat(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT);

    if (propVal < MIN || propVal > MAX) {
      //
      throw new IllegalArgumentException("Invalid value for ["
                                         + TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT + "]: "
                                         + propVal + " (must be between " + MIN + " and " + MAX + ")");
    }

    return propVal;
  }
}
