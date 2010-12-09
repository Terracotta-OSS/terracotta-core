/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.api.EvictableMap;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;

public class ConcurrentDistributedServerMapManagedObjectState extends ConcurrentDistributedMapManagedObjectState
    implements EvictableMap {

  public static final String MAX_TTI_SECONDS_FIELDNAME            = "maxTTISeconds";
  public static final String MAX_TTL_SECONDS_FIELDNAME            = "maxTTLSeconds";
  public static final String TARGET_MAX_IN_MEMORY_COUNT_FIELDNAME = "targetMaxInMemoryCount";
  public static final String TARGET_MAX_TOTAL_COUNT_FIELDNAME     = "targetMaxTotalCount";
  public static final String INVALIDATE_ON_CHANGE                 = "invalidateOnChange";

  private boolean            evictionInitiated                    = false;
  private boolean            invalidateOnChange;
  private int                maxTTISeconds;
  private int                maxTTLSeconds;
  private int                targetMaxInMemoryCount;
  private int                targetMaxTotalCount;

  protected ConcurrentDistributedServerMapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
    this.maxTTISeconds = in.readInt();
    this.maxTTLSeconds = in.readInt();
    this.targetMaxInMemoryCount = in.readInt();
    this.targetMaxTotalCount = in.readInt();
    this.invalidateOnChange = in.readBoolean();
  }

  protected ConcurrentDistributedServerMapManagedObjectState(final long classId, final Map map) {
    super(classId, map);
  }

  @Override
  public byte getType() {
    return CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE;
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    // Nothing to add since nothing is required to be faulted in the L1
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    if (type == DNAType.L2_SYNC) {
      // Write entire state info
      super.dehydrate(objectID, writer, type);
    } else if (type == DNAType.L1_FAULT) {
      // Don't fault the references
      dehydrateFields(objectID, writer);
    }
  }

  @Override
  protected void dehydrateFields(final ObjectID objectID, final DNAWriter writer) {
    super.dehydrateFields(objectID, writer);
    writer.addPhysicalAction(MAX_TTI_SECONDS_FIELDNAME, Integer.valueOf(this.maxTTISeconds));
    writer.addPhysicalAction(MAX_TTL_SECONDS_FIELDNAME, Integer.valueOf(this.maxTTLSeconds));
    writer.addPhysicalAction(TARGET_MAX_IN_MEMORY_COUNT_FIELDNAME, Integer.valueOf(this.targetMaxInMemoryCount));
    writer.addPhysicalAction(TARGET_MAX_TOTAL_COUNT_FIELDNAME, Integer.valueOf(this.targetMaxTotalCount));
    writer.addPhysicalAction(INVALIDATE_ON_CHANGE, Boolean.valueOf(this.invalidateOnChange));
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
        if (fieldName.equals(DSO_LOCK_TYPE_FIELDNAME)) {
          this.dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(LOCK_STRATEGY_FIELDNAME)) {
          final ObjectID newLockStrategy = (ObjectID) physicalAction.getObject();
          getListener().changed(objectID, this.lockStrategy, newLockStrategy);
          this.lockStrategy = newLockStrategy;
        } else if (fieldName.equals(MAX_TTI_SECONDS_FIELDNAME)) {
          this.maxTTISeconds = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(MAX_TTL_SECONDS_FIELDNAME)) {
          this.maxTTLSeconds = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(TARGET_MAX_IN_MEMORY_COUNT_FIELDNAME)) {
          this.targetMaxInMemoryCount = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(TARGET_MAX_TOTAL_COUNT_FIELDNAME)) {
          this.targetMaxTotalCount = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(INVALIDATE_ON_CHANGE)) {
          this.invalidateOnChange = ((Boolean) physicalAction.getObject());
        } else {
          throw new AssertionError("unexpected field name: " + fieldName);
        }
      } else {
        final LogicalAction logicalAction = (LogicalAction) action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyMethod(objectID, applyInfo, method, params);
        if (method == SerializationUtil.CLEAR || method == SerializationUtil.CLEAR_LOCAL_CACHE) {
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
    if (method == SerializationUtil.REMOVE_IF_VALUE_EQUAL) {
      // This is currently used by evictor, in future we may support all of ConcurrentMap operations at the server
      // directly
      applyRemoveIfValueEqual(params);
    } else if (method != SerializationUtil.CLEAR_LOCAL_CACHE) {
      // ignore CLEAR_LOCAL_CACHE, nothing to do, but broadcast
      super.applyMethod(objectID, applyInfo, method, params);
    }
    if (applyInfo.isActiveTxn() && method == SerializationUtil.PUT && this.targetMaxTotalCount > 0
        && !this.evictionInitiated && this.references.size() > this.targetMaxTotalCount * 1.15) { // 15 % overshoot
      this.evictionInitiated = true;
      applyInfo.initiateEvictionFor(objectID);
    }
  }

  private void applyRemoveIfValueEqual(final Object[] params) {
    final Object key = getKey(params);
    final Object value = getValue(params);
    final Object valueInMap = this.references.get(key);
    if (value.equals(valueInMap)) {
      this.references.remove(key);
    }
  }

  @Override
  protected void basicWriteTo(final ObjectOutput out) throws IOException {
    super.basicWriteTo(out);
    out.writeInt(this.maxTTISeconds);
    out.writeInt(this.maxTTLSeconds);
    out.writeInt(this.targetMaxInMemoryCount);
    out.writeInt(this.targetMaxTotalCount);
    out.writeBoolean(this.invalidateOnChange);
  }

  public Object getValueForKey(final Object portableKey) {
    return this.references.get(portableKey);
  }

  @Override
  protected boolean basicEquals(final LogicalManagedObjectState o) {
    if (!(o instanceof ConcurrentDistributedServerMapManagedObjectState)) { return false; }
    final ConcurrentDistributedServerMapManagedObjectState mmo = (ConcurrentDistributedServerMapManagedObjectState) o;
    return super.basicEquals(o) && this.maxTTISeconds == mmo.maxTTISeconds && this.maxTTLSeconds == mmo.maxTTLSeconds
           && this.targetMaxInMemoryCount == mmo.targetMaxInMemoryCount
           && this.invalidateOnChange == mmo.invalidateOnChange && this.targetMaxTotalCount == mmo.targetMaxTotalCount;
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

  // TODO:: This implementation could be better, could use LinkedHashMap to increase the chances of getting the
  // right samples, also should it return a sorted Map ? Are objects with lower OIDs having more changes to be evicted ?
  public Map getRandomSamples(final int count, final SortedSet<ObjectID> ignoreList) {
    // TODO::May be delay this setting to later, in evict, but have to be careful not to miss setting it.
    this.evictionInitiated = false;
    final Map samples = new HashMap(count);
    final Map ignored = new HashMap(count);
    final Random r = new Random();
    final int size = getSize();
    final int chance = count > size ? 100 : Math.max(10, (count / size) * 100);
    for (final Iterator i = this.references.entrySet().iterator(); samples.size() < count && i.hasNext();) {
      final Entry e = (Entry) i.next();
      if (ignoreList.contains(e.getValue())) {
        continue;
      }
      if (r.nextInt(100) < chance) {
        samples.put(e.getKey(), e.getValue());
      } else {
        ignored.put(e.getKey(), e.getValue());
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
}
