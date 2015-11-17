/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_BEGIN;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class ReplicationSender extends AbstractEventHandler<ReplicationEnvelope> {
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager group;
  private final Map<NodeID, AtomicLong> ordering = new HashMap<NodeID, AtomicLong>();
  private static final TCLogger logger           = TCLogging.getLogger(ReplicationSender.class);

  public ReplicationSender(GroupManager group) {
    this.group = group;
  }

  @Override
  public void handleEvent(ReplicationEnvelope context) throws EventHandlerException {
    NodeID nodeid = context.getDestination();
    ReplicationMessage msg = context.getMessage();
    if (msg == null) {
// this is a flush of the replication channel.  shut it down and return;
      ordering.remove(nodeid);
    } else {
      AtomicLong rOrder = ordering.get(nodeid);

      if (rOrder == null) {
        if (msg.getType() != ReplicationMessage.SYNC || msg.getReplicationType()!= SYNC_BEGIN) {
          throw new AssertionError("bad message queue");
        }
        rOrder = new AtomicLong();
        ordering.put(nodeid, rOrder);
      }
      msg.setReplicationID(rOrder.incrementAndGet());
      try {
        group.sendTo(nodeid, msg);
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
