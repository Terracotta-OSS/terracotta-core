/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TCMessageFactoryImpl implements TCMessageFactory {
  private final Map<TCMessageType, GeneratedMessageFactory> factories = new ConcurrentHashMap<>();
  private final MessageMonitor  monitor;
  private final SessionProvider sessionProvider;

  public TCMessageFactoryImpl(SessionProvider sessionProvider, MessageMonitor monitor) {
    this.sessionProvider = sessionProvider;
    this.monitor = monitor;
  }

  @Override
  public TCMessage createMessage(MessageChannel source, TCMessageType type)
      throws UnsupportedMessageTypeException {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(this.sessionProvider.getSessionID(), this.monitor,
                                 createBuffer(), source, type);
  }

  private static TCByteBufferOutputStream createBuffer() {
    return new TCByteBufferOutputStream(4, 4096, false);
  }

  @Override
  public TCMessage createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header,
                                 TCByteBuffer[] data) {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(this.sessionProvider.getSessionID(), this.monitor, source,
                                 header, data);
  }

  @Override
  public void addClassMapping(TCMessageType type, GeneratedMessageFactory messageFactory) {
    if ((type == null) || (messageFactory == null)) { throw new IllegalArgumentException(); }
    if (this.factories.put(type, messageFactory) != null) { throw new IllegalStateException(
                                                                                            "message already has class mapping: "
                                                                                                + type); }
  }

  @Override
  public void addClassMapping(TCMessageType type, Class<? extends TCMessage> msgClass) {
    if ((type == null) || (msgClass == null)) { throw new IllegalArgumentException(); }

    // This strange synchronization is for things like system tests that will end up using the same
    // message class, but with different TCMessageFactoryImpl instances
    synchronized (msgClass.getName().intern()) {
      final GeneratedMessageFactory factory = this.factories.get(type);
      if (factory == null) {
        this.factories.put(type, new GeneratedMessageFactoryImpl(msgClass));
      } else {
        throw new IllegalStateException("message already has class mapping: " + type);
      }
    }
  }

  private GeneratedMessageFactory lookupFactory(TCMessageType type) {
    final GeneratedMessageFactory factory = this.factories.get(type);
    if (factory == null) { throw new RuntimeException("No factory for type " + type); }
    return factory;
  }

  private static class GeneratedMessageFactoryImpl implements GeneratedMessageFactory {

    private final Constructor<? extends TCMessage> sendCstr;
    private final Constructor<? extends TCMessage> recvCstr;

    GeneratedMessageFactoryImpl(Class<? extends TCMessage> msgClass) {
      sendCstr = findConstructor(msgClass, SessionID.class, MessageMonitor.class, TCByteBufferOutputStream.class,
                                 MessageChannel.class, TCMessageType.class);
      recvCstr = findConstructor(msgClass, SessionID.class, MessageMonitor.class, MessageChannel.class,
                                 TCMessageHeader.class, TCByteBuffer[].class);

      if (sendCstr == null && recvCstr == null) {
        // require at least one half of message construction to work
        throw new RuntimeException("No constructors available for " + msgClass);
      }
    }

    private static Constructor<? extends TCMessage> findConstructor(Class<? extends TCMessage> msgClass, Class<?>... argTypes) {
      try {
        return msgClass.getConstructor(argTypes);
      } catch (Exception e) {
        return null;
      }
    }

    @Override
    public TCMessage createMessage(SessionID sid, MessageMonitor monitor, TCByteBufferOutputStream output,
                                   MessageChannel channel, TCMessageType type) {
      if (sendCstr == null) { throw new UnsupportedOperationException(); }

      try {
        return sendCstr.newInstance(sid, monitor, output, channel, type);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TCMessage createMessage(SessionID sid, MessageMonitor monitor, MessageChannel channel,
                                   TCMessageHeader msgHeader, TCByteBuffer[] data) {
      if (recvCstr == null) { throw new UnsupportedOperationException(); }

      try {
        return recvCstr.newInstance(sid, monitor, channel, msgHeader, data);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
