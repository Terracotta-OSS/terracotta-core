/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import com.tc.net.EphemeralPorts;
import com.tc.net.EphemeralPorts.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class PortChooser {
  private static final Logger LOGGER = LoggerFactory.getLogger(PortChooser.class);

  public static final int           MAX          = 65535;

  private static final Object       VM_WIDE_LOCK = (PortChooser.class.getName() + "LOCK").intern();
  private static final Set<Integer> chosen       = new HashSet<Integer>();
  private static final Random       random       = new Random();
  private static final Range        exclude      = EphemeralPorts.getRange();
  static {
    int candidatePortCount = 65536 - (exclude.getUpper() - exclude.getLower() + 1) - 1024;
    if (candidatePortCount < 1024) {
      LOGGER.warn("*********************************************************************************************" +
          "\nOnly {} ports available for " + PortChooser.class.getSimpleName() + " assignment" +
          "\n    Ephemeral range: {}, {}" +
          "\n*********************************************************************************************",
          candidatePortCount, exclude.getLower(), exclude.getUpper());
    }
  }

  private static final InetAddress LOCALHOST;
  static {
    InetAddress localHost = null;
    try {
      // Tests use "localhost" as the server host name which _does_ go through name resolution ...
      localHost = InetAddress.getByName("localhost");
    } catch (UnknownHostException e) {
      LOGGER.warn("Unable to obtain an InetAddress for localhost via InetAddress.getByName(\"localhost\")", e);
    }
    LOCALHOST = localHost;
  }

  public int chooseRandomPort() {
    synchronized (VM_WIDE_LOCK) {
      int portNum = choose();
      Assert.assertTrue(chosen.add(portNum));
      return portNum;
    }
  }

  public int chooseRandom2Port() {
    int port;
    synchronized (VM_WIDE_LOCK) {
      do {
        port = choose();
        if (port + 1 >= MAX) continue;
        if (!isPortUsed(port + 1)) {
          Assert.assertTrue(chosen.add(port));
          Assert.assertTrue(chosen.add(port + 1));
          break;
        }
      } while (true);
    }
    return port;
  }

  public int chooseRandomPorts(int numOfPorts) {
    Assert.assertTrue(numOfPorts > 0);
    int port = 0;
    synchronized (VM_WIDE_LOCK) {
      do {
        port = choose();
        if (port + numOfPorts > MAX) continue;
        boolean isChosen = true;
        for (int i = 1; i < numOfPorts; i++) {
          if (isPortUsed(port + i)) {
            isChosen = false;
            break;
          }
        }
        if (isChosen && (port + numOfPorts <= MAX)) {
          break;
        }
      } while (true);

      for (int i = 0; i < numOfPorts; i++) {
        Assert.assertTrue(chosen.add(port + i));
      }
    }
    return port;
  }

  public boolean isPortUsed(int portNum) {
    final Integer port = portNum;
    if (chosen.contains(port)) return true;
    // A port is free iff a server socket can bind to the port AND a client connection is rejected
    return !(canBind(portNum) && rejectsConnect(portNum));
  }

  // This method was added to address MNK-5621; it seems that some ports may appear free
  // (by canBind) on Windows and not really be free.  Some services on Windows, like Remote
  // Desktop (RDP) according to the issue, don't establish an open listener but do respond
  // to connection requests on their assigned port(s).
  // (See https://support.microsoft.com/en-us/help/832017/service-overview-and-network-port-requirements-for-windows.)
  // A "failure to connect" is necessary to determine if the port is actually available.
  // This check presumes firewall rules are not responsible for dropping the connection
  // request -- not much we can do about that.
  private boolean rejectsConnect(int portNumber) {
    Socket sock = null;
    boolean isFree = false;
    try {
      sock = new Socket();
      InetSocketAddress endpoint;
      if (LOCALHOST == null) {
        endpoint = new InetSocketAddress("localhost", portNumber);
      } else {
        endpoint = new InetSocketAddress(LOCALHOST, portNumber);
      }
      sock.connect(endpoint, 50);
      isFree = false;
    } catch (IOException e) {
      isFree = true;
    } finally {
      if (sock != null) {
        try {
          sock.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    return isFree;
  }

  private boolean canBind(int portNum) {
    if (exclude.isInRange(portNum)) { return false; }
    ServerSocket ss = null;
    boolean isFree = false;
    try {
      ss = new ServerSocket(portNum);
      isFree = true;
    } catch (BindException be) {
      isFree = false; // port in use,
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    return isFree;
  }

  private synchronized int choose() {
    while (true) {
      final int attempt = getNonEphemeralPort();
      if (chosen.contains(attempt)) {
        continue; // already picked at some point, try again
      }
      // A port is free iff a server socket can bind to the port AND a client connection is rejected
      if (canBind(attempt) && rejectsConnect(attempt)) return attempt;
    }
  }

  private static int getNonEphemeralPort() {
    while (true) {
      int p = random.nextInt(MAX - 1024) + 1024;
      if (!exclude.isInRange(p)) { return p; }
    }
  }

}
