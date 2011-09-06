/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.session.SessionProvider;

import java.lang.reflect.Method;
import java.util.Map;

public class TCMessageFactoryImpl implements TCMessageFactory {
  private final Map             factories = new ConcurrentReaderHashMap();
  private final MessageMonitor  monitor;
  private final SessionProvider sessionProvider;

  public TCMessageFactoryImpl(final SessionProvider sessionProvider, final MessageMonitor monitor) {
    this.sessionProvider = sessionProvider;
    this.monitor = monitor;
  }

  public TCMessage createMessage(final MessageChannel source, final TCMessageType type)
      throws UnsupportedMessageTypeException {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(this.sessionProvider.getSessionID(source.getRemoteNodeID()), this.monitor,
                                 new TCByteBufferOutputStream(4, 4096, false), source, type);
  }

  public TCMessage createMessage(final MessageChannel source, final TCMessageType type, final TCMessageHeader header,
                                 final TCByteBuffer[] data) {
    final GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(this.sessionProvider.getSessionID(source.getRemoteNodeID()), this.monitor, source,
                                 header, data);
  }

  public void addClassMapping(final TCMessageType type, final GeneratedMessageFactory messageFactory) {
    if ((type == null) || (messageFactory == null)) { throw new IllegalArgumentException(); }
    if (this.factories.put(type, messageFactory) != null) { throw new IllegalStateException(
                                                                                            "message already has class mapping: "
                                                                                                + type); }
  }

  public void addClassMapping(final TCMessageType type, final Class msgClass) {
    if ((type == null) || (msgClass == null)) { throw new IllegalArgumentException(); }

    // This strange synchronization is for things like system tests that will end up using the same
    // message class, but with different TCMessageFactoryImpl instances
    synchronized (msgClass.getName().intern()) {
      final GeneratedMessageFactory factory = (GeneratedMessageFactory) this.factories.get(type);
      if (factory == null) {
        this.factories.put(type, createFactory(type, msgClass));
      } else {
        throw new IllegalStateException("message already has class mapping: " + type);
      }

    }
  }

  private String generatedFactoryClassName(final TCMessageType type) {
    return getClass().getName() + "$" + type.getTypeName() + "Factory";
  }

  private GeneratedMessageFactory createFactory(final TCMessageType type, final Class msgClass) {
    final String factoryClassName = generatedFactoryClassName(type);

    final ClassLoader loader = msgClass.getClassLoader();

    Class c = null;
    try {
      // The factory class might already exist in the target loader
      c = loader.loadClass(factoryClassName);
    } catch (final ClassNotFoundException e) {
      c = defineFactory(factoryClassName, type, msgClass, loader);
    }

    try {
      return (GeneratedMessageFactory) c.newInstance();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Class defineFactory(final String className, final TCMessageType type, final Class msgClass,
                              final ClassLoader loader) {
    try {
      final byte[] clazz = GeneratedMessageFactoryClassCreator.create(className, msgClass);

      final Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] { String.class,
          byte[].class, Integer.TYPE, Integer.TYPE });
      defineClass.setAccessible(true);

      final Class c = (Class) defineClass.invoke(loader,
                                                 new Object[] { className, clazz, Integer.valueOf(0),
                                                     Integer.valueOf(clazz.length) });
      return c;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private GeneratedMessageFactory lookupFactory(final TCMessageType type) {
    final GeneratedMessageFactory factory = (GeneratedMessageFactory) this.factories.get(type);
    if (factory == null) { throw new RuntimeException("No factory for type " + type); }
    return factory;
  }

}
