/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.connectivity;


import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.util.UuidGenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This class provides a general remote proxy. It uses the Dynamic Proxy mechanism that was introduced with JDK 1.3.
 * <p/>The client proxy sends all requests to a server via a socket connection. The server returns results in the same
 * way. Every object that is transferred (i.e. result of method invocation) has to support the Serializable interface.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class RemoteProxy implements InvocationHandler, Serializable {
  /**
   * The serial version uid for the class.
   *
   * @TODO: recalculate
   */
  private static final long serialVersionUID = 1L;

  /**
   * All the instances that have been wrapped by a proxy. Maps each instance to its handle.
   */
  private transient static Map s_instances = new WeakHashMap();

  /**
   * The server host address.
   */
  private final String m_address;

  /**
   * The server port.
   */
  private final int m_port;

  /**
   * The handle to the instance wrapped by this proxy.
   */
  private String m_handle = null;

  /**
   * The interface class for the wrapped instance.
   */
  private Class[] m_targetInterfaces = null;

  /**
   * The names of all the interfaces for the wrapped instance.
   */
  private String[] m_targetInterfaceNames = null;

  /**
   * The class name for the wrapped instance.
   */
  private String m_targetImplName = null;

  /**
   * The socket.
   */
  private transient Socket m_socket;

  /**
   * The input stream.
   */
  private transient ObjectInputStream m_in;

  /**
   * The output stream.
   */
  private transient ObjectOutputStream m_out;

  /**
   * The class loader to use.
   */
  private transient ClassLoader m_loader;

  /**
   * The client context.
   */
  private transient Object m_context = null;

  /**
   * The dynamic proxy instance to the wrapped instance.
   */
  private transient Object m_proxy = null;

  /**
   * Creates a new proxy based on the interface and class names passes to it. For client-side use. This method is
   * never called directly.
   *
   * @param interfaces the class name of the interface for the object to create the proxy for
   * @param impl       the class name of the the object to create the proxy for
   * @param address    the address to connect to.
   * @param port       the port to connect to.
   * @param context    the context carrying the users principal and credentials
   * @param loader     the class loader to use
   */
  private RemoteProxy(final String[] interfaces,
                      final String impl,
                      final String address,
                      final int port,
                      final Object context,
                      final ClassLoader loader) {
    if ((interfaces == null) || (interfaces.length == 0)) {
      throw new IllegalArgumentException("at least one interface must be specified");
    }
    if (impl == null) {
      throw new IllegalArgumentException("implementation class name can not be null");
    }
    if (address == null) {
      throw new IllegalArgumentException("address can not be null");
    }
    if (port < 0) {
      throw new IllegalArgumentException("port not valid");
    }
    m_targetInterfaceNames = interfaces;
    m_targetImplName = impl;
    m_address = address;
    m_port = port;
    m_context = context;
    m_loader = loader;
  }

  /**
   * Creates a new proxy based on the instance passed to it. For server-side use. This method is never called
   * directly.
   *
   * @param targetInstance target instance to create the proxy for
   * @param address        the address to connect to.
   * @param port           the port to connect to.
   */
  private RemoteProxy(final Object targetInstance, final String address, final int port) {
    if (targetInstance == null) {
      throw new IllegalArgumentException("target instance can not be null");
    }
    if (address == null) {
      throw new IllegalArgumentException("address can not be null");
    }
    if (port < 0) {
      throw new IllegalArgumentException("port not valid");
    }
    m_targetInterfaces = targetInstance.getClass().getInterfaces();
    m_address = address;
    m_port = port;
    m_handle = wrapInstance(targetInstance);
  }

  /**
   * Creates a new proxy to a class. To be used on the client side to create a new proxy to an object.
   *
   * @param interfaces the class name of the interface for the object to create the proxy for
   * @param impl       the class name of the the object to create the proxy for
   * @param address    the address to connect to.
   * @param port       the port to connect to.
   * @return the new remote proxy instance
   */
  public static RemoteProxy createClientProxy(final String[] interfaces,
                                              final String impl,
                                              final String address,
                                              final int port) {
    return RemoteProxy.createClientProxy(
            interfaces, impl, address, port, Thread.currentThread().getContextClassLoader()
    );
  }

  /**
   * Creates a new proxy to a class. To be used on the client side to create a new proxy to an object.
   *
   * @param interfaces the class name of the interface for the object to create the proxy for
   * @param impl       the class name of the the object to create the proxy for
   * @param address    the address to connect to.
   * @param port       the port to connect to.
   * @param context    the context carrying the users principal and credentials
   * @return the new remote proxy instance
   */
  public static RemoteProxy createClientProxy(final String[] interfaces,
                                              final String impl,
                                              final String address,
                                              final int port,
                                              final Object context) {
    return RemoteProxy.createClientProxy(
            interfaces, impl, address, port, context, Thread.currentThread()
            .getContextClassLoader()
    );
  }

  /**
   * Creates a new proxy to a class. To be used on the client side to create a new proxy to an object.
   *
   * @param interfaces the class name of the interface for the object to create the proxy for
   * @param impl       the class name of the the object to create the proxy for
   * @param address    the address to connect to.
   * @param port       the port to connect to.
   * @param loader     the class loader to use
   * @return the new remote proxy instance
   */
  public static RemoteProxy createClientProxy(final String[] interfaces,
                                              final String impl,
                                              final String address,
                                              final int port,
                                              final ClassLoader loader) {
    return RemoteProxy.createClientProxy(interfaces, impl, address, port, null, loader);
  }

  /**
   * Creates a new proxy to a class. To be used on the client side to create a new proxy to an object.
   *
   * @param interfaces the class name of the interface for the object to create the proxy for
   * @param impl       the class name of the the object to create the proxy for
   * @param address    the address to connect to.
   * @param port       the port to connect to.
   * @param ctx        the context carrying the users principal and credentials
   * @param loader     the class loader to use
   * @return the new remote proxy instance
   */
  public static RemoteProxy createClientProxy(final String[] interfaces,
                                              final String impl,
                                              final String address,
                                              final int port,
                                              final Object context,
                                              final ClassLoader loader) {
    return new RemoteProxy(interfaces, impl, address, port, context, loader);
  }

  /**
   * Creates a proxy to a specific <b>instance </b> in the on the server side. This proxy could then be passed to the
   * client which can invoke method on this specific <b>instance </b>.
   *
   * @param the     target instance to create the proxy for
   * @param address the address to connect to.
   * @param port    the port to connect to.
   * @return the new remote proxy instance
   */
  public static RemoteProxy createServerProxy(final Object targetlInstance, final String address, final int port) {
    return new RemoteProxy(targetlInstance, address, port);
  }

  /**
   * Look up and retrives a proxy to an object from the server.
   *
   * @param loader the classloader to use
   * @return the proxy instance
   */
  public Object getInstance(final ClassLoader loader) {
    m_loader = loader;
    return getInstance();
  }

  /**
   * Look up and retrives a proxy to an object from the server.
   *
   * @return the proxy instance
   */
  public Object getInstance() {
    if (m_proxy != null) {
      return m_proxy;
    }
    if (m_loader == null) {
      m_loader = Thread.currentThread().getContextClassLoader();
    }
    try {
      m_socket = new Socket(InetAddress.getByName(m_address), m_port);
      m_socket.setTcpNoDelay(true);
      m_out = new ObjectOutputStream(m_socket.getOutputStream());
      m_in = new ObjectInputStream(m_socket.getInputStream());
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
    if (m_handle == null) {
      // is a client side proxy
      if (m_targetInterfaceNames == null) {
        throw new IllegalStateException("interface class name can not be null");
      }
      if (m_targetImplName == null) {
        throw new IllegalStateException("implementation class name can not be null");
      }
      try {
        // create a new instance on the server and getDefault the handle to it in return
        m_out.write(Command.CREATE);
        m_out.writeObject(m_targetImplName);
        m_out.flush();
        m_handle = (String) m_in.readObject();
        m_targetInterfaces = new Class[m_targetInterfaceNames.length];
        for (int i = 0; i < m_targetInterfaceNames.length; i++) {
          try {
            m_targetInterfaces[i] = Class.forName(m_targetInterfaceNames[i], false, m_loader);
          } catch (ClassNotFoundException e) {
            throw new WrappedRuntimeException(e);
          }
        }
      } catch (Exception e) {
        throw new WrappedRuntimeException(e);
      }
    }
    m_proxy = Proxy.newProxyInstance(m_loader, m_targetInterfaces, this);
    return m_proxy;
  }

  /**
   * This method is invoked automatically by the proxy. Should not be called directly.
   *
   * @param proxy  the proxy instance that the method was invoked on
   * @param method the Method instance corresponding to the interface method invoked on the proxy instance.
   * @param args   an array of objects containing the values of the arguments passed in the method invocation on the
   *               proxy instance.
   * @return the value to return from the method invocation on the proxy instance.
   */
  public Object invoke(final Object proxy, final Method method, final Object[] args) {
    try {
      m_out.write(Command.INVOKE);
      m_out.writeObject(m_context);
      m_out.writeObject(m_handle);
      m_out.writeObject(method.getName());
      m_out.writeObject(method.getParameterTypes());
      m_out.writeObject(args);
      m_out.flush();
      final Object response = m_in.readObject();
      if (response instanceof Exception) {
        throw (Exception) response;
      }
      return response;
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Closes the proxy and the connection to the server.
   */
  public void close() {
    try {
      m_out.write(Command.CLOSE);
      m_out.flush();
      m_out.close();
      m_in.close();
      m_socket.close();
    } catch (IOException e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Returns a proxy wrapped instance by its handle.
   *
   * @param handle the handle
   * @return the instance
   */
  public static Object getWrappedInstance(final String handle) {
    return s_instances.get(handle);
  }

  /**
   * Wraps a new instance and maps it to a handle.
   *
   * @param instance the instance to wrap
   * @return the handle for the instance
   */
  public static String wrapInstance(final Object instance) {
    final String handle = UuidGenerator.generate(instance);
    s_instances.put(handle, instance);
    return handle;
  }
}