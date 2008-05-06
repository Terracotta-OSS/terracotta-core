/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.connectivity;

import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Implements a server thread. Each request from the client gets its own instance. <p/>Response to three different
 * commands: <br/>Command.CREATE, Command.INVOKE and Command.CLOSE. <p/>It redirects the method invocation to the
 * Invoker for the class.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class RemoteProxyServerThread implements Runnable {
  /**
   * The socket.
   */
  private final Socket m_socket;

  /**
   * The input stream.
   */
  private ObjectInputStream m_in = null;

  /**
   * The output stream.
   */
  private ObjectOutputStream m_out = null;

  /**
   * The class loader to use.
   */
  private ClassLoader m_loader = null;

  /**
   * The custom invoker instance.
   */
  private Invoker m_invoker = null;

  /**
   * The time-out for the socket.
   */
  private int m_timeout = 60000;

  /**
   * Is-running flag.
   */
  private boolean m_running = true;

  /**
   * Creates a new instance.
   *
   * @param clientSocket the client socket
   * @param loader       the classloader to use
   * @param invoker      the invoker that makes the method invocation in the client thread
   */
  public RemoteProxyServerThread(final Socket clientSocket,
                                 final ClassLoader loader,
                                 final Invoker invoker,
                                 final int timeout) {
    if (clientSocket == null) {
      throw new IllegalArgumentException("client socket can not be null");
    }
    m_socket = clientSocket;
    m_loader = loader;
    m_invoker = invoker;
    m_timeout = timeout;
  }

  /**
   * Does the actual work of serving the client.
   */
  public void run() {
    Thread.currentThread().setContextClassLoader(m_loader);
    try {
      m_socket.setTcpNoDelay(true);
      m_socket.setSoTimeout(m_timeout);
      m_in = new ObjectInputStream(m_socket.getInputStream());
      m_out = new ObjectOutputStream(m_socket.getOutputStream());
    } catch (IOException e) {
      throw new WrappedRuntimeException(e);
    }
    while (m_running) {
      try {
        switch (m_in.read()) {
          case Command.CREATE:
            handleCreateCommand();
            break;
          case Command.INVOKE:
            handleInvocationCommand();
            break;
          case Command.CLOSE:
            m_running = false;
            break;
          default:
            break;
        }
      } catch (Exception e) {
        close();
        throw new WrappedRuntimeException(e);
      }
    }
    close();
  }

  /**
   * Handles the command CREATE.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private void handleCreateCommand() throws IOException,
          ClassNotFoundException,
          InstantiationException,
          IllegalAccessException {
    final String className = (String) m_in.readObject();
    Class klass = Class.forName(className, false, m_loader);
    final Object instance = klass.newInstance();
    final String handle = RemoteProxy.wrapInstance(instance);
    m_out.writeObject(handle);
    m_out.flush();
  }

  /**
   * Handles the command INVOKE.
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void handleInvocationCommand() throws IOException, ClassNotFoundException {
    final Object context = m_in.readObject();
    final String handle = (String) m_in.readObject();
    final String methodName = (String) m_in.readObject();
    final Class[] paramTypes = (Class[]) m_in.readObject();
    final Object[] args = (Object[]) m_in.readObject();
    Object result = null;
    try {
      result = m_invoker.invoke(handle, methodName, paramTypes, args, context);
    } catch (Exception e) {
      result = e;
    }
    m_out.writeObject(result);
    m_out.flush();
  }

  /**
   * Close the input/output streams along with the socket.
   */
  private void close() {
    try {
      if (m_in != null) {
        m_in.close();
      }
      if (m_out != null) {
        m_out.close();
      }
      if (m_socket != null) {
        m_socket.close();
      }
    } catch (IOException e) {
      throw new WrappedRuntimeException(e);
    }
  }
}