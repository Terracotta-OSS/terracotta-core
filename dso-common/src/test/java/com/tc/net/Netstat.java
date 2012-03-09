/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.InetAddress;
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

  private static final Pattern PATTERN  = Pattern
                                            .compile("^.*?(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\.\\:](\\d+)\\s+(::ffff:)?(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\.\\:](\\d+).*$");

  private static final Netstat INSTANCE = new Netstat();

  public static List<SocketConnection> getEstablishedTcpConnections() {
    return INSTANCE.listEstablishedTcpConnections();
  }

  List<SocketConnection> listEstablishedTcpConnections() {
    List<SocketConnection> connections = new ArrayList<Netstat.SocketConnection>();

    try {
      String netstat = executeNetstat();
      BufferedReader reader = new BufferedReader(new StringReader(netstat));
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
            String remoteAddr = matcher.group(4);
            int remotePort = Integer.parseInt(matcher.group(5));

            connections.add(new SocketConnection(InetAddress.getByName(localAddr), localPort, InetAddress
                .getByName(remoteAddr), remotePort));
          } else {
            System.out.println("NO MATCH: " + line);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return connections;
  }

  String executeNetstat() throws Exception {
    Result result = Exec.execute(new String[] { "netstat", "-n" });
    if (result.getExitCode() != 0) { throw new RuntimeException(result.toString()); }

    return result.getStdout();
  }

  public static class SocketConnection {
    private final InetAddress localAddr;
    private final int         localPort;
    private final InetAddress remoteAddr;
    private final int         remotePort;

    public SocketConnection(InetAddress localAddr, int localPort, InetAddress remoteAddr, int remotePort) {
      this.localAddr = localAddr;
      this.localPort = localPort;
      this.remoteAddr = remoteAddr;
      this.remotePort = remotePort;
    }

    public InetAddress getLocalAddr() {
      return localAddr;
    }

    public int getLocalPort() {
      return localPort;
    }

    public InetAddress getRemoteAddr() {
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
