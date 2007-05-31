package com.tc.net.proxy;

import com.tc.util.StringUtil;

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

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  private int                       roundRobinSequence;
  private ServerSocket              serverSocket;
  private Thread                    acceptThread;
  private volatile boolean          stop;
  private final Set                 connections = new HashSet();
  private final File                logDir;
  private final boolean             logData;
  private boolean                   reuseAddress = false;

  public TCPProxy(int listenPort, InetAddress destHost, int destPort, long delay, boolean logData, File logDir) {
    this(listenPort, new InetSocketAddress[] { new InetSocketAddress(destHost, destPort) }, delay, logData, logDir);
  }

  /**
   * If multiple endpoints are used, then the proxy will round robin between them.
   */
  public TCPProxy(int listenPort, InetSocketAddress[] endpoints, long delay, boolean logData, File logDir) {
    roundRobinSequence = 0;
    debug = false;
    stop = false;
    this.listenPort = listenPort;
    this.endpoints = endpoints;
    this.logData = logData;
    this.logDir = logDir;
    setDelay(delay);
  }
   
  public void setReuseAddress(boolean reuse) {
    reuseAddress = reuse;
  }

  public synchronized void start() throws IOException {
    stop();

    log("Starting listener on port " + listenPort + ", proxying to " + StringUtil.toString(endpoints, ", ", "[", "]")
        + " with " + getDelay() + "ms delay");

    if(!reuseAddress) {
      serverSocket = new ServerSocket(listenPort);
    } else {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);   
      try {
        serverSocket.bind(new InetSocketAddress((InetAddress)null, listenPort), 50);
      } catch(IOException e) {
        serverSocket.close();
        throw e;
      }
    }


    stop = false;

    final TCPProxy ME = this;
    acceptThread = new Thread(new Runnable() {
      public void run() {
        ME.run();
      }
    }, "Accept thread (port " + listenPort + ")");
    acceptThread.start();
  }

  public synchronized void stop() {
    stop = true;

    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (Exception e) {
      log("Error closing serverSocket", e);
    } finally {
      serverSocket = null;
    }

    try {
      if (acceptThread != null) {
        acceptThread.interrupt();

        try {
          acceptThread.join(10000);
        } catch (InterruptedException e) {
          log("Interrupted while join()'ing acceptor thread", e);
        }
      }
    } finally {
      acceptThread = null;
    }

    closeAllConnections();
  }

  synchronized void closeAllConnections() {
    Connection conns[];
    synchronized (connections) {
      conns = (Connection[]) connections.toArray(new Connection[] {});
    }

    for (int i = 0; i < conns.length; i++) {
      try {
        conns[i].close();
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
  }

  private synchronized int getAndIncrementRoundRobinSequence() {
    return roundRobinSequence++;
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
      final Object o = new Object();
      synchronized (o) {
        o.wait();
      }
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
              theProxy.closeAllConnections();
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
      for (int pos = 0; connectedSocket == null && pos < parent.endpoints.length; ++pos) {
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

      final InputStream clientIs = client.getInputStream();
      final OutputStream clientOs = client.getOutputStream();
      final InputStream proxyIs = proxy.getInputStream();
      final OutputStream proxyOs = proxy.getOutputStream();

      parent.register(this);

      clientThread = new Thread(new Runnable() {
        public void run() {
          runHalf(clientIs, proxyOs, true, clientLog);
        }
      }, "Client thread for connection " + client + " proxy to " + proxy);

      proxyThread = new Thread(new Runnable() {
        public void run() {
          runHalf(proxyIs, clientOs, false, proxyLog);
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
          // ignore
        }
      }
    }

    private void runHalf(InputStream src, OutputStream dest, boolean isClientHalf, OutputStream log) {
      byte buffer[] = new byte[4096];

      while (!stopConn) {
        int bytesRead = 0;
        try {
          bytesRead = src.read(buffer);
        } catch (SocketTimeoutException ste) {
          bytesRead = ste.bytesTransferred;
        } catch (IOException ioe) {
          // ignore
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
          close();
          return;
        }

        if (bytesRead > 0) {
          activity();
          delay();

          try {
            dest.write(buffer, 0, bytesRead);
          } catch (IOException ioe) {
            close();
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

    void close() {
      synchronized (closeLock) {
        if (stopConn) return;
        stopConn = true;
      }

      try {
        try {
          if (client != null) client.close();
        } catch (IOException e) {
          // ignore
        }

        try {
          if (proxy != null) proxy.close();
        } catch (IOException e) {
          // ignore
        }

        clientThread.interrupt();
        proxyThread.interrupt();

        try {
          clientThread.join(1000);
        } catch (InterruptedException ie) {
          // ignore
        }
        try {
          proxyThread.join(1000);
        } catch (InterruptedException ie) {
          // ignore
        }
      } finally {
        parent.deregister(this);

        try {
          if (clientLog != null) {
            clientLog.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        try {
          if (proxyLog != null) {
            proxyLog.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
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