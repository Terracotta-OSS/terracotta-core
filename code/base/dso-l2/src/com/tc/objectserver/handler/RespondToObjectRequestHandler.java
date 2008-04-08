/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class RespondToObjectRequestHandler extends AbstractEventHandler {

  private DSOChannelManager  channelManager;
  private ObjectManager      objectManager;
  private ClientStateManager stateManager;
  private TCLogger           logger;
  private Sequence           batchIDSequence = new SimpleSequence();
  private Sink               managedObjectRequestSink;

  public void handleEvent(EventContext context) {
    long batchID = batchIDSequence.next();
    ManagedObjectRequestContext morc = (ManagedObjectRequestContext) context;
    Collection objs = morc.getObjects();
    LinkedList objectsInOrder = new LinkedList();

    // Check to see if more objects needs to be looked for this request
    createNewLookupRequestsIfNecessary(morc);

    Collection requestedObjectIDs = morc.getLookupIDs();
    Set ids = new HashSet(Math.max((int) (objs.size() / .75f) + 1, 16));
    for (Iterator i = objs.iterator(); i.hasNext();) {
      ManagedObject mo = (ManagedObject) i.next();
      ids.add(mo.getID());
      if (requestedObjectIDs.contains(mo.getID())) {
        objectsInOrder.addLast(mo);
      } else {
        objectsInOrder.addFirst(mo);
      }
    }

    try {
      MessageChannel channel = channelManager.getActiveChannel(morc.getRequestedNodeID());

      // Only send objects that are NOT already there in the client. Look at the comment below.
      Set newIds = stateManager.addReferences(morc.getRequestedNodeID(), ids);
      int sendCount = 0;
      int batches = 0;
      ObjectStringSerializer serializer = new ObjectStringSerializer();
      TCByteBufferOutputStream out = new TCByteBufferOutputStream();
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {

        ManagedObject m = (ManagedObject) i.next();
        i.remove();
        // We dont want to send any object twice to the client even the client requested it 'coz it only means
        // that the object is on its way to the client. This is true because we process the removeObjectIDs and
        // lookups in Order. Earlier the if condition used to look like ...
        // if (ids.contains(m.getID()) || morc.getObjectIDs().contains(m.getID())) {}
        if (newIds.contains(m.getID())) {
          m.toDNA(out, serializer);
          sendCount++;
        } else if (morc.getLookupIDs().contains(m.getID())) {
          // logger.info("Ignoring request for look up from " + morc.getChannelID() + " for " + m.getID());
        }
        objectManager.releaseReadOnly(m);

        if (sendCount > 1000 || (sendCount > 0 && !i.hasNext())) {
          batches++;
          RequestManagedObjectResponseMessage responseMessage = (RequestManagedObjectResponseMessage) channel
              .createMessage(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE);
          responseMessage.initialize(out.toArray(), sendCount, serializer, batchID, i.hasNext() ? 0 : batches);
          responseMessage.send();
          if (i.hasNext()) {
            sendCount = 0;
            serializer = new ObjectStringSerializer();
            out = new TCByteBufferOutputStream();
          }
        }
      }
      Set missingOids = morc.getMissingObjectIDs();
      if (!missingOids.isEmpty()) {
        if (morc.isServerInitiated()) {
          // This is a possible case where changes are flying in and server is initiating some lookups and the lookups
          // go pending and in the meantime the changes made those looked up objects garbage and DGC removes those
          // objects. Now we dont want to send those missing objects to clients. Its not really an issue as the clients
          // should never lookup those objects, but still why send them ?
          logger.warn("Server Initiated lookup : " + morc + ". Ignoring Missing Objects : " + missingOids);
        } else {
          ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) channel
              .createMessage(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE);
          notFound.initialize(missingOids, batchID);
          notFound.send();
        }
      }

    } catch (NoSuchChannelException e) {
      logger.info("Not sending response because channel is disconnected: " + morc.getRequestedNodeID()
                  + ".  Releasing all checked-out objects...");
      for (Iterator i = objectsInOrder.iterator(); i.hasNext();) {
        objectManager.releaseReadOnly((ManagedObject) i.next());
      }
      return;
    }
  }

  private void createNewLookupRequestsIfNecessary(ManagedObjectRequestContext morc) {
    Set oids = morc.getLookupPendingObjectIDs();
    if (oids.isEmpty()) { return; }
    if (logger.isDebugEnabled()) {
      logger.debug("Creating Server initiated requests for : " + morc.getRequestedNodeID()
                   + " org request Id length = " + morc.getLookupIDs().size()
                   + "  Reachable object(s) to be looked up  length = " + oids.size());
    }
    ManagedObjectRequestContext.createAndAddManagedObjectRequestContextsTo(managedObjectRequestSink, morc
        .getRequestedNodeID(), morc.getRequestID(), oids, -1, morc.getSink());

  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.channelManager = oscc.getChannelManager();
    this.objectManager = oscc.getObjectManager();
    this.logger = oscc.getLogger(getClass());
    this.stateManager = oscc.getClientStateManager();
    this.managedObjectRequestSink = oscc.getStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE).getSink();
  }
}
