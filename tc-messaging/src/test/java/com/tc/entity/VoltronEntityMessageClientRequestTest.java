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
package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test to ensure that all subclasses of VoltronEntityMessage have the correct combination
 * of isClientRequest() and valid/invalid ClientID source and TransactionID.
 *
 * Contract:
 * - Messages with NULL_ID clientId MUST override isClientRequest() to return false
 * - Messages with NULL_ID transactionId MUST override isClientRequest() to return false
 * - Messages with valid clientId and transactionId SHOULD return true for isClientRequest()
 */
public class VoltronEntityMessageClientRequestTest {

  /**
   * DiagnosticMessageImpl returns ClientID.NULL_ID and should return false for isClientRequest().
   */
  @Test
  public void testDiagnosticMessageWithNullClientIdReturnsFalseForIsClientRequest() {
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    MessageChannel channel = null;
    TCMessageType type = TCMessageType.DIAGNOSTIC_REQUEST;
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    DiagnosticMessageImpl diagnosticMessage = new DiagnosticMessageImpl(
        sessionID, monitor, outputStream, channel, type);

    // Set contents with a valid transaction ID
    TransactionID transactionID = new TransactionID(1);
    byte[] extendedData = new byte[]{1, 2, 3};
    diagnosticMessage.setContents(transactionID, extendedData);

    // Verify that getSource returns NULL_ID
    ClientID source = diagnosticMessage.getSource();
    assertTrue("DiagnosticMessage should return NULL_ID for getSource",
        source.equals(ClientID.NULL_ID));

    // Verify that isClientRequest returns false
    assertFalse("DiagnosticMessage with NULL_ID clientId should return false for isClientRequest()",
        diagnosticMessage.isClientRequest());
  }

  /**
   * DiagnosticMessageImpl with NULL_ID transaction should return false for isClientRequest().
   */
  @Test
  public void testDiagnosticMessageWithNullTransactionIdReturnsFalseForIsClientRequest() {
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    MessageChannel channel = null;
    TCMessageType type = TCMessageType.DIAGNOSTIC_REQUEST;
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    DiagnosticMessageImpl diagnosticMessage = new DiagnosticMessageImpl(
        sessionID, monitor, outputStream, channel, type);

    // Set contents with NULL_ID transaction
    TransactionID transactionID = TransactionID.NULL_ID;
    byte[] extendedData = new byte[]{1, 2, 3};
    diagnosticMessage.setContents(transactionID, extendedData);

    // Verify that getTransactionID returns NULL_ID
    TransactionID txnId = diagnosticMessage.getTransactionID();
    assertTrue("DiagnosticMessage should return NULL_ID for getTransactionID",
        txnId.equals(TransactionID.NULL_ID));

    // Verify that isClientRequest returns false
    assertFalse("DiagnosticMessage with NULL_ID transactionId should return false for isClientRequest()",
        diagnosticMessage.isClientRequest());
  }

  /**
   * NetworkVoltronEntityMessageImpl with valid clientId and transactionId should return true for isClientRequest().
   * This is the baseline test to ensure normal client requests work correctly.
   */
  @Test
  public void testNetworkVoltronEntityMessageWithValidIdsReturnsTrueForIsClientRequest() {
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    MessageChannel channel = null;
    TCMessageType type = TCMessageType.VOLTRON_ENTITY_MESSAGE;
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    NetworkVoltronEntityMessageImpl message = new NetworkVoltronEntityMessageImpl(
        sessionID, monitor, outputStream, channel, type);

    // Set valid contents
    ClientID clientID = new ClientID(1);
    TransactionID transactionID = new TransactionID(2);
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3);
    VoltronEntityMessage.Type messageType = VoltronEntityMessage.Type.FETCH_ENTITY;
    boolean requiresReplication = false;
    TCByteBuffer extendedData = TCByteBufferFactory.wrap(new byte[1]);
    TransactionID oldestTransactionPending = new TransactionID(1);

    message.setContents(clientID, transactionID, EntityID.NULL_ID, entityDescriptor,
        messageType, requiresReplication, extendedData, oldestTransactionPending,
        EnumSet.of(VoltronEntityMessage.Acks.RECEIVED));

    // Verify that isClientRequest returns true (default behavior)
    assertTrue("NetworkVoltronEntityMessage with valid IDs should return true for isClientRequest()",
        message.isClientRequest());
  }

  /**
   * ResendVoltronEntityMessage with valid clientId and transactionId should return true for isClientRequest().
   * This verifies that resend messages are treated as client requests.
   */
  @Test
  public void testResendVoltronEntityMessageWithValidIdsReturnsTrueForIsClientRequest() {
    ClientID clientID = new ClientID(1);
    TransactionID transactionID = new TransactionID(2);
    EntityDescriptor entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3);
    VoltronEntityMessage.Type messageType = VoltronEntityMessage.Type.INVOKE_ACTION;
    boolean requiresReplication = true;
    TCByteBuffer extendedData = TCByteBufferFactory.wrap(new byte[]{1, 2, 3});

    ResendVoltronEntityMessage resendMessage = new ResendVoltronEntityMessage(
        clientID, transactionID, entityDescriptor, messageType, requiresReplication, extendedData);

    // Verify that isClientRequest returns true (default behavior)
    assertTrue("ResendVoltronEntityMessage with valid IDs should return true for isClientRequest()",
        resendMessage.isClientRequest());
  }

  /**
   * Test that verifies the contract: any VoltronEntityMessage implementation that returns
   * NULL_ID for clientId must override isClientRequest() to return false.
   * This test documents the expected behavior.
   */
  @Test
  public void testContractNullClientIdImpliesFalseIsClientRequest() {
    // Create a DiagnosticMessage which is the only known implementation that returns NULL_ID
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    DiagnosticMessageImpl diagnosticMessage = new DiagnosticMessageImpl(
        sessionID, monitor, outputStream, null, TCMessageType.DIAGNOSTIC_REQUEST);

    diagnosticMessage.setContents(new TransactionID(1), new byte[]{1});

    // Verify the contract
    if (diagnosticMessage.getSource().equals(ClientID.NULL_ID)) {
      assertFalse("Messages with NULL_ID clientId must return false for isClientRequest()",
          diagnosticMessage.isClientRequest());
    }
  }

  /**
   * Test that verifies the contract: any VoltronEntityMessage implementation that returns
   * NULL_ID for transactionId must override isClientRequest() to return false.
   * This test documents the expected behavior.
   */
  @Test
  public void testContractNullTransactionIdImpliesFalseIsClientRequest() {
    // Create a DiagnosticMessage with NULL_ID transaction
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    DiagnosticMessageImpl diagnosticMessage = new DiagnosticMessageImpl(
        sessionID, monitor, outputStream, null, TCMessageType.DIAGNOSTIC_REQUEST);

    diagnosticMessage.setContents(TransactionID.NULL_ID, new byte[]{1});

    // Verify the contract
    if (diagnosticMessage.getTransactionID().equals(TransactionID.NULL_ID)) {
      assertFalse("Messages with NULL_ID transactionId must return false for isClientRequest()",
          diagnosticMessage.isClientRequest());
    }
  }

  // ========== Tests for all VoltronEntityMessage.Type values ==========

  /**
   * Test NetworkVoltronEntityMessage with CREATE_ENTITY type.
   */
  @Test
  public void testNetworkVoltronEntityMessageCreateEntity() {
    testNetworkMessageType(VoltronEntityMessage.Type.CREATE_ENTITY, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with DESTROY_ENTITY type.
   */
  @Test
  public void testNetworkVoltronEntityMessageDestroyEntity() {
    testNetworkMessageType(VoltronEntityMessage.Type.DESTROY_ENTITY, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with RECONFIGURE_ENTITY type.
   */
  @Test
  public void testNetworkVoltronEntityMessageReconfigureEntity() {
    testNetworkMessageType(VoltronEntityMessage.Type.RECONFIGURE_ENTITY, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with FETCH_ENTITY type.
   */
  @Test
  public void testNetworkVoltronEntityMessageFetchEntity() {
    testNetworkMessageType(VoltronEntityMessage.Type.FETCH_ENTITY, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with RELEASE_ENTITY type.
   */
  @Test
  public void testNetworkVoltronEntityMessageReleaseEntity() {
    testNetworkMessageType(VoltronEntityMessage.Type.RELEASE_ENTITY, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with INVOKE_ACTION type.
   */
  @Test
  public void testNetworkVoltronEntityMessageInvokeAction() {
    testNetworkMessageType(VoltronEntityMessage.Type.INVOKE_ACTION, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with LOCAL_PIPELINE_FLUSH type.
   */
  @Test
  public void testNetworkVoltronEntityMessageLocalPipelineFlush() {
    testNetworkMessageType(VoltronEntityMessage.Type.LOCAL_PIPELINE_FLUSH, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with LOCAL_ENTITY_GC type.
   */
  @Test
  public void testNetworkVoltronEntityMessageLocalEntityGc() {
    testNetworkMessageType(VoltronEntityMessage.Type.LOCAL_ENTITY_GC, true);
  }

  /**
   * Test NetworkVoltronEntityMessage with DISCONNECT_CLIENT type.
   */
  @Test
  public void testNetworkVoltronEntityMessageDisconnectClient() {
    testNetworkMessageType(VoltronEntityMessage.Type.DISCONNECT_CLIENT, true);
  }

  /**
   * Helper method to test NetworkVoltronEntityMessage with different message types.
   */
  private void testNetworkMessageType(VoltronEntityMessage.Type messageType, boolean expectedIsClientRequest) {
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    MessageChannel channel = null;
    TCMessageType type = TCMessageType.VOLTRON_ENTITY_MESSAGE;
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    NetworkVoltronEntityMessageImpl message = new NetworkVoltronEntityMessageImpl(
        sessionID, monitor, outputStream, channel, type);

    ClientID clientID = new ClientID(1);
    TransactionID transactionID = new TransactionID(2);
    EntityDescriptor entityDescriptor;

    // INVOKE_ACTION requires indexed descriptor, others don't
    if (messageType == VoltronEntityMessage.Type.INVOKE_ACTION) {
      entityDescriptor = EntityDescriptor.createDescriptorForInvoke(
          new FetchID(3), new ClientInstanceID(4));
    } else {
      entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3);
    }

    boolean requiresReplication = false;
    TCByteBuffer extendedData = TCByteBufferFactory.wrap(new byte[1]);
    TransactionID oldestTransactionPending = new TransactionID(1);

    message.setContents(clientID, transactionID, EntityID.NULL_ID, entityDescriptor,
        messageType, requiresReplication, extendedData, oldestTransactionPending,
        EnumSet.of(VoltronEntityMessage.Acks.RECEIVED));

    // Verify clientID and transactionID are valid
    assertEquals("ClientID should match", clientID, message.getSource());
    assertEquals("TransactionID should match", transactionID, message.getTransactionID());
    assertEquals("Message type should match", messageType, message.getVoltronType());

    // Verify isClientRequest returns expected value
    assertEquals("NetworkVoltronEntityMessage with " + messageType + " should return " +
        expectedIsClientRequest + " for isClientRequest()",
        expectedIsClientRequest, message.isClientRequest());
  }

  // ========== Tests for ResendVoltronEntityMessage with all message types ==========

  /**
   * Test ResendVoltronEntityMessage with CREATE_ENTITY type.
   */
  @Test
  public void testResendVoltronEntityMessageCreateEntity() {
    testResendMessageType(VoltronEntityMessage.Type.CREATE_ENTITY, true);
  }

  /**
   * Test ResendVoltronEntityMessage with DESTROY_ENTITY type.
   */
  @Test
  public void testResendVoltronEntityMessageDestroyEntity() {
    testResendMessageType(VoltronEntityMessage.Type.DESTROY_ENTITY, true);
  }

  /**
   * Test ResendVoltronEntityMessage with RECONFIGURE_ENTITY type.
   */
  @Test
  public void testResendVoltronEntityMessageReconfigureEntity() {
    testResendMessageType(VoltronEntityMessage.Type.RECONFIGURE_ENTITY, true);
  }

  /**
   * Test ResendVoltronEntityMessage with FETCH_ENTITY type.
   */
  @Test
  public void testResendVoltronEntityMessageFetchEntity() {
    testResendMessageType(VoltronEntityMessage.Type.FETCH_ENTITY, true);
  }

  /**
   * Test ResendVoltronEntityMessage with RELEASE_ENTITY type.
   */
  @Test
  public void testResendVoltronEntityMessageReleaseEntity() {
    testResendMessageType(VoltronEntityMessage.Type.RELEASE_ENTITY, true);
  }

  /**
   * Test ResendVoltronEntityMessage with INVOKE_ACTION type.
   */
  @Test
  public void testResendVoltronEntityMessageInvokeAction() {
    testResendMessageType(VoltronEntityMessage.Type.INVOKE_ACTION, true);
  }

  /**
   * Helper method to test ResendVoltronEntityMessage with different message types.
   */
  private void testResendMessageType(VoltronEntityMessage.Type messageType, boolean expectedIsClientRequest) {
    ClientID clientID = new ClientID(1);
    TransactionID transactionID = new TransactionID(2);
    EntityDescriptor entityDescriptor;

    // INVOKE_ACTION requires indexed descriptor, others don't
    if (messageType == VoltronEntityMessage.Type.INVOKE_ACTION) {
      entityDescriptor = EntityDescriptor.createDescriptorForInvoke(
          new FetchID(3), new ClientInstanceID(4));
    } else {
      entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3);
    }

    boolean requiresReplication = true;
    TCByteBuffer extendedData = TCByteBufferFactory.wrap(new byte[]{1, 2, 3});

    ResendVoltronEntityMessage resendMessage = new ResendVoltronEntityMessage(
        clientID, transactionID, entityDescriptor, messageType, requiresReplication, extendedData);

    // Verify clientID and transactionID are valid
    assertEquals("ClientID should match", clientID, resendMessage.getSource());
    assertEquals("TransactionID should match", transactionID, resendMessage.getTransactionID());
    assertEquals("Message type should match", messageType, resendMessage.getVoltronType());

    // Verify isClientRequest returns expected value
    assertEquals("ResendVoltronEntityMessage with " + messageType + " should return " +
        expectedIsClientRequest + " for isClientRequest()",
        expectedIsClientRequest, resendMessage.isClientRequest());
  }

  /**
   * Comprehensive test verifying all VoltronEntityMessage implementations follow the contract.
   */
  @Test
  public void testAllImplementationsFollowContract() {
    // DiagnosticMessageImpl: NULL_ID clientId -> isClientRequest() must be false
    SessionID sessionID = new SessionID(0);
    MessageMonitor monitor = mock(MessageMonitor.class);
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream(4, 4096);

    DiagnosticMessageImpl diagnosticMessage = new DiagnosticMessageImpl(
        sessionID, monitor, outputStream, null, TCMessageType.DIAGNOSTIC_REQUEST);
    diagnosticMessage.setContents(new TransactionID(1), new byte[]{1});

    assertTrue("DiagnosticMessage must return NULL_ID for clientId",
        diagnosticMessage.getSource().equals(ClientID.NULL_ID));
    assertFalse("DiagnosticMessage with NULL_ID clientId must return false for isClientRequest()",
        diagnosticMessage.isClientRequest());

    // NetworkVoltronEntityMessageImpl: valid IDs -> isClientRequest() must be true
    NetworkVoltronEntityMessageImpl networkMessage = new NetworkVoltronEntityMessageImpl(
        sessionID, monitor, new TCByteBufferOutputStream(4, 4096), null,
        TCMessageType.VOLTRON_ENTITY_MESSAGE);
    networkMessage.setContents(new ClientID(1), new TransactionID(2), EntityID.NULL_ID,
        EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3),
        VoltronEntityMessage.Type.FETCH_ENTITY, false,
        TCByteBufferFactory.wrap(new byte[1]), new TransactionID(1),
        EnumSet.of(VoltronEntityMessage.Acks.RECEIVED));

    assertFalse("NetworkVoltronEntityMessage must not return NULL_ID for clientId",
        networkMessage.getSource().equals(ClientID.NULL_ID));
    assertFalse("NetworkVoltronEntityMessage must not return NULL_ID for transactionId",
        networkMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertTrue("NetworkVoltronEntityMessage with valid IDs must return true for isClientRequest()",
        networkMessage.isClientRequest());

    // ResendVoltronEntityMessage: valid IDs -> isClientRequest() must be true
    ResendVoltronEntityMessage resendMessage = new ResendVoltronEntityMessage(
        new ClientID(1), new TransactionID(2),
        EntityDescriptor.createDescriptorForLifecycle(EntityID.NULL_ID, 3),
        VoltronEntityMessage.Type.INVOKE_ACTION, true,
        TCByteBufferFactory.wrap(new byte[]{1, 2, 3}));

    assertFalse("ResendVoltronEntityMessage must not return NULL_ID for clientId",
        resendMessage.getSource().equals(ClientID.NULL_ID));
    assertFalse("ResendVoltronEntityMessage must not return NULL_ID for transactionId",
        resendMessage.getTransactionID().equals(TransactionID.NULL_ID));
    assertTrue("ResendVoltronEntityMessage with valid IDs must return true for isClientRequest()",
        resendMessage.isClientRequest());
  }
}
