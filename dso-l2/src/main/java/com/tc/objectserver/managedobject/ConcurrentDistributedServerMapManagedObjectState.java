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
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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

  private boolean        invalidateOnChange;
  private int            maxTTISeconds;
  private int            maxTTLSeconds;
  private int            targetMaxTotalCount;
  private String         cacheName;
  private boolean        localCacheEnabled;

  protected ConcurrentDistributedServerMapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
    this.dsoLockType = in.readInt();
    this.maxTTISeconds = in.readInt();
    this.maxTTLSeconds = in.readInt();
    this.targetMaxTotalCount = in.readInt();
    this.invalidateOnChange = in.readBoolean();
    this.cacheName = in.readUTF();
    this.localCacheEnabled = in.readBoolean();
  }

  protected ConcurrentDistributedServerMapManagedObjectState(final long classId, final Map map) {
    super(classId, map);
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
    writer.addPhysicalAction(MAX_TTI_SECONDS_FIELDNAME, Integer.valueOf(this.maxTTISeconds));
    writer.addPhysicalAction(MAX_TTL_SECONDS_FIELDNAME, Integer.valueOf(this.maxTTLSeconds));
    writer.addPhysicalAction(MAX_COUNT_IN_CLUSTER_FIELDNAME, Integer.valueOf(this.targetMaxTotalCount));
    writer.addPhysicalAction(INVALIDATE_ON_CHANGE_FIELDNAME, Boolean.valueOf(this.invalidateOnChange));
    writer.addPhysicalAction(CACHE_NAME_FIELDNAME, cacheName);
    writer.addPhysicalAction(LOCAL_CACHE_ENABLED_FIELDNAME, localCacheEnabled);
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo)
      throws IOException {
    boolean broadcast = false;
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction physicalAction = (PhysicalAction) action;

        final String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(LOCK_TYPE_FIELDNAME)) {
          this.dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(MAX_TTI_SECONDS_FIELDNAME)) {
          this.maxTTISeconds = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(MAX_TTL_SECONDS_FIELDNAME)) {
          this.maxTTLSeconds = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(MAX_COUNT_IN_CLUSTER_FIELDNAME)) {
          this.targetMaxTotalCount = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(INVALIDATE_ON_CHANGE_FIELDNAME)) {
          this.invalidateOnChange = ((Boolean) physicalAction.getObject());
        } else if (fieldName.equals(CACHE_NAME_FIELDNAME)) {
          Object value = physicalAction.getObject();
          String name;
          if (value instanceof UTF8ByteDataHolder) {
            name = ((UTF8ByteDataHolder) value).asString();
          } else {
            name = (String) value;
          }
          this.cacheName = name;
        } else if (fieldName.equals(LOCAL_CACHE_ENABLED_FIELDNAME)) {
          this.localCacheEnabled = (Boolean) physicalAction.getObject();
        } else {
          throw new AssertionError("unexpected field name: " + fieldName);
        }
      } else {
        final LogicalAction logicalAction = (LogicalAction) action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyMethod(objectID, applyInfo, method, params);
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
  protected void applyMethod(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                             final Object[] params) {
    switch (method) {
      case SerializationUtil.SET_MAX_TTI:
        this.maxTTISeconds = (Integer) params[0];
        break;
      case SerializationUtil.SET_MAX_TTL:
        this.maxTTLSeconds = (Integer) params[0];
        break;
      case SerializationUtil.SET_TARGET_MAX_TOTAL_COUNT:
        this.targetMaxTotalCount = (Integer) params[0];
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
      case SerializationUtil.EVICTION_COMPLETED:
        evictionCompleted();
        break;
      case SerializationUtil.CLEAR_LOCAL_CACHE:
        break;
      case SerializationUtil.DESTROY:
        this.references.clear();
        break;
      default:
        super.applyMethod(objectID, applyInfo, method, params);
    }
    if (applyInfo.isActiveTxn() && method == SerializationUtil.PUT && this.targetMaxTotalCount > 0
        && this.evictionStatus == EvictionStatus.NOT_INITIATED
        && this.references.size() > this.targetMaxTotalCount * (1D + (OVERSHOOT / 100D))) {
      this.evictionStatus = EvictionStatus.INITIATED;
      applyInfo.initiateEvictionFor(objectID);
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

  private void applyPutIfAbsent(final ObjectID mapID, ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = getKey(params);
    final Object value = getValue(params);
    final Object valueInMap = this.references.get(key);
    if (valueInMap == null) {
      this.references.put(key, value);
    } else if (value instanceof ObjectID) {
      // Invalidate the value so that the VM that initiated this call can remove it from the local cache.
      removedValueFromMap(mapID, applyInfo, (ObjectID) value);
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
           && this.targetMaxTotalCount == mmo.targetMaxTotalCount && this.localCacheEnabled == mmo.localCacheEnabled;
  }

  static MapManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final ConcurrentDistributedServerMapManagedObjectState cdmMos = new ConcurrentDistributedServerMapManagedObjectState(
                                                                                                                         in);
    return cdmMos;
  }

  /****************************************************************************
   * EvictableMap interface
   */

  public int getMaxTotalCount() {
    return this.targetMaxTotalCount;
  }

  public int getSize() {
    return this.references.size();
  }

  public Set getAllKeys() {
    return new HashSet(this.references.keySet());
  }

  public int getTTISeconds() {
    return this.maxTTISeconds;
  }

  public int getTTLSeconds() {
    return this.maxTTLSeconds;
  }

  public void evictionCompleted() {
    this.evictionStatus = EvictionStatus.NOT_INITIATED;
  }

  // TODO:: This implementation could be better, could use LinkedHashMap to increase the chances of getting the
  // right samples, also should it return a sorted Map ? Are objects with lower OIDs having more changes to be evicted ?
  public Map getRandomSamples(final int count, final ClientObjectReferenceSet serverMapEvictionClientObjectRefSet) {
    if (evictionStatus == EvictionStatus.SAMPLED) {
      // There is already a random sample that is yet to be processed, so returning empty collection. This can happen if
      // both period and capacity Evictors are working at the same object one after the other.
      return Collections.EMPTY_MAP;
    }
    this.evictionStatus = EvictionStatus.SAMPLED;
    final Map samples = new HashMap(count);
    final Map ignored = new HashMap(count);
    final Random r = new Random();
    final int size = getSize();
    final int chance = count > size ? 100 : Math.max(10, (count / size) * 100);
    for (final Iterator i = this.references.entrySet().iterator(); samples.size() < count && i.hasNext();) {
      final Entry e = (Entry) i.next();
      Object value = e.getValue();
      if (serverMapEvictionClientObjectRefSet.contains(value)) {
        continue;
      }
      if (r.nextInt(100) < chance) {
        samples.put(e.getKey(), value);
      } else {
        ignored.put(e.getKey(), value);
      }
    }
    if (samples.size() < count) {
      for (final Iterator i = ignored.entrySet().iterator(); samples.size() < count && i.hasNext();) {
        final Entry e = (Entry) i.next();
        samples.put(e.getKey(), e.getValue());
      }
    }
    return samples;
  }

  public String getCacheName() {
    return cacheName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((cacheName == null) ? 0 : cacheName.hashCode());
    result = prime * result + ((evictionStatus == null) ? 0 : evictionStatus.hashCode());
    result = prime * result + (invalidateOnChange ? 1231 : 1237);
    result = prime * result + (localCacheEnabled ? 1231 : 1237);
    result = prime * result + maxTTISeconds;
    result = prime * result + maxTTLSeconds;
    result = prime * result + targetMaxTotalCount;
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
