/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ServerMapGetValueResponse;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ServerMapRequestPrefetchObjectsContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.Counter;
import org.apache.log4j.Logger;

public class ServerMapPrefetchObjectHandler extends AbstractEventHandler {

  private DSOChannelManager           channelManager;
  private ClientStateManager          clientManager;
  private ObjectManager               objectManager;
  private ChannelStats                channelStats;
  private final Counter               globalObjectRequestCounter;  
  private final Logger                logger = Logger.getLogger(ServerMapPrefetchObjectHandler.class);

  public ServerMapPrefetchObjectHandler(Counter global) {
    globalObjectRequestCounter = global;
  }
  
  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerMapRequestPrefetchObjectsContext) {
      final ServerMapRequestPrefetchObjectsContext responseContext = (ServerMapRequestPrefetchObjectsContext) context;
      sendResponseForGetValue(responseContext);
    }
  }
  
  public void sendResponseForGetValue(ServerMapRequestPrefetchObjectsContext results) {
    final ClientID clientID = results.getClientID();
    final MessageChannel channel = getActiveChannel(clientID);
    if (channel == null) {
      this.logger.info("Client " + clientID + " is not active : Ignoring sending response for getValue() ");
      return;
    }

    final GetValueServerMapResponseMessage responseMessage = (GetValueServerMapResponseMessage) channel
        .createMessage(TCMessageType.GET_VALUE_SERVER_MAP_RESPONSE_MESSAGE);
    int count = results.prefetchObjects(clientManager);
    if ( count > 0 ) {
      this.globalObjectRequestCounter.increment(count);
      this.channelStats.notifyReadOperations(channel, count);
    }
    responseMessage.initializeGetValueResponse(results.getMapID(), results.getSerializer(), results.getAnswers());
    responseMessage.send();
    results.releaseAll(this.objectManager);
    if ( logger.isDebugEnabled() ) {
      debugStats(results);
    }
  } 

    private void debugStats(ServerMapRequestPrefetchObjectsContext results) {
      for ( ServerMapGetValueResponse msg : results.getAnswers() ) {
        logger.debug(msg);
      }
    }

    private MessageChannel getActiveChannel(final ClientID clientID) {
      try {
        return this.channelManager.getActiveChannel(clientID);
      } catch (final NoSuchChannelException e) {
        this.logger.warn("Client " + clientID + " disconnect before sending Response for ServerMap Request ");
        return null;
      }
    }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.channelManager = (DSOChannelManager)oscc.getChannelManager();
    this.objectManager = oscc.getObjectManager();
    this.clientManager = oscc.getClientStateManager();
    this.channelStats = oscc.getChannelStats();
  }

}
