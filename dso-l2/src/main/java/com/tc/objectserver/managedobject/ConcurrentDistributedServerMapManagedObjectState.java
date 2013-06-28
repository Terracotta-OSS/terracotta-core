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
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.impl.SamplingType;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.persistence.PersistentObjectFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEventType;
import com.tc.util.Events;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.tc.server.VersionedServerEvent.DEFAULT_VERSION;

public class ConcurrentDistributedServerMapManagedObjectState extends PartialMapManagedObjectState implements
    EvictableMap {

  private static final TCLogger LOGGER                         = TCLogging
                                                                   .getLogger(ConcurrentDistributedServerMapManagedObjectState.class);

  public static final String    CACHE_NAME_FIELDNAME           = "cacheName";
  public static final String    INVALIDATE_ON_CHANGE_FIELDNAME = "invalidateOnChange";
  public static final String    LOCK_TYPE_FIELDNAME            = "lockType";
  public static final String    LOCAL_CACHE_ENABLED_FIELDNAME  = "localCacheEnabled";
  public static final String    MAX_TTI_SECONDS_FIELDNAME      = "maxTTISeconds";
  public static final String    MAX_TTL_SECONDS_FIELDNAME      = "maxTTLSeconds";
  public static final String    MAX_COUNT_IN_CLUSTER_FIELDNAME = "maxCountInCluster";
  public static final String    COMPRESSION_ENABLED_FIELDNAME  = "compressionEnabled";
  public static final String    COPY_ON_READ_ENABLED_FIELDNAME = "copyOnReadEnabled";
  public static final String    EVICTION_ENABLED_FIELDNAME     = "evictionEnabled";

  protected int                 dsoLockType;

  private static final double   OVERSHOOT                      = getOvershoot();

  static {
    LOGGER.info("Eviction overshoot threshold is " + OVERSHOOT);
  }

  private SamplingType samplingType = SamplingType.FOR_EVICTION;
  private Iterator<Object> evictionIterator = null;

  private boolean        invalidateOnChange;
  private int            maxTTISeconds;
  private int            maxTTLSeconds;
  private int            targetMaxTotalCount;
  private String         cacheName;
  private boolean        evictionEnabled;
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
    this.evictionEnabled = in.readBoolean();
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
      dehydrateFields(writer);
      for (Object o : references.keySet()) {
        CDSMValue value = getValueForKey(o);
        writer.addLogicalAction(SerializationUtil.PUT, new Object[] {o, value.getObjectID(), value.getCreationTime(),
            value.getLastAccessedTime(), value.getTimeToIdle(), value.getTimeToLive()});
      }
    } else if (type == DNAType.L1_FAULT) {
      // Don't fault the references
      dehydrateFields(writer);
    }
  }

  protected void dehydrateFields(final DNAWriter writer) {
    writer.addPhysicalAction(LOCK_TYPE_FIELDNAME, this.dsoLockType);
    writer.addPhysicalAction(MAX_TTI_SECONDS_FIELDNAME, this.maxTTISeconds);
    writer.addPhysicalAction(MAX_TTL_SECONDS_FIELDNAME, this.maxTTLSeconds);
    writer.addPhysicalAction(MAX_COUNT_IN_CLUSTER_FIELDNAME, this.targetMaxTotalCount);
    writer.addPhysicalAction(INVALIDATE_ON_CHANGE_FIELDNAME, this.invalidateOnChange);
    writer.addPhysicalAction(CACHE_NAME_FIELDNAME, this.cacheName);
    writer.addPhysicalAction(LOCAL_CACHE_ENABLED_FIELDNAME, this.localCacheEnabled);
    writer.addPhysicalAction(COMPRESSION_ENABLED_FIELDNAME, this.compressionEnabled);
    writer.addPhysicalAction(COPY_ON_READ_ENABLED_FIELDNAME, this.copyOnReadEnabled);
    writer.addPhysicalAction(EVICTION_ENABLED_FIELDNAME, this.evictionEnabled);
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo)
      throws IOException {
    boolean broadcast = false;
    int eventCount = 0;
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        applyPhysicalAction((PhysicalAction)action, objectID, applyInfo);
      } else { // LogicalAction
        eventCount++;

        final LogicalAction logicalAction = (LogicalAction)action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyLogicalAction(objectID, applyInfo, method, params);

        //TODO: requires refactoring, we should call super.apply() instead
        if (method == SerializationUtil.CLEAR || method == SerializationUtil.CLEAR_LOCAL_CACHE
            || method == SerializationUtil.DESTROY || method == SerializationUtil.SET_LAST_ACCESSED_TIME) {
          // clear needs to be broadcasted so local caches can be cleared elsewhere
          broadcast = true;
        }
      }
    }
    if (eventCount != 0) {
      getOperationEventBus().post(Events.writeOperationCountChangeEvent(applyInfo.getServerTransactionID().getSourceID(), eventCount));
    }
    if (!broadcast) {
      applyInfo.ignoreBroadcastFor(objectID);
    }
  }

  @Override
  protected void applyPhysicalAction(final PhysicalAction action, final ObjectID objectID, final ApplyTransactionInfo info) {
    final String fieldName = action.getFieldName();
    if (LOCK_TYPE_FIELDNAME.equals(fieldName)) {
      this.dsoLockType = (Integer)action.getObject();
    } else if (MAX_TTI_SECONDS_FIELDNAME.equals(fieldName)) {
      this.maxTTISeconds = (Integer)action.getObject();
    } else if (MAX_TTL_SECONDS_FIELDNAME.equals(fieldName)) {
      this.maxTTLSeconds = (Integer)action.getObject();
    } else if (MAX_COUNT_IN_CLUSTER_FIELDNAME.equals(fieldName)) {
      this.targetMaxTotalCount = (Integer)action.getObject();
    } else if (INVALIDATE_ON_CHANGE_FIELDNAME.equals(fieldName)) {
      this.invalidateOnChange = (Boolean)action.getObject();
    } else if (fieldName.equals(CACHE_NAME_FIELDNAME)) {
      Object value = action.getObject();
      this.cacheName = asString(value);
    } else if (LOCAL_CACHE_ENABLED_FIELDNAME.equals(fieldName)) {
      this.localCacheEnabled = (Boolean)action.getObject();
    } else if (COMPRESSION_ENABLED_FIELDNAME.equals(fieldName)) {
      this.compressionEnabled = (Boolean)action.getObject();
    } else if (COPY_ON_READ_ENABLED_FIELDNAME.equals(fieldName)) {
      this.copyOnReadEnabled = (Boolean)action.getObject();
    } else if (EVICTION_ENABLED_FIELDNAME.equals(fieldName)) {
      this.evictionEnabled = (Boolean)action.getObject();
    } else {
      throw new AssertionError("unexpected field name: " + fieldName);
    }
  }

  @Override
  protected void applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo, final int method,
                                    final Object[] params) {
    switch (method) {
      case SerializationUtil.SET_LAST_ACCESSED_TIME:
        applySetLastAccessedTime(params);
        break;
      case SerializationUtil.FIELD_CHANGED:
        final String fieldName = asString(params[0]);
        final boolean boolValue = (Boolean) params[1];
        if (EVICTION_ENABLED_FIELDNAME.equals(fieldName)) {
          this.evictionEnabled = boolValue;
        }
        break;
      case SerializationUtil.INT_FIELD_CHANGED:
        final String intFieldName = asString(params[0]);
        final int intValue = (Integer) params[1];
        if (MAX_TTI_SECONDS_FIELDNAME.equals(intFieldName)) {
          this.maxTTISeconds = intValue;
        } else if (MAX_TTL_SECONDS_FIELDNAME.equals(intFieldName)) {
          this.maxTTLSeconds = intValue;
        } else if (MAX_COUNT_IN_CLUSTER_FIELDNAME.equals(intFieldName)) {
          this.targetMaxTotalCount = intValue;
        } else {
          throw new AssertionError("Unknown int field changed for oid: " + objectID + " - name: " + intFieldName + ", value: "
                                   + intValue);
        }
        break;
      case SerializationUtil.REMOVE_IF_VALUE_EQUAL:
        applyRemoveIfValueEqual(applyInfo, params);
        break;
      case SerializationUtil.EXPIRE_IF_VALUE_EQUAL:
        applyExpireIfValueEqual(applyInfo, params);
        break;
      case SerializationUtil.PUT_IF_ABSENT:
        applyPutIfAbsent(applyInfo, params);
        break;
      case SerializationUtil.PUT_VERSIONED:
        applyPutVersioned(applyInfo, params);
        break;
      case SerializationUtil.REMOVE_VERSIONED:
        applyRemoveVersioned(applyInfo, params);
        break;
      case SerializationUtil.REPLACE_IF_VALUE_EQUAL:
        applyReplaceIfEqualWithExpiry(applyInfo, params);
        break;
      case SerializationUtil.REPLACE:
        applyReplace(applyInfo, params);
        break;
      case SerializationUtil.EVICTION_COMPLETED:
        applyInfo.getServerEventRecorder().reconsiderRemovals(samplingType);
        evictionCompleted();
        // make sure we don't need more capacity eviction to get to target
        startCapacityEvictionIfNeccessary(applyInfo);
       break;
      case SerializationUtil.CLEAR_LOCAL_CACHE:
        break;
      default:
        super.applyLogicalAction(objectID, applyInfo, method, params);
    }
  }

  private static String asString(final Object value) {
    final String str;
    if (value instanceof UTF8ByteDataHolder) {
      str = ((UTF8ByteDataHolder) value).asString();
    } else {
      str = (String) value;
    }
    return str;
  }

  @Override
  protected void addedReference(final ApplyTransactionInfo applyInfo, final Object o) {
    if (o instanceof CDSMValue) {
      super.addedReference(applyInfo, ((CDSMValue)o).getObjectID());
    } else {
      super.addedReference(applyInfo, o);
    }
  }

  @Override
  protected void removedReference(final ApplyTransactionInfo applyInfo, final Object o) {
    Object ref = o;
    if (o instanceof CDSMValue) {
      ref = ((CDSMValue)o).getObjectID();
    }
    if (ref instanceof ObjectID) {
      applyInfo.deleteObject((ObjectID)ref);
      if (invalidateOnChange) {
        applyInfo.invalidate(getId(), (ObjectID) ref);
      }
    }
  }

  protected void addValue(ApplyTransactionInfo applyInfo, Object value, boolean keyExists) {
    if (applyInfo.isSearchEnabled() && value instanceof ObjectID) applyInfo.recordValue((ObjectID)value, keyExists);
  }

  @Override
  protected Object applyPut(final ApplyTransactionInfo applyInfo, final Object[] params) {
    final Object key = params[0];
    final ObjectID oid = (ObjectID)params[1];

    final CDSMValue value;
    if (params.length == 6) {
      value = new CDSMValue(oid, (Long) params[2], (Long) params[3], (Long) params[4], (Long) params[5]);
    } else {
      value = new CDSMValue(oid, 0, 0, 0, 0);
    }

    final CDSMValue old = (CDSMValue) references.get(key);
    final long newVersion = (old == null) ? 0 // new key-value pair
        : old.getVersion() + 1; // local put - increment version, if key already exists
    value.setVersion(newVersion);

    applyPutInternal(applyInfo, params, value, old);

    // collect modifications for futher broadcasting
    applyInfo.getServerEventRecorder().recordEvent(ServerEventType.PUT_LOCAL, key, oid, value.getVersion(), cacheName);
    return old;
  }

  private Object applyPutVersioned(ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = params[0];
    final ObjectID oid = (ObjectID)params[1];
    // put came from the WAN orchestrator - obediently apply new version
    final CDSMValue value = new CDSMValue(oid, (Long) params[2], (Long) params[3],
        (Long) params[4], (Long) params[5], (Long) params[6]);
    final CDSMValue old = (CDSMValue)references.get(key);

    applyPutInternal(applyInfo, params, value, old);
    return old;
  }

  private void applyPutInternal(final ApplyTransactionInfo applyInfo, final Object[] params, final CDSMValue value, final CDSMValue old) {
    final Object key = params[0];
    final ObjectID oid = (ObjectID)params[1];

    references.put(key, value);
    addedReferences(applyInfo, key, value);
    removedReferences(applyInfo, old);

    addValue(applyInfo, oid, old != null);
    startCapacityEvictionIfNeccessary(applyInfo);

    // collect modifications for futher broadcasting
    applyInfo.getServerEventRecorder().recordEvent(ServerEventType.PUT, key, oid, cacheName);
  }

  private boolean startCapacityEvictionIfNeccessary(final ApplyTransactionInfo applyInfo) {
    if (applyInfo.isActiveTxn()
        && this.evictionEnabled  //  do not trigger if eviction is disabled
        && this.targetMaxTotalCount >= 0 // do not trigger capacity eviction if totalMaxCount is negative
        && this.references.size() > this.targetMaxTotalCount * (1 + (OVERSHOOT / 100))) {
        applyInfo.initiateEvictionFor(getId());
        return true;
    }
    return false;
  }

  private void applyPutIfAbsent(ApplyTransactionInfo applyInfo, Object[] params) {
    Object key = params[0];
    Object value = params[1];
    if (!references.containsKey(key)) {
      applyPut(applyInfo, params);
    } else {
      removedReferences(applyInfo, value);
      addValue(applyInfo, value, false);
    }
  }

  private void applyReplace(ApplyTransactionInfo applyInfo, Object[] params) {
    Object key = params[0];
    Object value = params[1];
    if (references.containsKey(key)) {
      applyPut(applyInfo, params);
    } else {
      removedReferences(applyInfo, value);
      addValue(applyInfo, value, false);
    }
  }

  private void applyReplaceIfEqualWithExpiry(ApplyTransactionInfo applyInfo, Object[] params) {
    Object key = params[0];
    Object currentValue = params[1];
    Object newValue = params[2];
    CDSMValue old = getValueForKey(key);
    if (old != null && old.getObjectID().equals(currentValue)) {
      if (params.length == 7) {
        applyPut(applyInfo, new Object[] { key, newValue, params[3], params[4], params[5], params[6]});
      } else {
        applyPut(applyInfo, new Object[]{ key, newValue });
      }
    } else {
      removedReferences(applyInfo, newValue);
    }
  }

  private void applyRemoveIfValueEqual(ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = params[0];
    final Object value = params[1];
    final CDSMValue valueInMap = getValueForKey(key);
    if (valueInMap != null && value.equals(valueInMap.getObjectID())) {
      references.remove(key);
      removedReferences(applyInfo, value);
      final ObjectID objectId = (ObjectID)value;

      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.REMOVE, key, objectId, cacheName);
      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.REMOVE_LOCAL, key, objectId, valueInMap.getVersion(), cacheName);
    }
  }

  private void applyExpireIfValueEqual(ApplyTransactionInfo applyInfo, Object[] params) {
    final Object key = params[0];
    final Object value = params[1];
    final CDSMValue valueInMap = getValueForKey(key);
    if (valueInMap != null && value.equals(valueInMap.getObjectID())) {
      references.remove(key);
      removedReferences(applyInfo, value);
      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.EXPIRE, key, (ObjectID)value, cacheName);
    }
  }

  @Override
  protected Object applyRemove(final ApplyTransactionInfo applyInfo, final Object[] params) {
    final Object key = params[0];
    final Object old = super.applyRemove(applyInfo, params);
    if (old instanceof CDSMValue) {
      final CDSMValue oldValue = (CDSMValue)old;
      final ObjectID objectId = oldValue.getObjectID();

      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.REMOVE, key, objectId, cacheName);
      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.REMOVE_LOCAL, key, objectId, oldValue.getVersion(), cacheName);
    }
    return old;
  }

  private Object applyRemoveVersioned(final ApplyTransactionInfo applyInfo, final Object[] params) {
    final Object key = params[0];
    final long version = (Long)params[1]; // ignoring for the time being

    final Object old = super.applyRemove(applyInfo, params);
    if (old instanceof CDSMValue) {
      final ObjectID objectId = ((CDSMValue)old).getObjectID();
      applyInfo.getServerEventRecorder().recordEvent(ServerEventType.REMOVE, key, objectId, cacheName);
    }
    return old;
  }

  private void applySetLastAccessedTime(Object[] params) {
    Object key = params[0];
    Object value = params[1];
    long lastAccessedTime = (Long) params[2];
    CDSMValue wrappedValue = getValueForKey(key);
    if (wrappedValue != null && value.equals(wrappedValue.getObjectID())) {
      wrappedValue.setLastAccessedTime(lastAccessedTime);
      references.put(key, wrappedValue);
    }
  }

  @Override
  protected void applyClear(final ApplyTransactionInfo applyInfo) {
    removedReferences(applyInfo, references.values());
    references.clear();
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
    out.writeBoolean(evictionEnabled);
  }

  public CDSMValue getValueForKey(final Object portableKey) {
    return (CDSMValue) this.references.get(portableKey);
  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    if (!(o instanceof ConcurrentDistributedServerMapManagedObjectState)) { return false; }
    final ConcurrentDistributedServerMapManagedObjectState mmo = (ConcurrentDistributedServerMapManagedObjectState) o;
    return super.basicEquals(o) && this.dsoLockType == mmo.dsoLockType && this.maxTTISeconds == mmo.maxTTISeconds
           && this.maxTTLSeconds == mmo.maxTTLSeconds && this.invalidateOnChange == mmo.invalidateOnChange
           && this.targetMaxTotalCount == mmo.targetMaxTotalCount && this.localCacheEnabled == mmo.localCacheEnabled
           && this.compressionEnabled == mmo.compressionEnabled && this.copyOnReadEnabled == mmo.copyOnReadEnabled
           && this.evictionEnabled == mmo.evictionEnabled;
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
  public boolean isEvictionEnabled() {
    return evictionEnabled;
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
    if ( !this.evictionEnabled && this.maxTTISeconds == 0 && this.maxTTLSeconds == 0 ) {
      return false;
    }
    return true;
  }

 //  locked by ManagedObject checkout
  @Override
  public void evictionCompleted() {

  }

  @Override
  protected void addAllObjectReferencesTo(final Set refs) {
    // CDSM doesn't support object keys.
    for (Object o : references.values()) {
      CDSMValue value = (CDSMValue) o;
      refs.add(value.getObjectID());
    }
  }

  @Override
  public Map<Object, EvictableEntry> getRandomSamples(final int count,
                                                      final ClientObjectReferenceSet clientObjectRefSet,
                                                      final SamplingType smpType) {
    this.samplingType = smpType;
    final Map<Object, EvictableEntry> samples = new HashMap<Object, EvictableEntry>(count);
    final Set<Object> ignored = new HashSet<Object>(count);
    final Random r = new Random();
    final int size = getSize();
    final int chance = count > size ? 100 : Math.max(10, (count / size) * 100);
    for (int i = 0; samples.size() < count && i < size; i++) {
      if (evictionIterator == null || !evictionIterator.hasNext()) {
        evictionIterator = references.keySet().iterator();
      }
      final Object k = evictionIterator.next();
      if (k == null) {
        throw new AssertionError("key is not null");
      }
      if (r.nextInt(100) < chance) {
        CDSMValue value = getValueForKey(k);
        if (value == null || clientObjectRefSet.contains(value.getObjectID())) {
          continue;
        }
        samples.put(k, value);
      } else {
        ignored.add(k);
      }
    }
    if (samples.size() < count) {
      for (final Iterator<Object> i = ignored.iterator(); samples.size() < count && i.hasNext(); ) {
        final Object k = i.next();
        CDSMValue v = getValueForKey(k);
        if (v == null || clientObjectRefSet.contains(v.getObjectID())) {
          continue;
        }
        samples.put(k, v);
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
    result = prime * result + (invalidateOnChange ? 1231 : 1237);
    result = prime * result + (localCacheEnabled ? 1231 : 1237);
    result = prime * result + maxTTISeconds;
    result = prime * result + maxTTLSeconds;
    result = prime * result + targetMaxTotalCount;
    result = prime * result + (compressionEnabled ? 1231 : 1237);
    result = prime * result + (copyOnReadEnabled ? 1231 : 1237);
    result = prime * result + (evictionEnabled ? 1231 : 1237);
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
      throw new IllegalArgumentException("Invalid value for ["
                                         + TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT + "]: "
                                         + propVal + " (must be between " + MIN + " and " + MAX + ")");
    }

    return propVal;
  }
}
