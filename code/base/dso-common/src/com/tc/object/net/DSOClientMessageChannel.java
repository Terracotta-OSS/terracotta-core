/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import com.tc.async.api.Sink;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

public interface DSOClientMessageChannel {

  public void addClassMapping(TCMessageType messageType, Class messageClass);

  public ChannelIDProvider getChannelIDProvider();

  public void addListener(ChannelEventListener listener);

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink);

  public void open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException;

  public boolean isConnected();

  public void close();

  public ClientMessageChannel channel();

  public LockRequestMessageFactory getLockRequestMessageFactory();

  public RequestRootMessageFactory getRequestRootMessageFactory();

  public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory();

  public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory();

  public CommitTransactionMessageFactory getCommitTransactionMessageFactory();

  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory();

  public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory();

  public JMXMessage getJMXMessage();

}
