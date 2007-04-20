/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatServer {
  public static final String PULSE               = "PULSE";
  public static final String KILL                = "KILL";
  public static final String IS_APP_SERVER_ALIVE = "IS_APP_SERVER_ALIVE";
  public static final String IM_ALIVE            = "IM_ALIVE";
  public static final long   PULSE_INTERVAL      = 15 * 1000;

  private static final List  clientSockets       = Collections.synchronizedList(new ArrayList());

  private ListenThread       listenThread        = new ListenThread();
  private HeartBeatTask      heartBeatTask       = new HeartBeatTask();
  private Timer              timer               = new Timer();

  public HeartBeatServer() {
  //
  }

  public synchronized void start() {
    // start listening thread
    listenThread.start();
    // start hearbeat thread timer
    timer.scheduleAtFixedRate(heartBeatTask, 100, PULSE_INTERVAL);
  }

  public synchronized void shutdown() {
    try {
      listenThread.shutdown();
      listenThread.join();
    } catch (InterruptedException ignored) {
      // nop
    }
    timer.cancel();
    for (Iterator it = clientSockets.iterator(); it.hasNext();) {
      Socket s = (Socket) it.next();
      sendKillSignal(s);
    }
    clientSockets.clear();
  }

  public synchronized boolean anyAppServerAlive() {
    boolean alive = false;
    for (Iterator it = clientSockets.iterator(); it.hasNext();) {
      Socket s = (Socket) it.next();
      alive = alive || pingAppServer(s);
    }
    return alive;
  }

  private boolean pingAppServer(Socket s) {
    boolean alive = false;
    try {
      PrintWriter out = new PrintWriter(s.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
      out.println(IS_APP_SERVER_ALIVE);
      String reply = in.readLine();
      if (reply != null && IM_ALIVE.equals(reply)) {
        alive = true;
      }
    } catch (Throwable e) {
      // ignore - client might have exited or not an appserver
    }

    return alive;
  }

  private void sendKillSignal(Socket s) {
    try {
      PrintWriter out = new PrintWriter(s.getOutputStream(), true);
      out.println(KILL);
    } catch (Throwable e) {
      // ignore - client might have exited
    }
  }

  public synchronized int listeningPort() {
    if (!listenThread.isAlive()) throw new IllegalStateException("Heartbeat server has not started");
    return listenThread.listeningPort();
  }

  private static class ListenThread extends Thread {
    private ServerSocket serverSocket;
    private int          listeningPort;

    public void shutdown() {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
        // nop
      }
    }

    public void run() {
      try {
        serverSocket = new ServerSocket(0);
        listeningPort = serverSocket.getLocalPort();
        Socket clientSocket;
        while ((clientSocket = serverSocket.accept()) != null) {
          System.out.println("Got client...");
          clientSockets.add(clientSocket);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public int listeningPort() {
      return listeningPort;
    }
  }

  private static class HeartBeatTask extends TimerTask {

    public void run() {
      System.out.println("Timer goes off... sending out pulses...");
      for (Iterator it = clientSockets.iterator(); it.hasNext();) {
        Socket s = (Socket) it.next();
        if (sendPulse(s) == false) {
          it.remove();
        }
      }
    }

    public boolean sendPulse(Socket s) {
      try {
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        out.println(PULSE);
        return true;
      } catch (Throwable e) {
        System.err.println("Error sending pusle to client. It will be removed. " + e.getMessage());
        return false;
      }
    }
  }
}
