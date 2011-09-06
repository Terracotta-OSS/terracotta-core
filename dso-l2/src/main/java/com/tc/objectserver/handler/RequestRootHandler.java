/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.msg.RequestRootMessage;
import com.tc.object.msg.RequestRootResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

/**
 * @author steve
 */
public class RequestRootHandler extends AbstractEventHandler {
  private ObjectManager     objectManager;
  private DSOChannelManager channelManager;
  private TCLogger          logger;

  public void handleEvent(EventContext context) {
    RequestRootMessage rrm = (RequestRootMessage) context;
    ObjectID rootID = objectManager.lookupRootID(rrm.getRootName());
    try {
      MessageChannel channel = channelManager.getActiveChannel(rrm.getSourceNodeID());

      RequestRootResponseMessage rrrm = (RequestRootResponseMessage) channel
          .createMessage(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE);
      rrrm.initialize(rrm.getRootName(), rootID);
      rrrm.send();
    } catch (NoSuchChannelException e) {
      logger.info("Failed to send root request response because channel:" + rrm.getSourceNodeID() + " is disconnected.");
      return;
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;

    objectManager = oscc.getObjectManager();
    this.channelManager = oscc.getChannelManager();
    this.logger = oscc.getLogger(this.getClass());
  }
}