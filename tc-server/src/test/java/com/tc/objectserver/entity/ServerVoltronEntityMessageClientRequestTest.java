/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to ensure that CreateMessage and ReconfigureMessage (server-side VoltronEntityMessage implementations)
 * have the correct combination of isClientRequest() and valid/invalid ClientID source and TransactionID.
 *
 * Contract:
 * - Messages with NULL_ID clientId MUST override isClientRequest() to return false
 * - Messages with NULL_ID transactionId MUST override isClientRequest() to return false
 */
public class ServerVoltronEntityMessageClientRequestTest {

  /**
   * CreateMessage returns NULL_ID for both clientId and transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testCreateMessageWithNullIdsReturnsFalseForIsClientRequest() {
    CreateMessage createMessage = new CreateMessage("TestType", "TestName", 1L, new byte[]{1, 2, 3});

    // Verify that getSource returns NULL_ID
    ClientID source = createMessage.getSource();
    assertTrue("CreateMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = createMessage.getTransactionID();
    assertTrue("CreateMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns CREATE_ENTITY
    assertEquals("CreateMessage should return CREATE_ENTITY type",
        VoltronEntityMessage.Type.CREATE_ENTITY, createMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("CreateMessage with NULL_ID clientId and transactionId should return false for isClientRequest()",
        createMessage.isClientRequest());
  }

  /**
   * ReconfigureMessage returns NULL_ID for both clientId and transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testReconfigureMessageWithNullIdsReturnsFalseForIsClientRequest() {
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);
    ReconfigureMessage reconfigureMessage = new ReconfigureMessage(entityDescriptor, new byte[]{1, 2, 3});

    // Verify that getSource returns NULL_ID
    ClientID source = reconfigureMessage.getSource();
    assertTrue("ReconfigureMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = reconfigureMessage.getTransactionID();
    assertTrue("ReconfigureMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns RECONFIGURE_ENTITY
    assertEquals("ReconfigureMessage should return RECONFIGURE_ENTITY type",
        VoltronEntityMessage.Type.RECONFIGURE_ENTITY, reconfigureMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("ReconfigureMessage with NULL_ID clientId and transactionId should return false for isClientRequest()",
        reconfigureMessage.isClientRequest());
  }

  /**
   * DestroyMessage returns NULL_ID for both clientId and transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testDestroyMessageWithNullIdsReturnsFalseForIsClientRequest() {
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);
    DestroyMessage destroyMessage = new DestroyMessage(entityDescriptor);

    // Verify that getSource returns NULL_ID
    ClientID source = destroyMessage.getSource();
    assertTrue("DestroyMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = destroyMessage.getTransactionID();
    assertTrue("DestroyMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns DESTROY_ENTITY
    assertEquals("DestroyMessage should return DESTROY_ENTITY type",
        VoltronEntityMessage.Type.DESTROY_ENTITY, destroyMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("DestroyMessage with NULL_ID clientId and transactionId should return false for isClientRequest()",
        destroyMessage.isClientRequest());
  }

  /**
   * CreateSystemEntityMessage returns NULL_ID for both clientId and transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testCreateSystemEntityMessageWithNullIdsReturnsFalseForIsClientRequest() {
    EntityID entityID = new EntityID("SystemType", "SystemName");
    CreateSystemEntityMessage createSystemMessage = new CreateSystemEntityMessage(
        entityID, 1, com.tc.bytes.TCByteBufferFactory.wrap(new byte[]{1, 2, 3}));

    // Verify that getSource returns NULL_ID
    ClientID source = createSystemMessage.getSource();
    assertTrue("CreateSystemEntityMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = createSystemMessage.getTransactionID();
    assertTrue("CreateSystemEntityMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns CREATE_ENTITY
    assertEquals("CreateSystemEntityMessage should return CREATE_ENTITY type",
        VoltronEntityMessage.Type.CREATE_ENTITY, createSystemMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("CreateSystemEntityMessage with NULL_ID clientId and transactionId should return false for isClientRequest()",
        createSystemMessage.isClientRequest());
  }

  /**
   * LocalPipelineFlushMessage returns NULL_ID for both clientId and transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testLocalPipelineFlushMessageWithNullIdsReturnsFalseForIsClientRequest() {
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);
    LocalPipelineFlushMessage flushMessage = new LocalPipelineFlushMessage(entityDescriptor, false);

    // Verify that getSource returns NULL_ID
    ClientID source = flushMessage.getSource();
    assertTrue("LocalPipelineFlushMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = flushMessage.getTransactionID();
    assertTrue("LocalPipelineFlushMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns LOCAL_PIPELINE_FLUSH
    assertEquals("LocalPipelineFlushMessage should return LOCAL_PIPELINE_FLUSH type",
        VoltronEntityMessage.Type.LOCAL_PIPELINE_FLUSH, flushMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("LocalPipelineFlushMessage with NULL_ID clientId and transactionId should return false for isClientRequest()",
        flushMessage.isClientRequest());
  }

  /**
   * ClientDisconnectMessage returns valid clientId but NULL_ID transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testClientDisconnectMessageWithNullTransactionIdReturnsFalseForIsClientRequest() {
    ClientID clientID = new ClientID(1);
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);
    ClientDisconnectMessage disconnectMessage = new ClientDisconnectMessage(
        clientID, entityDescriptor, null, null);

    // Verify that getSource returns valid clientId
    ClientID source = disconnectMessage.getSource();
    assertEquals("ClientDisconnectMessage should return the provided clientId",
        clientID, source);

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = disconnectMessage.getTransactionID();
    assertTrue("ClientDisconnectMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns DISCONNECT_CLIENT
    assertEquals("ClientDisconnectMessage should return DISCONNECT_CLIENT type",
        VoltronEntityMessage.Type.DISCONNECT_CLIENT, disconnectMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("ClientDisconnectMessage with NULL_ID transactionId should return false for isClientRequest()",
        disconnectMessage.isClientRequest());
  }

  /**
   * ReferenceMessage returns valid clientId but NULL_ID transactionId,
   * and should return false for isClientRequest().
   */
  @Test
  public void testReferenceMessageWithNullTransactionIdReturnsFalseForIsClientRequest() {
    ClientID clientID = new ClientID(1);
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);
    ReferenceMessage referenceMessage = new ReferenceMessage(
        clientID, true, entityDescriptor, com.tc.bytes.TCByteBufferFactory.wrap(new byte[]{1}));

    // Verify that getSource returns valid clientId
    ClientID source = referenceMessage.getSource();
    assertEquals("ReferenceMessage should return the provided clientId",
        clientID, source);

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = referenceMessage.getTransactionID();
    assertTrue("ReferenceMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that getVoltronType returns FETCH_ENTITY
    assertEquals("ReferenceMessage should return FETCH_ENTITY type",
        VoltronEntityMessage.Type.FETCH_ENTITY, referenceMessage.getVoltronType());

    // Verify that isClientRequest returns false
    assertFalse("ReferenceMessage with NULL_ID transactionId should return false for isClientRequest()",
        referenceMessage.isClientRequest());
  }

  /**
   * Comprehensive test verifying all server-side VoltronEntityMessage implementations follow the contract.
   */
  @Test
  public void testAllServerMessagesFollowContract() {
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(
        new EntityID("TestType", "TestName"), 1L);

    // CreateMessage: NULL_ID clientId and transactionId -> isClientRequest() must be false
    CreateMessage createMessage = new CreateMessage("TestType", "TestName", 1L, new byte[]{1});
    assertTrue("CreateMessage must return NULL_ID for clientId",
        createMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("CreateMessage must return NULL_ID for transactionId",
        createMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("CreateMessage must return false for isClientRequest()",
        createMessage.isClientRequest());

    // ReconfigureMessage: NULL_ID clientId and transactionId -> isClientRequest() must be false
    ReconfigureMessage reconfigureMessage = new ReconfigureMessage(entityDescriptor, new byte[]{1});
    assertTrue("ReconfigureMessage must return NULL_ID for clientId",
        reconfigureMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("ReconfigureMessage must return NULL_ID for transactionId",
        reconfigureMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("ReconfigureMessage must return false for isClientRequest()",
        reconfigureMessage.isClientRequest());

    // DestroyMessage: NULL_ID clientId and transactionId -> isClientRequest() must be false
    DestroyMessage destroyMessage = new DestroyMessage(entityDescriptor);
    assertTrue("DestroyMessage must return NULL_ID for clientId",
        destroyMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("DestroyMessage must return NULL_ID for transactionId",
        destroyMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("DestroyMessage must return false for isClientRequest()",
        destroyMessage.isClientRequest());

    // CreateSystemEntityMessage: NULL_ID clientId and transactionId -> isClientRequest() must be false
    CreateSystemEntityMessage createSystemMessage = new CreateSystemEntityMessage(
        new EntityID("SystemType", "SystemName"), 1,
        com.tc.bytes.TCByteBufferFactory.wrap(new byte[]{1}));
    assertTrue("CreateSystemEntityMessage must return NULL_ID for clientId",
        createSystemMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("CreateSystemEntityMessage must return NULL_ID for transactionId",
        createSystemMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("CreateSystemEntityMessage must return false for isClientRequest()",
        createSystemMessage.isClientRequest());

    // LocalPipelineFlushMessage: NULL_ID clientId and transactionId -> isClientRequest() must be false
    LocalPipelineFlushMessage flushMessage = new LocalPipelineFlushMessage(entityDescriptor, false);
    assertTrue("LocalPipelineFlushMessage must return NULL_ID for clientId",
        flushMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("LocalPipelineFlushMessage must return NULL_ID for transactionId",
        flushMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("LocalPipelineFlushMessage must return false for isClientRequest()",
        flushMessage.isClientRequest());

    // ClientDisconnectMessage: valid clientId but NULL_ID transactionId -> isClientRequest() must be false
    ClientDisconnectMessage disconnectMessage = new ClientDisconnectMessage(
        new ClientID(1), entityDescriptor, null, null);
    assertFalse("ClientDisconnectMessage must return valid clientId",
        disconnectMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("ClientDisconnectMessage must return NULL_ID for transactionId",
        disconnectMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("ClientDisconnectMessage must return false for isClientRequest()",
        disconnectMessage.isClientRequest());

    // ReferenceMessage: valid clientId but NULL_ID transactionId -> isClientRequest() must be false
    ReferenceMessage referenceMessage = new ReferenceMessage(
        new ClientID(1), true, entityDescriptor,
        com.tc.bytes.TCByteBufferFactory.wrap(new byte[]{1}));
    assertFalse("ReferenceMessage must return valid clientId",
        referenceMessage.getSource().equals(ClientID.NULL_ID));
    assertTrue("ReferenceMessage must return NULL_ID for transactionId",
        referenceMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertFalse("ReferenceMessage must return false for isClientRequest()",
        referenceMessage.isClientRequest());
  }
}
