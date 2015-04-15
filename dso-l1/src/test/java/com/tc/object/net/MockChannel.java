/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.net;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.TestClientMessageChannel;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessageFactory;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithKeysMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.msg.SearchRequestMessageFactory;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.object.session.SessionID;

public class MockChannel implements DSOClientMessageChannel {

  @Override
  public void addListener(final ChannelEventListener listener) {
    throw new ImplementMe();
  }

  @Override
  public ClientMessageChannel channel() {
    return new TestClientMessageChannel();
  }

  @Override
  public void close() {
    throw new ImplementMe();
  }

  @Override
  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public ClientIDProvider getClientIDProvider() {
    return new ClientIDProviderImpl(new TestChannelIDProvider());
  }

  @Override
  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public LockRequestMessageFactory getLockRequestMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public RequestRootMessageFactory getRequestRootMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public NodesWithObjectsMessageFactory getNodesWithObjectsMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public KeysForOrphanedValuesMessageFactory getKeysForOrphanedValuesMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public NodeMetaDataMessageFactory getNodeMetaDataMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public NodesWithKeysMessageFactory getNodesWithKeysMessageFactory() {
    throw new ImplementMe();
  }

  @Override
  public boolean isConnected() {
    throw new ImplementMe();
  }

  @Override
  public void open(final char[] password) {
    throw new ImplementMe();
  }

  CompletedTransactionLowWaterMarkMessageFactory nullFactory = new NullCompletedTransactionLowWaterMarkMessageFactory();
  public GroupID[]                               groups      = new GroupID[] { new GroupID(0) };

  @Override
  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
    return this.nullFactory;
  }

  private class NullCompletedTransactionLowWaterMarkMessageFactory implements
      CompletedTransactionLowWaterMarkMessageFactory {

    @Override
    public CompletedTransactionLowWaterMarkMessage newCompletedTransactionLowWaterMarkMessage(final NodeID remoteID) {
      return new CompletedTransactionLowWaterMarkMessage(new SessionID(0), new NullMessageMonitor(),
                                                         new TCByteBufferOutputStream(4, 4096, false),
                                                         new MockMessageChannel(new ChannelID(0)),
                                                         TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE);
    }
  }

  @Override
  public GroupID[] getGroupIDs() {
    return this.groups;
  }

  public ServerMapRequestMessage newServerTCMapRequestMessage(final NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public ServerMapMessageFactory getServerMapMessageFactory() {
    throw new ImplementMe();
  }

  public void reloadConfiguration() {
    throw new ImplementMe();
  }

  @Override
  public SearchRequestMessageFactory getSearchRequestMessageFactory() {
    throw new ImplementMe();
  }
}
