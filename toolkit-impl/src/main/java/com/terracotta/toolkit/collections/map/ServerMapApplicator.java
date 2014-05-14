/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.TCObjectServerMap;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.locks.LockLevel;
import com.terracotta.toolkit.concurrent.locks.LockingUtils;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.object.DestroyApplicator;

import java.io.IOException;

public class ServerMapApplicator extends BaseApplicator {

  public static final String CACHE_NAME_FIELDNAME           = "cacheName";
  public static final String INVALIDATE_ON_CHANGE_FIELDNAME = "invalidateOnChange";
  public static final String LOCK_TYPE_FIELDNAME            = "lockType";
  public static final String LOCAL_CACHE_ENABLED_FIELDNAME  = "localCacheEnabled";
  public static final String MAX_TTI_SECONDS_FIELDNAME      = "maxTTISeconds";
  public static final String MAX_TTL_SECONDS_FIELDNAME      = "maxTTLSeconds";
  public static final String MAX_COUNT_IN_CLUSTER_FIELDNAME = "maxCountInCluster";
  public static final String EVICTION_ENABLED_FIELDNAME     = "evictionEnabled";
  public static final String COMPRESSION_ENABLED_FIELDNAME  = "compressionEnabled";
  public static final String COPY_ON_READ_ENABLED_FIELDNAME = "copyOnReadEnabled";

  public ServerMapApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tcObject, final DNAWriter writer,
                        final Object pojo) {
    InternalToolkitMap serverMap = (InternalToolkitMap) pojo;
    writer.addPhysicalAction(CACHE_NAME_FIELDNAME, serverMap.getName());
    writer.addPhysicalAction(INVALIDATE_ON_CHANGE_FIELDNAME, serverMap.invalidateOnChange());
    writer.addPhysicalAction(LOCK_TYPE_FIELDNAME, LockingUtils.translate(serverMap.getLockType()).toInt());
    writer.addPhysicalAction(LOCAL_CACHE_ENABLED_FIELDNAME, serverMap.isLocalCacheEnabled());
    writer.addPhysicalAction(MAX_TTI_SECONDS_FIELDNAME, serverMap.getMaxTTISeconds());
    writer.addPhysicalAction(MAX_TTL_SECONDS_FIELDNAME, serverMap.getMaxTTLSeconds());
    writer.addPhysicalAction(MAX_COUNT_IN_CLUSTER_FIELDNAME, serverMap.getMaxCountInCluster());
    writer.addPhysicalAction(EVICTION_ENABLED_FIELDNAME, serverMap.isEvictionEnabled());
    writer.addPhysicalAction(COMPRESSION_ENABLED_FIELDNAME, serverMap.isCompressionEnabled());
    writer.addPhysicalAction(COPY_ON_READ_ENABLED_FIELDNAME, serverMap.isCopyOnReadEnabled());
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna) {
    try {
      final DNACursor cursor = dna.getCursor();

      UnclusteredConfiguration config = new UnclusteredConfiguration();
      String name = null;
      ToolkitLockTypeInternal lockType = null;
      while (cursor.next(encoding)) {
        PhysicalAction physicalAction = cursor.getPhysicalAction();
        if (CACHE_NAME_FIELDNAME.equals(physicalAction.getFieldName())) {
          name = (String) physicalAction.getObject();
        } else if (INVALIDATE_ON_CHANGE_FIELDNAME.equals(physicalAction.getFieldName())) {
          // ignore this physical action
        } else if (LOCK_TYPE_FIELDNAME.equals(physicalAction.getFieldName())) {
          lockType = LockingUtils.translate(LockLevel.fromInt((Integer) physicalAction.getObject()));
        } else if (LOCAL_CACHE_ENABLED_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setBoolean(ToolkitConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME,
                            (Boolean) physicalAction.getObject());
        } else if (MAX_TTI_SECONDS_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setInt(ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME, (Integer) physicalAction.getObject());
        } else if (MAX_TTL_SECONDS_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setInt(ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME, (Integer) physicalAction.getObject());
        } else if (MAX_COUNT_IN_CLUSTER_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, (Integer) physicalAction.getObject());
        } else if (EVICTION_ENABLED_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setBoolean(ToolkitConfigFields.EVICTION_ENABLED_FIELD_NAME,
              (Boolean)physicalAction.getObject());
        } else if (COMPRESSION_ENABLED_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setBoolean(ToolkitConfigFields.COMPRESSION_ENABLED_FIELD_NAME,
              (Boolean) physicalAction.getObject());
        } else if (COPY_ON_READ_ENABLED_FIELDNAME.equals(physicalAction.getFieldName())) {
          config.setBoolean(ToolkitConfigFields.COPY_ON_READ_ENABLED_FIELD_NAME,
              (Boolean) physicalAction.getObject());
        } else {
          throw new AssertionError("Got unexpected physical action: " + physicalAction);
        }
      }

      Consistency consistency = null;
      if (lockType != null) {
        switch(lockType) {
          case CONCURRENT:
            consistency = Consistency.EVENTUAL;
            break;
          case SYNCHRONOUS_WRITE:
            consistency = Consistency.SYNCHRONOUS_STRONG;
            break;
          case WRITE:
            consistency = Consistency.STRONG;
            break;
          default:
            break;
        }
      }
      config.setString(ToolkitConfigFields.CONSISTENCY_FIELD_NAME, consistency.name());
      return new ServerMap(config, name);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return addTo;
  }

  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tcObjectExternal, final DNA dna,
                      final Object pojo) throws ClassNotFoundException, IOException {
    final DNACursor cursor = dna.getCursor();

    if (!dna.isDelta()) {
      // Shouldn't have anything to hydrate
      if (cursor.next(this.encoding)) { throw new AssertionError("ServerMap shouldn't fault in entry mappings : DNA "
                                                                 + dna); }
      return;
    }

    boolean receivedUnexpectedBroadcast = false;
    // Transactions could be folded, hence ignoring other changes.
    while (cursor.next(this.encoding)) {
      final LogicalAction action = cursor.getLogicalAction();
      final LogicalOperation method = action.getLogicalOperation();
      if (LogicalOperation.CLEAR.equals(method) || LogicalOperation.CLEAR_LOCAL_CACHE.equals(method)) {
        ((TCObjectServerMap) tcObjectExternal).clearLocalCache((TCServerMap) pojo);
      } else if (LogicalOperation.DESTROY.equals(method)) {
        ((DestroyApplicator) pojo).applyDestroy();
      } else if (LogicalOperation.SET_LAST_ACCESSED_TIME.equals(method)) {
        ((TCObjectServerMap) tcObjectExternal).removeValueFromLocalCache(action.getParameters()[0]);
      } else {
        getLogger().warn("ServerMap received delta changes for methods other than CLEAR : " + method);
        receivedUnexpectedBroadcast = true;
      }
    }
    if (receivedUnexpectedBroadcast) {
      // Can't assert here as resent transactions on server crash can be broadcasted as it might skip apply at the
      // server.
      // Resent transactions might need to be broadcasted.
      getLogger().warn("ServerMap shouldn't normally be broadcasting changes " +
                       "unless its a clear/destroy, but could be a resent txn after crash : DNA " + dna);
    }
  }
}
