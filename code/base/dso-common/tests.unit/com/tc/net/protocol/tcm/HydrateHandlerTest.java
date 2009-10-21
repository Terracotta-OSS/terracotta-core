/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.object.session.SessionID;
import com.tc.stats.Stats;
import com.tc.test.TCTestCase;

import java.util.Collection;

public class HydrateHandlerTest extends TCTestCase {

  public void testHydrateException() {
    HydrateHandler handler = new HydrateHandler();

    Channel channel = new Channel(1);
    Message message = new Message(channel);
    HydrateContext context = new HydrateContext(message, new TestSink());

    handler.handleEvent(context);
    assertTrue(channel.wasClosed);
    assertTrue(message.wasHydrated);
  }

  private static class TestSink implements Sink {

    public void add(EventContext context) {
      throw new AssertionError("not supposed to happen");
    }

    public boolean addLossy(EventContext context) {
      throw new AssertionError("not supposed to happen");
    }

    public void addMany(Collection contexts) {
      throw new AssertionError("not supposed to happen");
    }

    public void clear() {
      throw new AssertionError("not supposed to happen");
    }

    public AddPredicate getPredicate() {
      throw new AssertionError("not supposed to happen");
    }

    public void setAddPredicate(AddPredicate predicate) {
      throw new AssertionError("not supposed to happen");
    }

    public int size() {
      throw new AssertionError("not supposed to happen");
    }

    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();

    }

    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    public void resetStats() {
      throw new ImplementMe();
    }

  }

  private static class Channel implements MessageChannel {

    private final ChannelID channelID;
    private boolean         wasClosed = false;

    public Channel(int id) {
      channelID = new ChannelID(id);
    }

    public void addAttachment(String key, Object value, boolean replace) {
      throw new ImplementMe();
    }

    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();
    }

    public void close() {
      this.wasClosed = true;
    }

    public TCMessage createMessage(TCMessageType type) {
      throw new ImplementMe();
    }

    public Object getAttachment(String key) {
      throw new ImplementMe();
    }

    public ChannelID getChannelID() {
      return channelID;
    }

    public TCSocketAddress getLocalAddress() {
      throw new ImplementMe();
    }

    public TCSocketAddress getRemoteAddress() {
      throw new ImplementMe();
    }

    public boolean isClosed() {
      throw new ImplementMe();
    }

    public boolean isConnected() {
      throw new ImplementMe();
    }

    public boolean isOpen() {
      throw new ImplementMe();
    }

    public NetworkStackID open() {
      throw new ImplementMe();
    }

    public Object removeAttachment(String key) {
      throw new ImplementMe();
    }

    public void send(TCNetworkMessage message) {
      throw new ImplementMe();
    }

    public NodeID getLocalNodeID() {
      throw new ImplementMe();
    }

    public void setLocalNodeID(NodeID source) {
      throw new ImplementMe();

    }

    public NodeID getRemoteNodeID() {
      throw new ImplementMe();
    }

  }

  private static class Message implements TCMessage {

    private final Channel channel;
    private boolean       wasHydrated = false;

    public Message(Channel channel) {
      this.channel = channel;
    }

    public void dehydrate() {
      throw new ImplementMe();
    }

    public MessageChannel getChannel() {
      return channel;
    }

    public SessionID getLocalSessionID() {
      throw new ImplementMe();
    }

    public TCMessageType getMessageType() {
      return TCMessageType.PING_MESSAGE;
    }

    public int getTotalLength() {
      throw new ImplementMe();
    }

    public void hydrate() {
      this.wasHydrated = true;
      throw new RuntimeException(
                                 "This exception is SUPPOSED to happen -- please don't squelch it's printing in HydrateHandler");
    }

    public void send() {
      throw new ImplementMe();
    }

    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    public NodeID getDestinationNodeID() {
      throw new ImplementMe();
    }
  }

}
