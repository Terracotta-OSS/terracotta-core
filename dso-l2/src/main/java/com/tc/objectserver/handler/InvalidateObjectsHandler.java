/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.InvalidateObjectsMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.context.InvalidateObjectsForClientContext;
import com.tc.objectserver.l1.api.InvalidateObjectManager;

public class InvalidateObjectsHandler extends AbstractEventHandler implements EventHandler {

  private static final TCLogger         logger = TCLogging.getLogger(InvalidateObjectsHandler.class);
  private final InvalidateObjectManager invalidateObjMgr;
  private final DSOChannelManager       channelManager;

  public InvalidateObjectsHandler(InvalidateObjectManager invalidateObjMgr, DSOChannelManager channelManager) {
    this.invalidateObjMgr = invalidateObjMgr;
    this.channelManager = channelManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    InvalidateObjectsForClientContext invalidateContext = (InvalidateObjectsForClientContext) context;
    ClientID clientID = invalidateContext.getClientID();
    Invalidations invalidations = invalidateObjMgr.getObjectsIDsToInvalidate(clientID);

    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) { return; }

    final InvalidateObjectsMessage message = (InvalidateObjectsMessage) channel
        .createMessage(TCMessageType.INVALIDATE_OBJECTS_MESSAGE);

    message.initialize(invalidations);
    message.send();
  }

  private MessageChannel getActiveChannel(final ClientID clientID) {
    try {
      return this.channelManager.getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      logger.warn("Client " + clientID + " disconnect before sending Message to invalidate Objects.");
      return null;
    }
  }

}
