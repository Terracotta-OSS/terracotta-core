/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates a connection between a parent java process and it's child process.
 * <p>
 * SCENARIO: When a parent process creates a child the parent may monitor the child using various hooks. If the parent
 * process itself dies unexpectedly (kill -9) the child process will remain alive unaware of it's parents fate.
 * <p>
 * This class is able to create a server thread on the parent and a watchdog thread on the child which periodically
 * pages it's parent to make sure it's still alive. If the parent's heartbeat flatlines, the child's watchdog thread
 * will call <tt>System.exit(0)</tt>.
 */
public final class LinkedJavaProcessPollingAgent {

  private static final int       NORMAL_HEARTBEAT_INTERVAL = 60 * 1000;

  private static final String    HEARTBEAT                 = "HEARTBEAT";
  private static final String    SHUTDOWN                  = "SHUTDOWN";
  private static final String    ARE_YOU_ALIVE             = "ARE_YOU_ALIVE";
  private static final String    I_AM_ALIVE                = "I_AM_ALIVE";

  private static final int       MAX_HEARTBEAT_DELAY       = 4 * NORMAL_HEARTBEAT_INTERVAL;
  private static final int       EXIT_CODE                 = 42;
  private static HeartbeatServer server                    = null;
  private static PingThread      client                    = null;

  /**
   * Creates a server thread in the parent process posting a periodic heartbeat.
   * 
   * @return server port - must be passed to {@link startClientWatchdogService()}
   */
  public static synchronized int getChildProcessHeartbeatServerPort() {
    if (server == null) {
      server = new HeartbeatServer();
      server.start();
      System.err.println("Child-process heartbeat server started on port: " + server.getPort());
    }
    return server.getPort();
  }

  /**
   * Creates a watchdog service thread in the child process which receives a heartbeart from the parent process.
   * 
   * @param pingPort - this must come from {@link getChildProcessHeartbeatServerPort()}
   * @param childClass - used for debugging
   * @param honorShutdownMsg - false, will ignore the destroy() method and keep this client alive after the shutdown
   *        message is broadcast
   */
  public static synchronized void startClientWatchdogService(int pingPort, String childClass, boolean honorShutdownMsg) {
    if (client == null) {
      client = new PingThread(pingPort, childClass, honorShutdownMsg);
      client.start();
      log("Child-process watchdog for class " + childClass + " monitoring server on port: " + pingPort);
    }
  }

  public static synchronized void startClientWatchdogService(int pingPort, String childClass) {
    startClientWatchdogService(pingPort, childClass, false);
  }

  /**
   * Sends a kill signal to the child process
   */
  public static synchronized void destroy() {
    server.shutdown();
  }

  public static synchronized boolean isAnyAlive() {
    return server.isAnyAlive();
  }

  private static void log(String msg) {
    System.err.println("LJP: [" + new Date() + "] " + msg);
  }

  static void reallySleep(long millis) {
    try {
      long millisLeft = millis;
      while (millisLeft > 0) {
        long start = System.currentTimeMillis();
        Thread.sleep(millisLeft);
        millisLeft -= System.currentTimeMillis() - start;
      }
    } catch (InterruptedException ie) {
      throw new AssertionError(ie);
    }
  }

  private static class PingThread extends Thread {
    private final int      pingPort;
    private final String   forClass;
    private boolean        honorShutdownMsg;
    private BufferedReader in;
    private PrintWriter    out;

    public PingThread(int port, String forClass, boolean honorShutdownMsg) {
      this(port, forClass);
      this.honorShutdownMsg = honorShutdownMsg;
    }

    public PingThread(int port, String forClass) {
      if (!(port > 0)) throw new RuntimeException("Port not > 0");
      if (forClass.trim().length() == 0) throw new RuntimeException("blank argument");

      this.pingPort = port;
      this.forClass = forClass;

      this.setDaemon(true);
    }

    public void run() {
      int port = -1;
      try {
        Socket toServer = new Socket("localhost", this.pingPort);
        toServer.setSoTimeout(MAX_HEARTBEAT_DELAY);

        port = toServer.getLocalPort();

        in = new BufferedReader(new InputStreamReader(toServer.getInputStream()));
        out = new PrintWriter(toServer.getOutputStream(), true);

        while (true) {
          long start = System.currentTimeMillis();

          String data = in.readLine();
          if (HEARTBEAT.equals(data)) {
            log("Got heartbeat for main class " + this.forClass);
          } else if (SHUTDOWN.equals(data)) {
            if (!honorShutdownMsg) continue;
            log("Client received shutdown message from server. Shutting Down...");
            System.exit(0);
          } else if (ARE_YOU_ALIVE.equals(data)) {
            out.println(I_AM_ALIVE);
          }

          long elapsed = System.currentTimeMillis() - start;
          if (elapsed > MAX_HEARTBEAT_DELAY) { throw new Exception("Client took too long to response."); }
        }
      } catch (Exception e) {
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
        log("Didn't get heartbeat for at least " + MAX_HEARTBEAT_DELAY + " milliseconds. Killing self (port " + port
            + ").");
      } finally {
        log("Ping thread exiting port (" + port + ")");
        System.exit(EXIT_CODE);
      }
    }
  }

  private static class HeartbeatServer extends Thread {
    private int  port;
    private List heartBeatThreads = new LinkedList();

    public HeartbeatServer() {
      this.port = -1;
      this.setDaemon(true);
    }

    public synchronized int getPort() {
      while (this.port < 0) {
        try {
          this.wait();
        } catch (InterruptedException ie) {
          // whatever
        }
      }
      return this.port;
    }

    private synchronized void shutdown() {
      synchronized (heartBeatThreads) {
        HeartbeatThread ht;
        try {
          for (Iterator i = heartBeatThreads.iterator(); i.hasNext();) {
            ht = (HeartbeatThread) i.next();
            ht.shutdown();
          }
        } catch (IOException e) {
          log("Heartbeat server couldn't shutdown clients -- they may have shutdown anyway");
          log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
        }
      }
    }

    private synchronized boolean isAnyAlive() {
      boolean isAnyAlive = false;
      try {
        synchronized (heartBeatThreads) {
          for (Iterator it = heartBeatThreads.iterator(); it.hasNext();) {
            HeartbeatThread ht = (HeartbeatThread) it.next();
            if (!ht.isAlive() || ht.isInterrupted()) continue;
            isAnyAlive = isAnyAlive || ht.ping();
          }
        }
      } catch (Throwable e) {
        isAnyAlive = false;
      }

      return isAnyAlive;
    }

    public void run() {
      try {
        ServerSocket serverSocket = new ServerSocket(0);

        synchronized (this) {
          this.port = serverSocket.getLocalPort();
          this.notifyAll();
        }

        while (true) {
          Socket sock = serverSocket.accept();
          System.err.println("Got heartbeat connection from client; starting heartbeat.");
          synchronized (heartBeatThreads) {
            HeartbeatThread hbt = new HeartbeatThread(sock);
            heartBeatThreads.add(hbt);
            hbt.start();
          }
        }
      } catch (Exception e) {
        log("Heartbeat server couldn't listen or accept a connection");
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
      } finally {
        log("Heartbeat server terminating.");
      }
    }
  }

  private static class HeartbeatThread extends Thread {
    private final Socket   socket;
    private final int      port;

    private BufferedReader in;
    private PrintWriter    out;

    public HeartbeatThread(Socket socket) {
      if (socket == null) throw new NullPointerException();
      this.socket = socket;
      try {
        this.socket.setSoTimeout(2 * NORMAL_HEARTBEAT_INTERVAL);
      } catch (SocketException e) {
        throw new RuntimeException(e);
      }
      this.port = socket.getPort();
      this.setDaemon(true);
    }

    public boolean ping() {
      boolean status = false;

      if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) { return status; }

      try {
        out.write(ARE_YOU_ALIVE);
        out.flush();
        status = in.readLine().equals(I_AM_ALIVE);
      } catch (Exception e) {
        status = false;
      }

      return status;
    }

    public synchronized void shutdown() throws IOException {
      try {
        out.println(SHUTDOWN);
        out.flush();
      } catch (Exception e) {
        log("Socket Exception: client may have already shutdown.");
      }
    }

    public void run() {
      try {
        out = new PrintWriter(this.socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

        while (true) {
          synchronized (this) {
            if (!socket.isOutputShutdown()) {
              out.println(HEARTBEAT);
              out.flush();
            }
          }
          reallySleep(NORMAL_HEARTBEAT_INTERVAL);
        }
      } catch (SocketException e) {
        log("Socket Exception: client may have already shutdown.");
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
      } catch (Exception e) {
        log("Heartbeat thread for child process (port " + port + ") got exception");
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
      } finally {
        log("Heartbeat thread for child process (port " + port + ") terminating.");
      }
    }
  }
}
