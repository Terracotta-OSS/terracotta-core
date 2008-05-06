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

  public TCMessageFactoryImpl(SessionProvider sessionProvider, MessageMonitor monitor) {
    this.sessionProvider = sessionProvider;
    this.monitor = monitor;
  }

  public TCMessage createMessage(MessageChannel source, TCMessageType type) throws UnsupportedMessageTypeException {
    GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(sessionProvider.getSessionID(), monitor, new TCByteBufferOutputStream(4, 4096, false),
                                 source, type);
  }

  public TCMessage createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header, TCByteBuffer[] data) {
    GeneratedMessageFactory factory = lookupFactory(type);
    return factory.createMessage(sessionProvider.getSessionID(), monitor, source, header, data);
  }

  public void addClassMapping(TCMessageType type, Class msgClass) {
    if ((type == null) || (msgClass == null)) { throw new IllegalArgumentException(); }

    // This strange synchronization is for things like system tests that will end up using the same
    // message class, but with different TCMessageFactoryImpl instances
    synchronized (msgClass.getName().intern()) {
      GeneratedMessageFactory factory = (GeneratedMessageFactory) factories.get(type);
      if (factory == null) {
        factories.put(type, createFactory(type, msgClass));
      } else {
        throw new IllegalStateException("message already has class mapping: " + type);
      }

    }
  }

  private String generatedFactoryClassName(TCMessageType type) {
    return getClass().getName() + "$" + type.getTypeName() + "Factory";
  }

  private GeneratedMessageFactory createFactory(TCMessageType type, Class msgClass) {
    String factoryClassName = generatedFactoryClassName(type);

    ClassLoader loader = msgClass.getClassLoader();

    Class c = null;
    try {
      // The factory class might already exist in the target loader
      c = loader.loadClass(factoryClassName);
    } catch (ClassNotFoundException e) {
      c = defineFactory(factoryClassName, type, msgClass, loader);
    }

    try {
      return (GeneratedMessageFactory) c.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Class defineFactory(String className, TCMessageType type, Class msgClass, ClassLoader loader) {
    try {
      byte[] clazz = GeneratedMessageFactoryClassCreator.create(className, msgClass);

      Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class,
          Integer.TYPE, Integer.TYPE });
      defineClass.setAccessible(true);

      Class c = (Class) defineClass.invoke(loader, new Object[] { className, clazz, new Integer(0),
          new Integer(clazz.length) });
      return c;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private GeneratedMessageFactory lookupFactory(TCMessageType type) {
    GeneratedMessageFactory factory = (GeneratedMessageFactory) factories.get(type);
    if (factory == null) { throw new RuntimeException("No factory for type " + type); }
    return factory;
  }

}
