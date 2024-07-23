/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.entity.InvocationCallback;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PassthroughConnectionStateTest {
  @Test
  public void testOldTransactionIDComputation() throws Exception {
    PassthroughServerProcess passthroughServer = mock(PassthroughServerProcess.class);
    PassthroughConnectionState passthroughConnectionState = new PassthroughConnectionState(passthroughServer);
    ArgumentCaptor<byte[]> byteArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
    doNothing().when(passthroughServer).sendMessageToServer(ArgumentCaptor.forClass(PassthroughConnection.class).capture(), byteArgumentCaptor.capture());

    //send some messages and verify
    for(int i = 1; i <= 20; i++) {
      sendOneMessageAndVerify(passthroughConnectionState, byteArgumentCaptor, 1, i);
    }

    //clear some messages from in-flight messages and verify
    for(int i = 1; i <= 5L; i++) {
      passthroughConnectionState.removeInvocationForTransaction(null, i);
    }
    sendOneMessageAndVerify(passthroughConnectionState, byteArgumentCaptor, 6, 21);

    //clear all in-flight messages and verify
    for(int i = 6; i <= 21; i++) {
      passthroughConnectionState.removeInvocationForTransaction(null, i);
    }
    sendOneMessageAndVerify(passthroughConnectionState, byteArgumentCaptor, 22, 22);
  }

  private static void sendOneMessageAndVerify(PassthroughConnectionState passthroughConnectionState, ArgumentCaptor<byte[]> byteArgumentCaptor, long expectedOldTxnID, long expectedCurTxnID) {
    passthroughConnectionState.sendNormal(mock(PassthroughConnection.class), PassthroughMessageCodec.createAckMessage(), mock(InvocationCallback.class));
    TestTxnInfo testTxnInfo = PassthroughMessageCodec.decodeRawMessage((type, shouldReplicate, transactionID, oldestTransactionID, input) -> new TestTxnInfo(oldestTransactionID, transactionID), byteArgumentCaptor
        .getValue());
    assertThat(testTxnInfo.oldestTransactionID <= testTxnInfo.currentTransactionID, is(true));
    assertThat(testTxnInfo.oldestTransactionID, is(equalTo(expectedOldTxnID)));
    assertThat(testTxnInfo.currentTransactionID, is(equalTo(expectedCurTxnID)));
  }

  private static class TestTxnInfo {
    public final long oldestTransactionID;
    public final long currentTransactionID;

    private TestTxnInfo(final long oldestTransactionID, final long currentTransactionID) {
      this.oldestTransactionID = oldestTransactionID;
      this.currentTransactionID = currentTransactionID;
    }
  }

}