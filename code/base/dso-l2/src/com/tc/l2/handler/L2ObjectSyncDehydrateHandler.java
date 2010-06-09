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
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

import java.util.Iterator;
import java.util.Map;

public class L2ObjectSyncDehydrateHandler extends AbstractEventHandler {

  private static final TCLogger   logger = TCLogging.getLogger(L2ObjectSyncDehydrateHandler.class);

  private final SequenceGenerator sequenceGenerator;

  private Sink                    sendSink;
  private ObjectManager           objectManager;

  public L2ObjectSyncDehydrateHandler(final SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
    final Map moObjects = mosc.getObjects();
    // XXX::Note:: this sequence id is assigned before releasing any objects to ensure that transactions are not missed
    // for object in flight for PASSIVE-UNINITIALIED L2.
    try {
      mosc.setSequenceID(this.sequenceGenerator.getNextSequence(mosc.getNodeID()));
    } catch (final SequenceGeneratorException e) {
      logger.error("Error generating a sequence number ", e);
      releaseAllObjects(moObjects);
      return;
    }
    final ObjectStringSerializer serializer = new ObjectStringSerializer();
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    for (final Iterator i = moObjects.values().iterator(); i.hasNext();) {
      final ManagedObject m = (ManagedObject) i.next();
      m.toDNA(out, serializer, DNAType.L2_SYNC);
      this.objectManager.releaseReadOnly(m);
    }
    mosc.setDehydratedBytes(out.toArray(), moObjects.size(), serializer);
    this.sendSink.add(mosc);
  }

  private void releaseAllObjects(final Map moObjects) {
    for (final Iterator i = moObjects.values().iterator(); i.hasNext();) {
      final ManagedObject m = (ManagedObject) i.next();
      this.objectManager.releaseReadOnly(m);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
  }
}
