/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;

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
public class LinkedJavaProcessPollingAgent {

  public static final int        NORMAL_HEARTBEAT_INTERVAL = 60 * 1000;
  public static final byte[]     HEARTBEAT_DATA            = "Heartbeat".getBytes();
  private static final int       MAX_HEARTBEAT_DELAY       = 4 * NORMAL_HEARTBEAT_INTERVAL;
  public static final int        EXIT_CODE                 = 42;
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
   */
  public static synchronized void startClientWatchdogService(int pingPort, String childClass) {
    if (client == null) {
      client = new PingThread(pingPort, childClass);
      client.start();
      log("Child-process watchdog for class " + childClass + " monitoring server on port: " + pingPort);
    }
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
    private final int    pingPort;
    private final String forClass;

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
        port = toServer.getLocalPort();
        InputStream inStream = toServer.getInputStream();

        toServer.setSoTimeout(MAX_HEARTBEAT_DELAY);
        byte[] data = new byte[HEARTBEAT_DATA.length];

        while (true) {
          int read = 0;

          long startTime = System.currentTimeMillis();
          while (read < data.length && System.currentTimeMillis() < (startTime + MAX_HEARTBEAT_DELAY)) {
            int thisRead = inStream.read(data, read, data.length - read);
            if (thisRead < 0) break;
            read += thisRead;
          }

          if (read != data.length) throw new IOException("Only got " + read + " bytes, not " + data.length
                                                         + " bytes, within " + (System.currentTimeMillis() - startTime)
                                                         + " ms.");

          for (int i = 0; i < data.length; ++i) {
            if (data[i] != HEARTBEAT_DATA[i]) throw new IOException("Got invalid data; byte " + i
                                                                    + " should have been " + HEARTBEAT_DATA[i]
                                                                    + ", but was " + data[i] + ".");
          }

          log("Read heartbeat for process with main class " + this.forClass + ".");
        }
      } catch (Exception e) {
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
        log("Didn't get heartbeat for at least " + MAX_HEARTBEAT_DELAY + " milliseconds. Killing self (port " + port
            + ").");
        System.exit(EXIT_CODE);
      } finally {
        log("Ping thread exiting port (" + port + ")");
      }
    }
  }

  private static class HeartbeatServer extends Thread {
    private int port;

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
          new HeartbeatThread(sock).start();
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
    private final Socket socket;
    private final int    port;

    public HeartbeatThread(Socket socket) {
      if (socket == null) throw new NullPointerException();

      this.socket = socket;
      this.port = socket.getPort();
      this.setDaemon(true);
    }

    public void run() {
      try {
        OutputStream out = this.socket.getOutputStream();

        while (true) {
          out.write(HEARTBEAT_DATA);
          out.flush();
          // System.err.println("Wrote heartbeat to client.");

          reallySleep(NORMAL_HEARTBEAT_INTERVAL);
        }
      } catch (Exception e) {
        log("Heartbeat thread for child process (port " + port + ") got exception");
        log(e.getClass() + ": " + Arrays.asList(e.getStackTrace()));
      } finally {
        log("Heartbeat thread for child process (port " + port + ") terminating.");
      }
    }
  }
}
