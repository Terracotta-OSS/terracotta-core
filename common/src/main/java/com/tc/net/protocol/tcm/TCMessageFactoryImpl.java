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
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import java.util.EnumMap;
import java.util.Map;

public class TCMessageFactoryImpl implements TCMessageFactory {
  private final Map<TCMessageType, GeneratedMessageFactory> generators = new EnumMap<>(TCMessageType.class);
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
    return factory.createMessage(this.sessionProvider.getSessionID(), this.monitor, source, type);
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
    if (generators.put(type, messageFactory) != null)  { 
      throw new IllegalStateException("message already has class mapping: "+ type); 
    }
  }

  @Override
  public void addClassMapping(TCMessageType type, Class<? extends TCMessage> msgClass) {
    if ((type == null) || (msgClass == null)) { throw new IllegalArgumentException(); }

    // This strange synchronization is for things like system tests that will end up using the same
    // message class, but with different TCMessageFactoryImpl instances
    synchronized (msgClass.getName().intern()) {
      if (generators.put(type, new GeneratedMessageFactoryImpl(msgClass)) != null) {
        throw new IllegalStateException("message already has class mapping: " + type);
      }
    }
  }

  private GeneratedMessageFactory lookupFactory(TCMessageType type) {
    final GeneratedMessageFactory factory = generators.get(type);
    if (factory == null) { throw new RuntimeException("No factory for type " + type); }
    return factory;
  }

  private static class GeneratedMessageFactoryImpl implements GeneratedMessageFactory {

    private final MethodHandle sendHdl;
    private final MethodHandle recvHdl;
    
    GeneratedMessageFactoryImpl(Class<? extends TCMessage> msgClass) {
      sendHdl = findMethodHandle(msgClass, SessionID.class, MessageMonitor.class, 
                                 MessageChannel.class, TCMessageType.class);
      recvHdl = findMethodHandle(msgClass, SessionID.class, MessageMonitor.class, MessageChannel.class,
                                 TCMessageHeader.class, TCByteBuffer[].class);
      if (sendHdl == null && recvHdl == null) {
        // require at least one half of message construction to work
        throw new RuntimeException("No constructors available for " + msgClass);
      }
    }

    private static MethodHandle findMethodHandle(Class<? extends TCMessage> msgClass, Class<?>... argTypes) {
      try {
        MethodType type = MethodType.methodType(void.class, argTypes);
        return MethodHandles.lookup().findConstructor(msgClass, type);
      } catch (Exception e) {
        return null;
      }
    }
    
    @Override
    public TCMessage createMessage(SessionID sid, MessageMonitor monitor, MessageChannel channel, TCMessageType type) {
      if (sendHdl == null) { throw new UnsupportedOperationException(); }

      try {
        return (TCMessage)sendHdl.invoke(sid, monitor, channel, type);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TCMessage createMessage(SessionID sid, MessageMonitor monitor, MessageChannel channel,
                                   TCMessageHeader msgHeader, TCByteBuffer[] data) {
      if (recvHdl == null) { throw new UnsupportedOperationException(); }

      try {
        return (TCMessage)recvHdl.invoke(sid, monitor, channel, msgHeader, data);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

}
