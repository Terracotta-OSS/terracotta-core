/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

import java.util.Iterator;

public class L2ObjectSyncRequestHandler extends AbstractEventHandler {

  private static final TCLogger      logger                          = TCLogging
                                                                         .getLogger(L2ObjectSyncRequestHandler.class);
  private static final int           L2_OBJECT_SYNC_MESSAGE_MAXSIZE  = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_MAXSIZE_MB) * 1024 * 1024;
  private static final int           L2_OBJECT_SYNC_BATCH_SIZE       = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE);

  private static final int           MAX_L2_OBJECT_SYNC_BATCH_SIZE   = 5000;
  private static final int           MAX_L2_OBJECT_SYNC_MESSAGE_SIZE = 250 * 1024 * 1024;

  private final SequenceGenerator    sequenceGenerator;
  private final L2ObjectStateManager l2ObjectStateMgr;
  private Sink                       sendSink;
  private ObjectManager              objectManager;
  private GroupManager               groupManager;

  public L2ObjectSyncRequestHandler(final SequenceGenerator sequenceGenerator,
                                    final L2ObjectStateManager objectStateManager) {
    this.sequenceGenerator = sequenceGenerator;
    this.l2ObjectStateMgr = objectStateManager;

    if (L2_OBJECT_SYNC_BATCH_SIZE <= 0) {
      throw new AssertionError(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE
                               + " cant be less than or equal to zero.");
    } else if (L2_OBJECT_SYNC_BATCH_SIZE > MAX_L2_OBJECT_SYNC_BATCH_SIZE) {
      logger.warn(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE + " set too high : "
                  + L2_OBJECT_SYNC_BATCH_SIZE);
    }

    if (L2_OBJECT_SYNC_MESSAGE_MAXSIZE <= 0) {
      throw new AssertionError(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_MAXSIZE_MB
                               + " cant be negative or 0");
    } else if (L2_OBJECT_SYNC_MESSAGE_MAXSIZE > MAX_L2_OBJECT_SYNC_MESSAGE_SIZE) {
      logger.warn(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_MAXSIZE_MB + " set too high : "
                  + L2_OBJECT_SYNC_MESSAGE_MAXSIZE);
    }
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof SyncObjectsRequest) {
      doSyncObjectsRequest((SyncObjectsRequest) context);
    } else {
      throw new AssertionError("Unknown event context " + context);
    }
  }

  private void doSyncObjectsRequest(SyncObjectsRequest request) {
    NodeID nodeID = request.getNodeID();
    ManagedObjectSyncContext mosc = l2ObjectStateMgr.getSomeObjectsToSyncContext(nodeID, L2_OBJECT_SYNC_BATCH_SIZE);
    if (mosc != null) {
      doSyncObjectsDehydrate(mosc);
    }
  }

  private void doSyncObjectsDehydrate(ManagedObjectSyncContext context) {
    final ManagedObjectSyncContext mosc = context;
    ObjectIDSet oids = mosc.getRequestedObjectIDs();

    // Prefetch objects so they are there for us when we need it.
    objectManager.preFetchObjectsAndCreate(oids, TCCollections.EMPTY_OBJECT_ID_SET);

    try {
      /**
       * Note:: this sequence id is assigned before releasing any objects to ensure that transactions are not missed for
       * object in flight for PASSIVE-UNINITIALIED L2.
       */

      mosc.setSequenceID(this.sequenceGenerator.getNextSequence(mosc.getNodeID()));
    } catch (final SequenceGeneratorException e) {
      logger.error("Error generating a sequence number ", e);
      this.groupManager.zapNode(mosc.getNodeID(), L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                                "Error sending objects." + L2HAZapNodeRequestProcessor.getErrorString(e));
      return;
    }

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    final ObjectIDSet synced = new ObjectIDSet();
    final ObjectIDSet notSynced = new ObjectIDSet(oids);
    final Iterator<ObjectID> i = notSynced.iterator();
    for (; i.hasNext() && L2_OBJECT_SYNC_MESSAGE_MAXSIZE > (out.getBytesWritten() + serializer.getApproximateBytesWritten());) {
      ManagedObject m = null;
      try {
        ObjectID oid = i.next();
        i.remove();
        m = objectManager.getQuietObjectByID(oid);
        m.toDNA(out, serializer, DNAType.L2_SYNC);
        synced.add(oid);
      } finally {
        if (m != null) {
          this.objectManager.releaseReadOnly(m);
        }
      }
    }
    mosc.setDehydratedBytes(synced, notSynced, out.toArray(), synced.size(), serializer);
    this.sendSink.add(mosc);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
    final L2Coordinator l2Coordinator = oscc.getL2Coordinator();
    this.groupManager = l2Coordinator.getGroupManager();
  }
}
