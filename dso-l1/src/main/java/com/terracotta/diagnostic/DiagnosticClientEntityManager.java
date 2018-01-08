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
package com.terracotta.diagnostic;

import com.tc.entity.DiagnosticMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage.Acks;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientEntityManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityClientEndpointImpl;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.InFlightMessage;
import com.tc.object.InFlightMonitor;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.tx.TransactionID;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityException;

/**
 *
 */
public class DiagnosticClientEntityManager implements ClientEntityManager {
  
  private final ClientMessageChannel channel;
  private final AtomicLong tid = new AtomicLong();
  private final Map<TransactionID, InFlightMessage> waitingForAnswer = new ConcurrentHashMap<TransactionID, InFlightMessage>();

  public DiagnosticClientEntityManager(ClientMessageChannel channel) {
    this.channel = channel;
  }

  @Override
  public EntityClientEndpoint fetchEntity(EntityID entity, long version, ClientInstanceID entityDescriptor, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec, Runnable closeHook) throws EntityException {
    Assert.assertEquals(Diagnostics.class.getName(), entity.getClassName());
    Assert.assertEquals("root", entity.getEntityName());
    return new EntityClientEndpointImpl(entity, version, EntityDescriptor.NULL_ID, this, new byte[] {}, codec, closeHook, null);
  }

  @Override
  public void handleMessage(ClientInstanceID entityDescriptor, byte[] message) {

  }
  
  @Override
  public void handleMessage(TransactionID entityDescriptor, byte[] message) {

  }
  
  @Override
  public byte[] createEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public boolean destroyEntity(EntityID entityID, long version) throws EntityException {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException {
    throw new UnsupportedOperationException(); 
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Collections.emptyMap();
  }

  @Override
  public void received(TransactionID id) {

  }

  @Override
  public void complete(TransactionID id) {

  }

  @Override
  public void complete(TransactionID id, byte[] value) {
    waitingForAnswer.remove(id).setResult(value, null);
  }

  @Override
  public void failed(TransactionID id, EntityException e) {

  }

  @Override
  public void retired(TransactionID id) {

  }

  @Override
  public void pause() {

  }

  @Override
  public void unpause() {

  }

  @Override
  public void initializeHandshake(ClientHandshakeMessage handshakeMessage) {

  }

  @Override
  public void shutdown(boolean fromShutdownHook) {

  }

  @Override
  public InFlightMessage invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean defer, byte[] payload) {
    DiagnosticMessage network = createMessage(payload);
    InFlightMessage message = new InFlightMessage(eid, network, Collections.<Acks>emptySet(), null, false, false);
    waitingForAnswer.put(network.getTransactionID(), message);
    network.send();
    return message;
  }

  @Override
  public InFlightMessage invokeActionWithTimeout(EntityID eid, EntityDescriptor entityDescriptor, Set<Acks> acks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean defer, long invokeTimeout, TimeUnit units, byte[] payload) throws InterruptedException, TimeoutException {
    return invokeAction(eid, entityDescriptor, acks, monitor, false, false, false, payload);
  }

  private DiagnosticMessage createMessage(byte[] config) {
    // Get the clientID for our channel.
    // Get the next transaction ID.
    TransactionID transactionID = new TransactionID(tid.incrementAndGet());

    // Create the message and populate it.
    DiagnosticMessage message = (DiagnosticMessage) channel.createMessage(TCMessageType.DIAGNOSTIC_REQUEST);
    Assert.assertNotNull(config);
    message.setContents(transactionID, config);
    return message;
  }
}
