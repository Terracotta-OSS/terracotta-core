/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeartBeatClient extends Thread {
  private static final int  HEARTBEAT_TIMEOUT = HeartBeatServer.PULSE_INTERVAL + 5000;
  private static DateFormat DATEFORMAT        = new SimpleDateFormat("HH:mm:ss.SSS");

  private Socket            socket;
  private boolean           isAppServer       = false;
  private String            clientName;
  private int               missedPulse       = 0;

  public HeartBeatClient(int listenPort, String clientName, boolean isAppServer) {
    this.isAppServer = isAppServer;
    this.clientName = clientName;
    try {
      socket = new Socket("localhost", listenPort);
      socket.setSoTimeout(HEARTBEAT_TIMEOUT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void log(String message) {
    System.out.println(DATEFORMAT.format(new Date()) + " - HeartBeatClient: " + message);
  }

  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      // introduce myself to the server
      // sending clientName
      out.println(clientName + ":" + socket.getLocalPort());

      while (true) {
        try {
          // will time out if it didn't get any pulse from server
          String signal = in.readLine();
          if (signal == null) {
            throw new Exception("Null signal");
          } else if (HeartBeatServer.PULSE.equals(signal)) {
            log("Received pulse from heartbeat server, port " + socket.getLocalPort());
            out.println(signal);
          } else if (HeartBeatServer.KILL.equals(signal)) {
            log("Received KILL from heartbeat server. Killing self.");
            System.exit(1);
          } else if (HeartBeatServer.IS_APP_SERVER_ALIVE.equals(signal)) {
            log("Received IS_APP_SERVER_ALIVE from heartbeat server. ");
            if (isAppServer) {
              out.println(HeartBeatServer.IM_ALIVE);
              log("  responded: IM_ALIVE");
            } else {
              out.println("NOT_AN_APP_SERVER");
              log("  responded: NOT_AN_APP_SERVER");
            }
          } else {
            throw new Exception("Unknown signal");
          }
        } catch (SocketTimeoutException toe) {
          log("No pulse received for " + (HeartBeatServer.PULSE_INTERVAL/1000) + " seconds");
          log("Missed pulse count: " + missedPulse++);
          if (missedPulse > 3) {
            throw new Exception("Missing 3 pulse from HeartBeatServer");
          }
        }
      }   
    } catch (Throwable e) {
      log("Caught exception in heartbeat client. Killing self.");
      e.printStackTrace();
    } finally {
      try {
        socket.close();
      } catch (Exception ignored) {
      }
      System.exit(1);
    }
  }

}
