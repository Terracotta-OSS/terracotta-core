/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import org.apache.commons.lang.ArrayUtils;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;

import java.lang.reflect.Constructor;
import java.util.Map;

public class TCMessageFactoryImpl implements TCMessageFactory {
  private static final Class[]            SIG1            = new Class[] { MessageMonitor.class, TCByteBufferOutput.class,
      MessageChannel.class, TCMessageType.class          };
  private static final Class[]            SIG2            = new Class[] { SessionID.class, MessageMonitor.class,
      MessageChannel.class, TCMessageHeader.class, TCByteBuffer[].class };
  private static final TCMessageFinalizer NULL_FINALIZER  = new NullFinalizer();

  private final Map                       typeOnlyCstr    = new ConcurrentReaderHashMap();
  private final Map                       typeAndDataCstr = new ConcurrentReaderHashMap();
  private final TCMessageFinalizer        finalizer;
  private final MessageMonitor            monitor;
  private final SessionProvider            sessionProvider;

  public TCMessageFactoryImpl(SessionProvider sessionProvider, MessageMonitor monitor) {
    this(sessionProvider, monitor, NULL_FINALIZER);
  }

  public TCMessageFactoryImpl(SessionProvider sessionProvider, MessageMonitor monitor, TCMessageFinalizer finalizer) {
    this.sessionProvider = sessionProvider;
    this.monitor = monitor;
    this.finalizer = finalizer;
  }

  public TCMessage createMessage(MessageChannel source, TCMessageType type) throws UnsupportedMessageTypeException {
    return createMessage(lookupConstructor(type, typeOnlyCstr), new Object[] { monitor,
        new TCByteBufferOutputStream(4, 4096, false), source, type });
  }

  public TCMessage createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header, TCByteBuffer[] data) {
    return createMessage(lookupConstructor(type, typeAndDataCstr), new Object[] { sessionProvider.getSessionID(), monitor, source, header, data });
  }

  public void addClassMapping(TCMessageType type, Class msgClass) {
    if ((type == null) || (msgClass == null)) { throw new IllegalArgumentException(); }

    Constructor cstr1 = getConstructor(msgClass, SIG1);
    Constructor cstr2 = getConstructor(msgClass, SIG2);

    synchronized (this) {
      typeOnlyCstr.put(type, cstr1);
      typeAndDataCstr.put(type, cstr2);
    }
  }

  private static Constructor lookupConstructor(TCMessageType type, Map map) {
    Constructor rv = (Constructor) map.get(type);
    if (rv == null) { throw new RuntimeException("No class registerted for type " + type); }
    return rv;
  }

  private static Constructor getConstructor(Class msgClass, Class[] signature) {
    try {
      return msgClass.getDeclaredConstructor(signature);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getClass().getName() + ": " + e.getMessage());
    }
  }

  private TCMessage createMessage(Constructor cstr, Object[] args) {
    try {
      TCMessage rv = (TCMessage) cstr.newInstance(args);
      finalizer.finalizeMessage(rv);
      return rv;
    } catch (Exception e) {
      System.err.println("Args; " + ArrayUtils.toString(args));
      throw new RuntimeException(e);
    }
  }

  private static final class NullFinalizer implements TCMessageFinalizer {
    public final void finalizeMessage(TCMessage message) {
      return;
    }
  }

}
