/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HeartBeatClient extends Thread {
  private Socket  socket;
  private boolean isAppServer = false;

  public HeartBeatClient(int listenPort, boolean isAppServer) {
    this.isAppServer = isAppServer;
    try {
      socket = new Socket("localhost", listenPort);
      socket.setSoTimeout(HeartBeatServer.PULSE_INTERVAL + 1000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      while (true) {
        // will time out if it didn't get any pulse from server
        String signal = in.readLine();

        if (HeartBeatServer.PULSE.equals(signal)) {
          System.out.println("Received pulse from server.");
        } else if (HeartBeatServer.KILL.equals(signal)) {
          System.out.println("Received KILL from server. Killing self.");
          System.exit(1);
        } else if (HeartBeatServer.IS_APP_SERVER_ALIVE.equals(signal)) {
          System.out.println("Received app server ping.");
          if (isAppServer) {
            out.println(HeartBeatServer.IM_ALIVE);
          } else {
            out.println("NOT_AN_APP_SERVER");
          }
        }
      }

    } catch (Throwable e) {
      System.err.println("Caught exception in heartbeat client. Killing self." + e.getMessage());
      System.exit(1);
    }
  }

}
