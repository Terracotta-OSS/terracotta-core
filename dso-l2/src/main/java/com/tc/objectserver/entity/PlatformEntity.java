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
import com.tc.util.Assert;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;


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
  public void addInvokeRequest(ServerEntityRequest request, EntityMessage entityMessage, byte[] payload, int defaultKey) {
    // We don't actually invoke the message, only complete it, so make sure that it wasn't deserialized as something we
    // expect to use.
    Assert.assertNull(entityMessage);
    processor.scheduleRequest(descriptor, request, payload, ()-> {
      request.complete();
      if (this.isActive) {
        request.retired();
      }
    }, ConcurrencyStrategy.UNIVERSAL_KEY);
  }

  @Override
  public void addSyncRequest(ServerEntityRequest sync, byte[] payload, int concurrencyKey) {
    processor.scheduleRequest(descriptor, sync, payload, ()-> {
      sync.complete();
      if (this.isActive) {
        sync.retired();
      }
    }, ConcurrencyStrategy.MANAGEMENT_KEY);
  }

  @Override
  public void addLifecycleRequest(ServerEntityRequest create, byte[] arg) {
    processor.scheduleRequest(descriptor, create, arg, ()-> {
      create.complete();
      if (this.isActive) {
        create.retired();
      }
    }, ConcurrencyStrategy.MANAGEMENT_KEY);
  }

  @Override
  public void reconnectClient(ClientID clientID, ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {
  // never reconnect
  }

  @Override
  public void sync(NodeID passive) {
  //  never sync
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
}
