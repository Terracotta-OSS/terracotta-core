/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ServerEntityAction;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.terracotta.entity.ClientDescriptor;

/**
 *
 */
public class PassiveSyncServerEntityRequest extends AbstractServerEntityRequest {
  
  private final GroupManager group;
  private final NodeID passive;

  public PassiveSyncServerEntityRequest(EntityID eid, long version, int concurrency, GroupManager group, NodeID passive) {
    super(new EntityDescriptor(eid,ClientInstanceID.NULL_ID,version), ServerEntityAction.SYNC_ENTITY, makePayload(concurrency), null, null, null, false);
    this.group = group;
    this.passive = passive;
  }

  @Override
  public ServerEntityAction getAction() {
    return ServerEntityAction.SYNC_ENTITY;
  }
  
  public static byte[] makePayload(int concurrency) {
    return ByteBuffer.allocate(Integer.BYTES).putInt(concurrency).array();
  }
  
  public static int getConcurrency(byte[] payload) {
    return ByteBuffer.wrap(payload).getInt();
  }
  
  @Override
  public boolean requiresReplication() {
    return false;
  }
  
  public void sendToPassive(GroupMessage msg) {
    try {
      group.sendTo(passive, msg);
    } catch (GroupException ge) {
      throw new RuntimeException(ge);
    }
  }

  @Override
  public Optional<MessageChannel> getReturnChannel() {
    return Optional.empty();
  }

  @Override
  public ClientDescriptor getSourceDescriptor() {
    return null;
  }
  
  public synchronized void waitFor() {
    try {
      while (!isDone()) {
        this.wait();
      }
    } catch (InterruptedException ie) {
      //  TODO
      throw new RuntimeException(ie);
    }
  }

  @Override
  public synchronized void complete() {
    this.notifyAll();
    super.complete();
  }
}
