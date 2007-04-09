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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
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

  private static final int       MAX_HEARTBEAT_DELAY       = 2 * NORMAL_HEARTBEAT_INTERVAL;
  private static final int       EXIT_CODE                 = 42;
  private static HeartbeatServer server                    = null;
  private static PingThread      client                    = null;

  public static synchronized void startHeartBeatServer() {
    if (server == null) {
      server = new HeartbeatServer();
      server.start();
    }
  }

  public static synchronized boolean isServerRunning() {
    if (server == null) { return false; }
    return server.isRunning();
  }

  /**
   * Creates a server thread in the parent process posting a periodic heartbeat.
   * 
   * @return server port - must be passed to {@link startClientWatchdogService()}
   */
  public static synchronized int getChildProcessHeartbeatServerPort() {
    if (server == null) throw new IllegalStateException("Heartbeat Server has not started!");
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
      System.err.println("Child-process watchdog for class " + childClass + " monitoring server on port: " + pingPort);
    }
  }

  public static synchronized void startClientWatchdogService(int pingPort, String childClass) {
    startClientWatchdogService(pingPort, childClass, false);
  }

  /**
   * Shutdown heartbeat server and
   * send a kill signal to child processes
   */
  public static synchronized void shutdown() {
    server.shutdown();
    server = null;
  }

  public static boolean isAnyAppServerAlive() {
    return server.isAnyAppServerAlive();
  }

  private static synchronized void log(String msg) {
    System.out.println("LJP: [" + new Date() + "] " + msg);
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
      Socket toServer = null;
      try {
        toServer = new Socket("localhost", this.pingPort);
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
            out.println(forClass);
            out.flush();
          } else {
            throw new Exception("Doesn't recognize data: " + data);
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
        if (toServer != null) {
          try {
            toServer.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        System.exit(EXIT_CODE);
      }
    }
  }

  private static class HeartbeatServer extends Thread {
    private int              port;
    private List             heartBeatThreads = new ArrayList();
    private ServerSocket     serverSocket     = null;
    private boolean          running          = false;
    private volatile boolean isStarting       = false;

    public HeartbeatServer() {
      this.port = -1;
      this.setDaemon(true);
    }

    public synchronized boolean isAnyAppServerAlive() {
      boolean foundAlive = false;
      synchronized (heartBeatThreads) {
        for (Iterator it = heartBeatThreads.iterator(); it.hasNext();) {
          HeartbeatThread hb = (HeartbeatThread) it.next();
          boolean aliveStatus = hb.isAppServerAlive();
          log("pinging: " + hb.port + ", alive? = " + aliveStatus);
          foundAlive = foundAlive || aliveStatus;
        }
      }
      return foundAlive;
    }

    public synchronized int getPort() {
      while (port == -1) {
        try {
          this.wait(5000);
        } catch (InterruptedException e) {
          throw new RuntimeException("Server might have not started yet", e);
        }
      }
      return port;
    }

    public synchronized boolean isRunning() {
      while (isStarting) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      return running;
    }

    public synchronized void setRunning(boolean status) {
      running = status;
    }

    private synchronized void shutdown() {
      setRunning(false);
      
      if (serverSocket != null) {
        try {
          serverSocket.close(); // this effectively interrupts the thread and force it to exit
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      
      synchronized (heartBeatThreads) {
        HeartbeatThread ht;
        for (Iterator i = heartBeatThreads.iterator(); i.hasNext();) {
          ht = (HeartbeatThread) i.next();
          ht.sendKillSignal();
        }
      }
    }

    public void run() {

      try {
        isStarting = true;
        synchronized (this) {
          serverSocket = new ServerSocket(0);
          this.port = serverSocket.getLocalPort();
          setRunning(true);
          isStarting = false;
          this.notifyAll();
        }

        System.err.println("Child-process heartbeat server started on port: " + port);

        while (true) {
          Socket sock = serverSocket.accept();
          log("Got heartbeat connection from client; starting heartbeat.");
          synchronized (heartBeatThreads) {
            HeartbeatThread hbt = new HeartbeatThread(sock);
            heartBeatThreads.add(hbt);
            hbt.start();
          }
        }
      } catch (Exception e) {
        if (!running)
          log("Heartbeat server was shutdown.");
        else
          log("Got expcetion in heartbeat server: " + e.getMessage());
      } finally {
        setRunning(false);
        log("Heartbeat server terminated.");
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
        this.socket.setSoTimeout(MAX_HEARTBEAT_DELAY);
      } catch (SocketException e) {
        throw new RuntimeException(e);
      }
      this.port = socket.getPort();
      this.setDaemon(true);
    }

    public synchronized void sendKillSignal() {
      try {
        out.println(SHUTDOWN);
        out.flush();
      } catch (Exception e) {
        log("Socket Exception: client may have already shutdown.");
      }
    }

    public boolean isAppServerAlive() {
      if (socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) return false;

      try {
        log("sending ARE_YOU_ALIVE...");
        out.println(ARE_YOU_ALIVE);
        out.flush();
        String result = in.readLine();
        log("received: " + result);
        if (result == null || result.endsWith("TCServerMain")) {
          // not an apserver
          return false;
        } else {
          return true;
        }
      } catch (IOException e) {
        log("got exception: " + e.getMessage());
        return false;
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
        try {
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
