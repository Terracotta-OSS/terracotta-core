/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

    @Override
    public void add(EventContext context) {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public void addMany(Collection contexts) {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public void clear() {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public AddPredicate getPredicate() {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public int size() {
      throw new AssertionError("not supposed to happen");
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();

    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
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

    @Override
    public void addAttachment(String key, Object value, boolean replace) {
      throw new ImplementMe();
    }

    @Override
    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();
    }

    @Override
    public void close() {
      this.wasClosed = true;
    }

    @Override
    public TCMessage createMessage(TCMessageType type) {
      throw new ImplementMe();
    }

    @Override
    public Object getAttachment(String key) {
      throw new ImplementMe();
    }

    @Override
    public ChannelID getChannelID() {
      return channelID;
    }

    @Override
    public TCSocketAddress getLocalAddress() {
      throw new ImplementMe();
    }

    @Override
    public TCSocketAddress getRemoteAddress() {
      throw new ImplementMe();
    }

    @Override
    public boolean isClosed() {
      throw new ImplementMe();
    }

    @Override
    public boolean isConnected() {
      throw new ImplementMe();
    }

    @Override
    public boolean isOpen() {
      throw new ImplementMe();
    }

    @Override
    public NetworkStackID open() {
      throw new ImplementMe();
    }

    @Override
    public NetworkStackID open(char[] password) {
      throw new ImplementMe();
    }

    @Override
    public Object removeAttachment(String key) {
      throw new ImplementMe();
    }

    @Override
    public void send(TCNetworkMessage message) {
      throw new ImplementMe();
    }

    @Override
    public NodeID getLocalNodeID() {
      throw new ImplementMe();
    }

    @Override
    public void setLocalNodeID(NodeID source) {
      throw new ImplementMe();

    }

    @Override
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

    @Override
    public void dehydrate() {
      throw new ImplementMe();
    }

    @Override
    public MessageChannel getChannel() {
      return channel;
    }

    @Override
    public SessionID getLocalSessionID() {
      throw new ImplementMe();
    }

    @Override
    public TCMessageType getMessageType() {
      return TCMessageType.PING_MESSAGE;
    }

    @Override
    public int getTotalLength() {
      throw new ImplementMe();
    }

    @Override
    public void hydrate() {
      this.wasHydrated = true;
      throw new RuntimeException(
                                 "This exception is SUPPOSED to happen -- please don't squelch it's printing in HydrateHandler");
    }

    @Override
    public void send() {
      throw new ImplementMe();
    }

    @Override
    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    @Override
    public NodeID getDestinationNodeID() {
      throw new ImplementMe();
    }
  }

}
