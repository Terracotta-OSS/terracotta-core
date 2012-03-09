/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.proxy;

import com.tc.util.StringUtil;
import com.tc.util.concurrent.ThreadUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

/**
 * A simple TCP proxy (with round robin load balancing support) to simulate network delays and help debug network
 * streams.
 */
public class TCPProxy {

  private volatile boolean          debug;
  private long                      delay;
  private final int                 listenPort;
  private final InetSocketAddress[] endpoints;
  private AtomicInteger             roundRobinSequence = new AtomicInteger(0);
  private ServerSocket              serverSocket;
  private Thread                    acceptThread;
  private volatile boolean          stop;
  private final Set                 connections        = new HashSet();
  private final File                logDir;
  private final boolean             logData;
  private boolean                   reuseAddress       = false;

  public TCPProxy(int listenPort, InetAddress destHost, int destPort, long delay, boolean logData, File logDir) {
    this(listenPort, new InetSocketAddress[] { new InetSocketAddress(destHost, destPort) }, delay, logData, logDir);
  }

  /**
   * If multiple endpoints are used, then the proxy will round robin between them.
   */
  public TCPProxy(int listenPort, InetSocketAddress[] endpoints, long delay, boolean logData, File logDir) {
    this.debug = false;
    this.stop = false;
    this.listenPort = listenPort;
    this.endpoints = endpoints;
    this.logData = logData;
    this.logDir = logDir;
    setDelay(delay);

    verifyEndpoints();
  }

  private void verifyEndpoints() {
    for (int i = 0; i < endpoints.length; i++) {
      InetSocketAddress addr = endpoints[i];
      if (addr.getAddress() == null) {
        //
        throw new RuntimeException("Cannot resolve address for host " + addr.getHostName());
      }
    }
  }

  public void setReuseAddress(boolean reuse) {
    reuseAddress = reuse;
  }

  /*
   * Probe if backend is ready for connection. Make sure L2 is ready before calling start().
   */
  public boolean probeBackendConnection() {
    Socket connectedSocket = null;
    for (int pos = 0; pos < endpoints.length; ++pos) {
      final int roundRobinOffset = (pos + roundRobinSequence.get()) % endpoints.length;
      try {
        connectedSocket = new Socket(endpoints[roundRobinOffset].getAddress(), endpoints[roundRobinOffset].getPort());
        break;
      } catch (IOException ioe) {
        //
      }
    }
    if (connectedSocket != null) {
      try {
        connectedSocket.close();
      } catch (Exception e) {
        //
      }
      return (true);
    } else return (false);
  }

  public synchronized void start() throws IOException {

    if (acceptThread != null) {
      log("Stop previous accept thread before start a new one");
      fastStop();
    }

    log("Starting listener on port " + listenPort + ", proxying to " + StringUtil.toString(endpoints, ", ", "[", "]")
        + " with " + getDelay() + "ms delay");

    if (!reuseAddress) {
      serverSocket = new ServerSocket(listenPort);
    } else {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      try {
        serverSocket.bind(new InetSocketAddress(listenPort), 500);
      } catch (IOException e) {
        serverSocket.close();
        throw new RuntimeException("Failed to bind port " + listenPort + " is bad: " + e);
      }
    }

    stop = false;

    final TCPProxy ME = this;
    acceptThread = new Thread(new Runnable() {
      public void run() {
        ME.run();
      }
    }, "Accept thread (port " + listenPort + ")");

    // verify
    int count = 0;
    while (true) {
      try {
        Socket sk = new Socket("localhost", listenPort);
        sk.close();
        break;
      } catch (Exception e) {
        if (++count > 10) { throw new RuntimeException("Listen socket at " + listenPort + " is bad: " + e); }
        log("Listen socket at " + listenPort + " is bad: " + e);

        serverSocket.close();
        ThreadUtil.reallySleep(100);

        log("Rebind listen socket at " + listenPort);
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        try {
          serverSocket.bind(new InetSocketAddress(listenPort), 500);
        } catch (IOException ee) {
          serverSocket.close();
          throw new RuntimeException("Failed to bind port " + listenPort + " is bad: " + ee);
        }

      }
    }
    acceptThread.setDaemon(true);
    acceptThread.start();
  }

  /*
   * Stop without joing dead threads. This is to workaround the issue of taking too long to stop proxy which longer than
   * OOO's L1 reconnect timeout.
   */
  public synchronized void fastStop() {
    subStop(false);
  }

  public synchronized void stop() {
    subStop(true);
  }

  synchronized void subStop(boolean waitDeadThread) {
    stop = true;

    if (acceptThread == null) return;
    acceptThread.interrupt();

    /*
     * Observed on windows-xp. The ServerSocket is still hanging around after "close()", until someone makes a new
     * connection. To make sure the old ServerSocket and accept thread go away for good, fake a connection to the old
     * socket.
     */
    while (true) {
      try {
        Socket sk = new Socket("localhost", listenPort);
        sk.close();
      } catch (Exception x) {
        // that's fine for fake connection.
        break;
      }
      ThreadUtil.reallySleep(100);
    }

    try {
      try {
        acceptThread.join(10000);
      } catch (InterruptedException e) {
        log("Interrupted while join()'ing acceptor thread", e);
        Thread.currentThread().interrupt();
      }
    } finally {
      acceptThread = null;
    }

    closeAllConnections(waitDeadThread);
  }

  public synchronized void closeClientConnections(boolean waitDeadThread, boolean split) {
    Connection conns[];
    synchronized (connections) {
      conns = (Connection[]) connections.toArray(new Connection[] {});
    }

    for (int i = 0; i < conns.length; i++) {
      try {
        conns[i].closeClientHalf(waitDeadThread, split);
      } catch (Exception e) {
        log("Error closing client-side connection " + conns[i].toString(), e);
      }
    }
  }

  synchronized void closeAllConnections(boolean waitDeadThread) {
    Connection conns[];
    synchronized (connections) {
      conns = (Connection[]) connections.toArray(new Connection[] {});
    }

    for (int i = 0; i < conns.length; i++) {
      try {
        conns[i].close(waitDeadThread);
      } catch (Exception e) {
        log("Error closing connection " + conns[i].toString(), e);
      }
    }
  }

  public void toggleDebug() {
    debug = !debug;
  }

  public synchronized long getDelay() {
    return delay;
  }

  public synchronized void setDelay(long newDelay) {
    if (newDelay < 0) { throw new IllegalArgumentException("Delay must be greater than or equal to zero"); }
    delay = newDelay;
  }

  void interrupt() {
    Connection conns[];
    synchronized (connections) {
      conns = (Connection[]) connections.toArray(new Connection[] {});
    }

    for (int i = 0; i < conns.length; i++) {
      conns[i].interrupt();
    }
  }

  private void run() {
    while (!stop) {
      final Socket socket;
      try {
        socket = serverSocket.accept();
      } catch (IOException ioe) {
        log("Accept error " + ioe);
        continue;
      }

      if (Thread.interrupted()) {
        continue;
      }

      if (socket != null) {
        debug("Accepted connection from " + socket.toString());

        try {
          new Connection(socket, this, logData, logDir);
        } catch (IOException ioe) {
          log("Error connecting to any of remote hosts " + StringUtil.toString(endpoints, ", ", "[", "]") + ", "
              + ioe.getMessage());
          try {
            socket.close();
          } catch (IOException clientIOE) {
            log("Unable to close client socket after failing to proxy: " + clientIOE.getMessage());
          }
        }
      }
    }
    try {
      serverSocket.close();
      serverSocket = null;
    } catch (IOException e) {
      // throw e;
      throw new RuntimeException("Unable to close client socket " + e);
    }
  }

  private int getAndIncrementRoundRobinSequence() {
    return roundRobinSequence.incrementAndGet();
  }

  void deregister(Connection connection) {
    synchronized (connections) {
      connections.remove(connection);
    }
  }

  void register(Connection connection) {
    synchronized (connections) {
      connections.add(connection);
    }
  }

  public void status() {
    synchronized (System.err) {
      System.err.println();
      System.err.println("Listening on port : " + listenPort);
      System.err.println("Connection delay  : " + getDelay() + "ms");
      System.err.println("Proxying to       : " + StringUtil.toString(endpoints, ", ", "[", "]"));
      System.err.println("Debug Logging     : " + debug);
      System.err.println("Active connections:");

      Connection conns[];
      synchronized (connections) {
        conns = (Connection[]) connections.toArray(new Connection[] {});
      }

      for (int i = 0; i < conns.length; i++) {
        System.err.println("\t" + i + ": " + conns[i].toString());
      }

      if (conns.length == 0) {
        System.err.println("\tNONE");
      }
    }
  }

  private static void help() {
    synchronized (System.err) {
      System.err.println();
      System.err.println("h       - this help message");
      System.err.println("s       - print proxy status");
      System.err.println("d <num> - adjust the delay time to <num> milliseconds");
      System.err.println("c       - close all active connections");
      System.err.println("l       - toggle debug logging");
      System.err.println("q       - quit (shutdown proxy)");
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    if ((args.length < 2) || (args.length > 3)) {
      usage();
      System.exit(1);
    }

    final int listenPort = Integer.valueOf(args[0]).intValue();
    final String[] endpointStrings = args[1].split(",");
    final InetSocketAddress[] endpoints = new InetSocketAddress[endpointStrings.length];
    for (int pos = 0; pos < endpointStrings.length; ++pos) {
      final int separatorIdx = endpointStrings[pos].indexOf(":");
      endpoints[pos] = new InetSocketAddress(endpointStrings[pos].substring(0, separatorIdx), Integer
          .parseInt(endpointStrings[pos].substring(separatorIdx + 1)));
    }

    long delay = 0;
    if (args.length == 3) {
      delay = (Long.valueOf(args[2]).longValue());
    }

    // If this is set to true then we are in non-interactive mode and don't print a prompt
    final boolean daemonMode = Boolean.getBoolean("daemon");

    final TCPProxy theProxy = new TCPProxy(listenPort, endpoints, delay, false, null);
    theProxy.start();

    if (daemonMode) {
      //block this thread - we don't want to terminate
      Thread.currentThread().join();
    } else {
      try {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String line = "";
        prompt();
        while ((line = stdin.readLine()) != null) {
          line = line.trim();

          if (line.toLowerCase().startsWith("q")) {
            break;
          }

          try {
            if (line.toLowerCase().startsWith("h")) {
              help();
              continue;
            }

            if (line.toLowerCase().startsWith("s")) {
              theProxy.status();
              continue;
            }

            if (line.toLowerCase().startsWith("c")) {
              theProxy.closeAllConnections(true);
              out("all connections closed");
              continue;
            }

            if (line.toLowerCase().startsWith("l")) {
              theProxy.toggleDebug();
              out("debug logging toggled");
              continue;
            }

            if (line.toLowerCase().startsWith("d")) {
              if (line.length() <= 2) {
                out("you must supply a delay value");
                continue;
              }

              try {
                theProxy.setDelay(Long.valueOf(line.substring(2)).longValue());
                theProxy.interrupt();
              } catch (Exception e) {
                out(e);
              }
              continue;
            }
          } catch (Exception e) {
            out(e);
          } finally {
            prompt();
          }
        }
      } finally {
        theProxy.stop();
      }
    }
  }

  private static class Connection {
    private final Socket       client;
    private final Socket       proxy;
    private final TCPProxy     parent;
    private final Thread       clientThread;
    private final Thread       proxyThread;
    private final Object       closeLock     = new Object();
    private volatile boolean   stopConn      = false;
    private final long         connectTime;
    private long               lastActivity;
    private long               clientBytesIn = 0;
    private long               proxyBytesIn  = 0;
    private final OutputStream clientLog;
    private final OutputStream proxyLog;
    private volatile boolean   allowSplit    = false;

    Connection(Socket client, TCPProxy parent, boolean logData, File logDir) throws IOException {
      this.parent = parent;
      this.client = client;
      this.connectTime = System.currentTimeMillis();
      this.lastActivity = this.connectTime;

      // Round robin and try connecting to the next available backend server; this is done by adding an ever increasing
      // sequence number to the offset into the endpoint array (and then mod'ing it so you don't index past the array);
      // this will ensure that you loop through the array in order and start over at the beginning once you reach the
      // end
      IOException lastConnectException = null;
      Socket connectedSocket = null;
      final int roundRobinSequence = parent.getAndIncrementRoundRobinSequence();
      for (int pos = 0; pos < parent.endpoints.length; ++pos) {
        final int roundRobinOffset = (pos + roundRobinSequence) % parent.endpoints.length;
        try {
          connectedSocket = new Socket(parent.endpoints[roundRobinOffset].getAddress(),
                                       parent.endpoints[roundRobinOffset].getPort());
          break;
        } catch (IOException ioe) {
          lastConnectException = ioe;
        }
      }
      if (connectedSocket == null) {
        final IOException ioe = lastConnectException != null ? lastConnectException
            : new IOException("Unable to establish a proxy connection to a back end server: "
                              + StringUtil.toString(parent.endpoints, ",", "[", "]"));
        throw ioe;
      } else {
        proxy = connectedSocket;
      }

      if (logData) {
        final String log = client.getLocalAddress().getHostName().toString() + "." + client.getPort();
        clientLog = new FileOutputStream(new File(logDir, log + ".in"), false);
        proxyLog = new FileOutputStream(new File(logDir, log + ".out"), false);
      } else {
        clientLog = null;
        proxyLog = null;
      }

      proxy.setSoTimeout(100);
      client.setSoTimeout(100);
      // TcpDealy can cause multiple times slower for small packages.
      proxy.setTcpNoDelay(true);
      client.setTcpNoDelay(true);

      final InputStream clientIs = client.getInputStream();
      final OutputStream clientOs = client.getOutputStream();
      final InputStream proxyIs = proxy.getInputStream();
      final OutputStream proxyOs = proxy.getOutputStream();

      parent.register(this);

      clientThread = new Thread(new Runnable() {
        public void run() {
          runHalf(clientIs, proxyOs, true, clientLog, Connection.this.client);
        }
      }, "Client thread for connection " + client + " proxy to " + proxy);

      proxyThread = new Thread(new Runnable() {
        public void run() {
          runHalf(proxyIs, clientOs, false, proxyLog, proxy);
        }
      }, "Proxy thread for connection " + client + " proxy to " + proxy);

      clientThread.start();
      proxyThread.start();
    }

    private synchronized void activity() {
      lastActivity = System.currentTimeMillis();
    }

    private synchronized long getLastActivity() {
      return lastActivity;
    }

    private synchronized void addProxyBytesIn(long bytesIn) {
      this.proxyBytesIn += bytesIn;
    }

    private synchronized void addClientBytesIn(long bytesIn) {
      this.clientBytesIn += bytesIn;
    }

    private synchronized long getProxyBytesIn() {
      return this.proxyBytesIn;
    }

    private synchronized long getClientBytesIn() {
      return this.clientBytesIn;
    }

    public String toString() {
      return "Client: " + client + ", proxy to: " + proxy + ", connect: " + new Date(connectTime) + ", idle: "
             + (System.currentTimeMillis() - getLastActivity()) + ", bytes from client: " + getClientBytesIn()
             + ", bytes from endpoint: " + getProxyBytesIn();
    }

    private void delay() {
      final long sleep = parent.getDelay();

      if (sleep > 0) {
        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void runHalf(InputStream src, OutputStream dest, boolean isClientHalf, OutputStream log, Socket s) {
      byte buffer[] = new byte[4096];

      while (!stopConn) {
        int bytesRead = 0;
        try {
          bytesRead = src.read(buffer);
        } catch (SocketTimeoutException ste) {
          bytesRead = ste.bytesTransferred;
        } catch (IOException ioe) {
          parent.debug("IOException on " + (isClientHalf ? "client" : "proxy") + " connection", ioe);
          return;
        } finally {
          if (bytesRead > 0) {
            try {
              if (log != null) {
                log.write(buffer, 0, bytesRead);
                log.flush();
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            parent.debug("read " + bytesRead + " on " + (isClientHalf ? "client" : "proxy") + " connection");
            if (isClientHalf) addClientBytesIn(bytesRead);
            else addProxyBytesIn(bytesRead);
          }
        }

        if (bytesRead < 0) {
          // delay();
          if (!allowSplit) {
            close(true);
          }
          return;
        }

        if (bytesRead > 0) {
          activity();
          delay();

          try {
            dest.write(buffer, 0, bytesRead);
            dest.flush();
          } catch (IOException ioe) {
            if (!allowSplit) {
              close(true);
            }
            return;
          }
        }
      }
    }

    void interrupt() {
      try {
        clientThread.interrupt();
      } finally {
        proxyThread.interrupt();
      }
    }

    void closeClientHalf(boolean wait, boolean split) {
      this.allowSplit = split;
      try {
        closeHalf(client, clientThread, clientLog, wait);
      } catch (Throwable t) {
        t.printStackTrace();
      }

    }

    void closeProxyHalf(boolean wait, boolean split) {
      this.allowSplit = split;
      try {
        closeHalf(proxy, proxyThread, proxyLog, wait);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    private static void closeHalf(Socket socket, Thread thread, OutputStream out, boolean wait) {
      try {
        try {
          if (socket != null) socket.close();
        } catch (IOException e) {
          // ignore
        }

        thread.interrupt();

        if (wait) {
          try {
            thread.join(1000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      } finally {
        try {
          if (out != null) {
            out.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    void close(boolean waitDeadThread) {
      synchronized (closeLock) {
        if (stopConn) return;
        stopConn = true;
      }

      try {
        closeClientHalf(waitDeadThread, false);
        closeProxyHalf(waitDeadThread, false);
      } finally {
        parent.deregister(this);
      }
    }
  }

  private static void prompt() {
    synchronized (System.err) {
      System.err.print("\nproxy> ");
      System.err.flush();
    }
  }

  private static void out(String message) {
    synchronized (System.err) {
      System.err.println(message);
    }
  }

  private static void out(Throwable t) {
    if (t == null) return;
    synchronized (System.err) {
      t.printStackTrace(System.err);
    }
  }

  private static void log(String message) {
    log(message, null);
  }

  private static void log(String message, Throwable t) {
    synchronized (System.err) {
      System.err.println(new Date() + ": " + message);
      if (t != null) {
        t.printStackTrace(System.err);
      }
    }
  }

  private void debug(String message) {
    debug(message, null);
  }

  private void debug(String message, Throwable t) {
    if (debug) log(message, t);
  }

  private static void usage() {
    System.err.println("usage: TCPProxy <listen port> <endpoint[,endpoint...]> [delay]");
    System.err.println("    <listen port> - The port the proxy should listen on");
    System.err
        .println("       <endpoint> - Comma separated list of 1 or more <host>:<port> pairs to round robin requests to");
    System.err.println("          [delay] - Millisecond delay between network data (optional, default: 0)");
  }

}
