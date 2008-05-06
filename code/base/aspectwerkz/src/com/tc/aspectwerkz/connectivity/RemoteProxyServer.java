/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.connectivity;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Producer that listens to a specified port for client requests. <p/>The implementation is based on sockets. <p/>The
 * invoker spawns a specified number of listener threads in which each one of these spawns a new RemoteProxyServerThread
 * for each client request that comes in. <p/>Uses a thread pool from util.concurrent.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class RemoteProxyServer implements Runnable {
  private static String HOST_NAME;

  private static int PORT;

  private static boolean BOUNDED_THREAD_POOL;

  private static boolean LISTENER_THREAD_RUN_AS_DAEMON;

  private static int BACKLOG;

  private static int NUM_LISTENER_THREADS;

  private static int LISTENER_THREAD_PRIORITY = Thread.NORM_PRIORITY;

  private static int CLIENT_THREAD_TIMEOUT;

  private static int THREAD_POOL_MAX_SIZE;

  private static int THREAD_POOL_MIN_SIZE;

  private static int THREAD_POOL_INIT_SIZE;

  private static int THREAD_POOL_KEEP_ALIVE_TIME;

  private static boolean THREAD_POOL_WAIT_WHEN_BLOCKED;

  /**
   * Initalize the server properties.
   */
  static {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(System.getProperty("aspectwerkz.resource.bundle")));
    } catch (Exception e) {
      System.out.println("no aspectwerkz resource bundle found on classpath, using defaults");

      // ignore, use defaults
    }
    String property = properties.getProperty("remote.server.hostname");
    if (property == null) {
      HOST_NAME = property;
    } else {
      HOST_NAME = property;
    }
    property = properties.getProperty("remote.server.port");
    if (property == null) {
      PORT = 7777;
    } else {
      PORT = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.listener.threads.backlog");
    if (property == null) {
      BACKLOG = 200;
    } else {
      BACKLOG = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.listener.threads.nr");
    if (property == null) {
      NUM_LISTENER_THREADS = 10;
    } else {
      NUM_LISTENER_THREADS = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.client.threads.timeout");
    if (property == null) {
      CLIENT_THREAD_TIMEOUT = 60000;
    } else {
      CLIENT_THREAD_TIMEOUT = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.thread.pool.max.size");
    if (property == null) {
      THREAD_POOL_MAX_SIZE = 100;
    } else {
      THREAD_POOL_MAX_SIZE = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.thread.pool.min.size");
    if (property == null) {
      THREAD_POOL_MIN_SIZE = 10;
    } else {
      THREAD_POOL_MIN_SIZE = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.thread.pool.init.size");
    if (property == null) {
      THREAD_POOL_INIT_SIZE = 10;
    } else {
      THREAD_POOL_INIT_SIZE = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.thread.pool.keep.alive.time");
    if (property == null) {
      THREAD_POOL_KEEP_ALIVE_TIME = 300000;
    } else {
      THREAD_POOL_KEEP_ALIVE_TIME = Integer.parseInt(property);
    }
    property = properties.getProperty("remote.server.thread.pool.type");
    if ((property != null) && property.equals("dynamic")) {
      BOUNDED_THREAD_POOL = false;
    } else {
      BOUNDED_THREAD_POOL = true;
    }
    property = properties.getProperty("remote.server.listener.threads.run.as.daemon");
    if ((property != null) && property.equals("true")) {
      LISTENER_THREAD_RUN_AS_DAEMON = true;
    } else {
      LISTENER_THREAD_RUN_AS_DAEMON = false;
    }
    property = properties.getProperty("remote.server.thread.pool.wait.when.blocked");
    if ((property != null) && property.equals("true")) {
      THREAD_POOL_WAIT_WHEN_BLOCKED = true;
    } else {
      THREAD_POOL_WAIT_WHEN_BLOCKED = false;
    }
  }

  /**
   * The server socket.
   */
  private ServerSocket m_serverSocket = null;

  /**
   * All listener threads.
   */
  private Thread[] m_listenerThreads = null;

  /**
   * The thread pool.
   */
  private PooledExecutor m_threadPool = null;

  /**
   * The class loader to use.
   */
  private ClassLoader m_loader = null;

  /**
   * The invoker instance.
   */
  private Invoker m_invoker = null;

  /**
   * Marks the server as running.
   */
  private boolean m_running = true;

  /**
   * Starts a server object and starts listening for client access.
   *
   * @param loader  the classloader to use
   * @param invoker the invoker that makes the method invocation in the client thread
   */
  public RemoteProxyServer(final ClassLoader loader, final Invoker invoker) {
    m_invoker = invoker;
    m_loader = loader;
  }

  /**
   * Starts up the proxy server.
   */
  public void start() {
    m_running = true;
    try {
      InetAddress bindAddress = InetAddress.getByName(HOST_NAME);
      m_serverSocket = new ServerSocket(PORT, BACKLOG, bindAddress);
      if (BOUNDED_THREAD_POOL) {
        createBoundedThreadPool(
                THREAD_POOL_MAX_SIZE,
                THREAD_POOL_MIN_SIZE,
                THREAD_POOL_INIT_SIZE,
                THREAD_POOL_KEEP_ALIVE_TIME,
                THREAD_POOL_WAIT_WHEN_BLOCKED
        );
      } else {
        createDynamicThreadPool(THREAD_POOL_MIN_SIZE, THREAD_POOL_INIT_SIZE, THREAD_POOL_KEEP_ALIVE_TIME);
      }
      m_listenerThreads = new Thread[NUM_LISTENER_THREADS];
      for (int i = 0; i < NUM_LISTENER_THREADS; i++) {
        m_listenerThreads[i] = new Thread(this);
        m_listenerThreads[i].setName("AspectWerkz::Listener " + (i + 1));
        m_listenerThreads[i].setDaemon(LISTENER_THREAD_RUN_AS_DAEMON);
        m_listenerThreads[i].setPriority(LISTENER_THREAD_PRIORITY);
        m_listenerThreads[i].start();
      }
    } catch (IOException e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Stops the socket proxy server.
   */
  public void stop() {
    m_running = false;
    for (int i = 0; i < NUM_LISTENER_THREADS; i++) {
      m_listenerThreads[i].interrupt();
    }
    m_threadPool.shutdownNow();
  }

  /**
   * Does the actual work of listening for a client request and spawns a new RemoteProxyServerThread to serve the
   * client.
   */
  public void run() {
    try {
      while (m_running) {
        final Socket clientSocket = m_serverSocket.accept();
        synchronized (m_threadPool) {
          m_threadPool.execute(
                  new RemoteProxyServerThread(
                          clientSocket,
                          m_loader,
                          m_invoker,
                          CLIENT_THREAD_TIMEOUT
                  )
          );
        }
      }
      m_serverSocket.close();
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Creates a new bounded thread pool.
   *
   * @param threadPoolMaxSize
   * @param threadPoolMinSize
   * @param threadPoolInitSize
   * @param keepAliveTime
   * @param waitWhenBlocked
   */
  private void createBoundedThreadPool(final int threadPoolMaxSize,
                                       final int threadPoolMinSize,
                                       final int threadPoolInitSize,
                                       final int keepAliveTime,
                                       final boolean waitWhenBlocked) {
    m_threadPool = new PooledExecutor(new BoundedBuffer(threadPoolInitSize), threadPoolMaxSize);
    m_threadPool.setKeepAliveTime(keepAliveTime);
    m_threadPool.createThreads(threadPoolInitSize);
    m_threadPool.setMinimumPoolSize(threadPoolMinSize);
    if (waitWhenBlocked) {
      m_threadPool.waitWhenBlocked();
    }
  }

  /**
   * Creates a new dynamic thread pool
   *
   * @param threadPoolMinSize
   * @param threadPoolInitSize
   * @param keepAliveTime
   */
  private void createDynamicThreadPool(final int threadPoolMinSize,
                                       final int threadPoolInitSize,
                                       final int keepAliveTime) {
    m_threadPool = new PooledExecutor(new LinkedQueue());
    m_threadPool.setKeepAliveTime(keepAliveTime);
    m_threadPool.createThreads(threadPoolInitSize);
    m_threadPool.setMinimumPoolSize(threadPoolMinSize);
  }
}