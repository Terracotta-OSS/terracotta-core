/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.protocol.tcm;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.session.SessionID;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import java.util.EnumMap;
import java.util.Map;

public class TCMessageFactoryImpl implements TCMessageFactory {
  private final Map<TCMessageType, GeneratedMessageFactory> generators = new EnumMap<>(TCMessageType.class);
  private final MessageMonitor  monitor;

  public TCMessageFactoryImpl(MessageMonitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public TCAction createMessage(MessageChannel source, TCMessageType type, TCByteBufferOutputStream output)
      throws UnsupportedMessageTypeException {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(source.getSessionID(), this.monitor,
                                 output, source, type);
  }

  @Override
  public TCAction createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header,
                                 TCByteBufferInputStream data) {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(source.getSessionID(), this.monitor, source,
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
  public void addClassMapping(TCMessageType type, Class<? extends TCAction> msgClass) {
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
    
    GeneratedMessageFactoryImpl(Class<? extends TCAction> msgClass) {
      sendHdl = findMethodHandle(msgClass, SessionID.class, MessageMonitor.class, TCByteBufferOutputStream.class,
                                 MessageChannel.class, TCMessageType.class);
      recvHdl = findMethodHandle(msgClass, SessionID.class, MessageMonitor.class, MessageChannel.class,
                                 TCMessageHeader.class, TCByteBufferInputStream.class);
      if (sendHdl == null && recvHdl == null) {
        // require at least one half of message construction to work
        throw new RuntimeException("No constructors available for " + msgClass);
      }
    }

    private static MethodHandle findMethodHandle(Class<? extends TCAction> msgClass, Class<?>... argTypes) {
      try {
        MethodType type = MethodType.methodType(void.class, argTypes);
        return MethodHandles.lookup().findConstructor(msgClass, type);
      } catch (Exception e) {
        return null;
      }
    }
    
    @Override
    public TCAction createMessage(SessionID sid, MessageMonitor monitor, TCByteBufferOutputStream output,
                                   MessageChannel channel, TCMessageType type) {
      if (sendHdl == null) { throw new UnsupportedOperationException(); }

      try {
        return (TCAction)sendHdl.invoke(sid, monitor, output, channel, type);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public TCAction createMessage(SessionID sid, MessageMonitor monitor, MessageChannel channel,
                                   TCMessageHeader msgHeader, TCByteBufferInputStream data) {
      if (recvHdl == null) { throw new UnsupportedOperationException(); }

      try {
        return (TCAction)recvHdl.invoke(sid, monitor, channel, msgHeader, data);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

}
