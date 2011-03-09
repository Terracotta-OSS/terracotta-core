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
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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

public class L2ObjectSyncDehydrateHandler extends AbstractEventHandler {

  private static final TCLogger   logger                      = TCLogging.getLogger(L2ObjectSyncDehydrateHandler.class);
  private static final int        L2_OBJECT_SYNC_MESSAGE_SIZE = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_SIZE_MB) * 1024 * 1024;

  private final SequenceGenerator sequenceGenerator;

  private Sink                    sendSink;
  private ObjectManager           objectManager;
  private GroupManager            groupManager;

  public L2ObjectSyncDehydrateHandler(final SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = sequenceGenerator;
    if (L2_OBJECT_SYNC_MESSAGE_SIZE <= 0) { throw new AssertionError(
                                                                     TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_SIZE_MB
                                                                         + " cant be negative or 0"); }
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;

    ObjectIDSet oids = mosc.getRequestedObjectIDs();

    // Prefetch objects so they are there for us when we need it.
    objectManager.preFetchObjectsAndCreate(oids, TCCollections.EMPTY_OBJECT_ID_SET);

    // XXX::Note:: this sequence id is assigned before releasing any objects to ensure that transactions are not missed
    // for object in flight for PASSIVE-UNINITIALIED L2.
    try {
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
    final ObjectIDSet notSynced = new ObjectIDSet();
    final Iterator i = oids.iterator();
    for (; i.hasNext() && L2_OBJECT_SYNC_MESSAGE_SIZE > out.getBytesWritten();) {
      ManagedObject m = null;
      try {
        ObjectID oid = (ObjectID) i.next();
        m = objectManager.getQuietObjectByID(oid);
        m.toDNA(out, serializer, DNAType.L2_SYNC);
        synced.add(oid);
      } finally {
        if (m != null) {
          this.objectManager.releaseReadOnly(m);
        }
      }
    }
    while (i.hasNext()) {
      notSynced.add((ObjectID) i.next());
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
