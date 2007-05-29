/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HeartBeatClient extends Thread {
  private Socket              socket;
  private boolean             isAppServer       = false;
  private static final int    HEARTBEAT_TIMEOUT = HeartBeatServer.PULSE_INTERVAL + 5000;
  private String              clientName;
  private static final Logger logger            = Logger.getLogger(HeartBeatClient.class);

  public HeartBeatClient(int listenPort, String clientName, boolean isAppServer) {
    logger.setAdditivity(false);
    this.isAppServer = isAppServer;
    this.clientName = clientName;
    try {
      socket = new Socket("localhost", listenPort);
      socket.setSoTimeout(HEARTBEAT_TIMEOUT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      // introduce myself to the server
      // sending clientName
      out.println(clientName + ":" + socket.getLocalPort());

      while (true) {
        // will time out if it didn't get any pulse from server
        String signal = in.readLine();
        if (signal == null) {
          throw new Exception("Null signal");
        } else if (HeartBeatServer.PULSE.equals(signal)) {
          logger.info("Received pulse from heartbeat server, port " + socket.getLocalPort());
        } else if (HeartBeatServer.KILL.equals(signal)) {
          logger.info("Received KILL from heartbeat server. Killing self.");
          System.exit(1);
        } else if (HeartBeatServer.IS_APP_SERVER_ALIVE.equals(signal)) {
          logger.info("Received IS_APP_SERVER_ALIVE from heartbeat server. ");
          if (isAppServer) {
            out.println(HeartBeatServer.IM_ALIVE);
            logger.info("  responded: IM_ALIVE");
          } else {
            out.println("NOT_AN_APP_SERVER");
            logger.info("  responded: NOT_AN_APP_SERVER");
          }
        } else {
          throw new Exception("Unknown signal");
        }
      }

    } catch (Throwable e) {
      logger.info("Caught exception in heartbeat client. Killing self. " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (Exception ignored) {
      }
      System.exit(1);
    }
  }

}
