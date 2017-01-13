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
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.util.Assert;
import java.util.function.Consumer;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.StateDumper;
import org.terracotta.exception.EntityException;


public class PlatformEntity implements ManagedEntity {
  public static EntityID PLATFORM_ID = new EntityID("platform", "root");
  public static long VERSION = 1L;
  private static EntityDescriptor descriptor = new EntityDescriptor(PLATFORM_ID, ClientInstanceID.NULL_ID, VERSION);
  public final RequestProcessor processor;
  private boolean isActive;

  public PlatformEntity(RequestProcessor processor) {
    this.processor = processor;
    // We always start in the passive state.
    this.isActive = false;
  }
  
  @Override
  public EntityID getID() {
    return PLATFORM_ID;
  }

  @Override
  public long getVersion() {
    return VERSION;
  }

  @Override
  public SimpleCompletion addRequestMessage(ServerEntityRequest request, MessagePayload payload, Consumer<byte[]> complete, Consumer<EntityException> exception) {
    // We don't actually invoke the message, only complete it, so make sure that it wasn't deserialized as something we
    // expect to use.
    ActivePassiveAckWaiter waiter = processor.scheduleRequest(descriptor, request, payload, ()-> {complete.accept(payload.getRawPayload());}, false, payload.getConcurrency());    
    return new SimpleCompletion() {
      @Override
      public void waitForCompletion() {
        waiter.waitForCompleted();
      }
    };
  }

  @Override
  public boolean isDestroyed() {
    return false;
  }

  @Override
  public boolean isActive() {
    return isActive;
  }

  @Override
  public boolean isRemoveable() {
    return false;
  }

  @Override
  public void sync(NodeID passive) {
  //  never sync
  }

  @Override
  public void startSync() {

  }
  
  @Override
  public void loadEntity(byte[] configuration) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public MessageCodec<?, ?> getCodec() {
    // The platform entity has no codec so calling this means there is an error elsewhere.
    Assert.fail();
    return null;
  }

  @Override
  public void promoteEntity() {
    // Set us to active mode.
    this.isActive = true;
  }

  @Override
  public void resetReferences(int count) {

  }
 
  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    stateDumper.dumpState(getID().toString(), "platform entity");
  }

  @Override
  public boolean clearQueue() {
    return true;
  }

  @Override
  public RetirementManager getRetirementManager() {
    // The platform entity doesn't expose this since it isn't expecting message interdependencies, internally.
    Assert.fail();
    return null;
  }
}
