/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;

/**
 *
 */
public class ReplicationSender extends AbstractEventHandler<ReplicationMessage> {
  
  private final GroupManager group;
  private static final TCLogger logger           = TCLogging.getLogger(ReplicationSender.class);

  public ReplicationSender(GroupManager group) {
    this.group = group;
  }

  @Override
  public void handleEvent(ReplicationMessage context) throws EventHandlerException {
    for (NodeID nodeid : context.getDestinations()) {
      try {
        group.sendTo(nodeid, context);//  i'm not sure that this is possible, make sure that the same message can be serialized to multiple passives
      }  catch (GroupException ge) {
        logger.info(ge);
      }
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
  }
  
}
