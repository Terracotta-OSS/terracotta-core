/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Netstat {

  public static void main(String[] args) {
    for (SocketConnection sc : getEstablishedTcpConnections()) {
      System.out.println(sc);
    }
  }

  private static final Pattern PATTERN = Pattern
                                           .compile("^[^\\d]*(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\.\\:](\\d+)[^\\d]*(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\.\\:](\\d+).*$");

  public static List<SocketConnection> getEstablishedTcpConnections() {
    List<SocketConnection> connections = new ArrayList<Netstat.SocketConnection>();

    try {
      Result result = Exec.execute(new String[] { "netstat", "-n" });
      if (result.getExitCode() != 0) { throw new RuntimeException(result.toString()); }

      BufferedReader reader = new BufferedReader(new StringReader(result.getStdout()));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.toLowerCase().startsWith("udp")) {
          continue;
        }

        if (line.endsWith("ESTABLISHED")) {
          Matcher matcher = PATTERN.matcher(line);
          if (matcher.matches()) {
            String localAddr = matcher.group(1);
            int localPort = Integer.parseInt(matcher.group(2));
            String remoteAddr = matcher.group(3);
            int remotePort = Integer.parseInt(matcher.group(4));
            connections.add(new SocketConnection(localAddr, localPort, remoteAddr, remotePort));
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return connections;
  }

  public static class SocketConnection {
    private final String localAddr;
    private final int    localPort;
    private final String remoteAddr;
    private final int    remotePort;

    public SocketConnection(String localAddr, int localPort, String remoteAddr, int remotePort) {
      this.localAddr = localAddr;
      this.localPort = localPort;
      this.remoteAddr = remoteAddr;
      this.remotePort = remotePort;
    }

    public String getLocalAddr() {
      return localAddr;
    }

    public int getLocalPort() {
      return localPort;
    }

    public String getRemoteAddr() {
      return remoteAddr;
    }

    public int getRemotePort() {
      return remotePort;
    }

    @Override
    public String toString() {
      return "local(" + localAddr + ":" + localPort + ") remote(" + remoteAddr + ":" + remotePort + ")";
    }
  }

}
