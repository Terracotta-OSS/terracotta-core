package com.tc.object.tx;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessageFactory;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionID;

public class MockChannel implements DSOClientMessageChannel {

  public void addClassMapping(TCMessageType messageType, Class messageClass) {
    throw new ImplementMe();
  }

  public void addListener(ChannelEventListener listener) {
    throw new ImplementMe();
  }

  public ClientMessageChannel channel() {
    throw new ImplementMe();
  }

  public void close() {
    throw new ImplementMe();
  }

  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
    throw new ImplementMe();
  }

  public ChannelIDProvider getChannelIDProvider() {
    throw new ImplementMe();
  }

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    throw new ImplementMe();
  }

  public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
    throw new ImplementMe();
  }

  public LockRequestMessageFactory getLockRequestMessageFactory() {
    throw new ImplementMe();
  }

  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
    throw new ImplementMe();
  }

  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
    throw new ImplementMe();
  }

  public RequestRootMessageFactory getRequestRootMessageFactory() {
    throw new ImplementMe();
  }

  public boolean isConnected() {
    throw new ImplementMe();
  }

  public void open() {
    throw new ImplementMe();
  }

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    throw new ImplementMe();
  }

  public JMXMessage getJMXMessage() {
    throw new ImplementMe();
  }

  CompletedTransactionLowWaterMarkMessageFactory nullFactory = new NullCompletedTransactionLowWaterMarkMessageFactory();

  public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
    return nullFactory;
  }

  private class NullCompletedTransactionLowWaterMarkMessageFactory implements
      CompletedTransactionLowWaterMarkMessageFactory {

    public CompletedTransactionLowWaterMarkMessage newCompletedTransactionLowWaterMarkMessage() {
      return new CompletedTransactionLowWaterMarkMessage(new SessionID(0), new NullMessageMonitor(),
                                                         new TCByteBufferOutputStream(4, 4096, false),
                                                         new MockMessageChannel(new ChannelID(0)),
                                                         TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE);
    }

  }
}
