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

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.util.Assert;
import java.util.LinkedHashMap;
import java.util.Map;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServiceProvider;


public class PlatformEntity implements ManagedEntity {
  public static EntityID PLATFORM_ID = new EntityID("platform", "root");
  public static FetchID PLATFORM_FETCH_ID = new FetchID(0L);
  public static long VERSION = 1L;
  private final Sink<VoltronEntityMessage> messageSelf;
  public final RequestProcessor processor;
  private boolean isActive;

  public PlatformEntity(Sink<VoltronEntityMessage>messageSelf, RequestProcessor processor) {
    this.processor = processor;
    this.messageSelf = messageSelf;
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
  public void addRequestMessage(ServerEntityRequest request, MessagePayload payload, ResultCapture capture) {
    // We don't actually invoke the message, only complete it, so make sure that it wasn't deserialized as something we
    // expect to use.
    processor.scheduleRequest(false, PLATFORM_ID, VERSION, PLATFORM_FETCH_ID, request, payload, (w)-> {capture.complete(payload.getRawPayload());}, false, payload.getConcurrency());    
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
  public SyncReplicationActivity.EntityCreationTuple  startSync() {
    return null;
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
  public Runnable promoteEntity() {
    // Set us to active mode.
    this.isActive = true;
    return  null;
  }

  @Override
  public void resetReferences(int count) {

  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", this.PLATFORM_ID);
    return map;
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

  @Override
  public long getConsumerID() {
    // The platform uses the consumerID 0 constant.
    return ServiceProvider.PLATFORM_CONSUMER_ID;
  }

  @Override
  public void addLifecycleListener(LifecycleListener listener) {
    // Not expected on this entity.
    Assert.assertFalse(true);
  }
}
