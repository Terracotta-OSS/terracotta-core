/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.net.groups.GroupManager;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

import java.util.Iterator;
import java.util.Map;

public class L2ObjectSyncDehydrateHandler extends AbstractEventHandler {
  
  private static final TCLogger              logger = TCLogging.getLogger(L2ObjectSyncDehydrateHandler.class);

  private final SequenceGenerator sequenceGenerator;

  private Sink                    sendSink;
  private ObjectManager           objectManager;

  private GroupManager groupManager;

  public L2ObjectSyncDehydrateHandler(SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = sequenceGenerator;
  }

  public void handleEvent(EventContext context) {
    ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
    Map moObjects = mosc.getObjects();
    // XXX::Note:: this sequence id is assigned before releasing any objects to ensure that transactions are not missed
    // for object in flight for PASSIVE-UNINITIALIED L2.
    try {
      mosc.setSequenceID(sequenceGenerator.getNextSequence(mosc.getNodeID()));
    } catch (SequenceGeneratorException e) {
      logger.error("Error generating a sequence number ", e);
      groupManager.zapNode(mosc.getNodeID());
      releaseAllObjects(moObjects);
      return;
    }
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    for (Iterator i = moObjects.values().iterator(); i.hasNext();) {
      ManagedObject m = (ManagedObject) i.next();
      m.toDNA(out, serializer);
      objectManager.releaseReadOnly(m);
    }
    mosc.setDehydratedBytes(out.toArray(), moObjects.size(), serializer);
    sendSink.add(mosc);
  }

  private void releaseAllObjects(Map moObjects) {
    for (Iterator i = moObjects.values().iterator(); i.hasNext();) {
      ManagedObject m = (ManagedObject) i.next();
      objectManager.releaseReadOnly(m);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
  }
}
