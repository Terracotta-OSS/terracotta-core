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
 */
package com.tc.util;

import com.tc.net.EphemeralPorts;
import com.tc.net.EphemeralPorts.Range;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class PortChooser {
  public static final int           MAX          = 65535;

  private static final Object       VM_WIDE_LOCK = (PortChooser.class.getName() + "LOCK").intern();
  private static final Set<Integer> chosen       = new HashSet<Integer>();
  private static final Random       random       = new Random();
  private static final Range        exclude      = EphemeralPorts.getRange();

  public int chooseRandomPort() {
    synchronized (VM_WIDE_LOCK) {
      int portNum = choose();
      Assert.assertTrue(chosen.add(Integer.valueOf(portNum)));
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
          Assert.assertTrue(chosen.add(Integer.valueOf(port)));
          Assert.assertTrue(chosen.add(Integer.valueOf(port + 1)));
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
        Assert.assertTrue(chosen.add(Integer.valueOf(port + i)));
      }
    }
    return port;
  }

  public boolean isPortUsed(int portNum) {
    final Integer port = Integer.valueOf(portNum);
    if (chosen.contains(port)) return true;
    return !canBind(portNum) && !canConnect(portNum);
  }

  private boolean canConnect(int portNumber) {
    Socket sock = null;
    boolean isFree = false;
    try {
      sock = new Socket("localhost", portNumber);
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
      if (chosen.contains(Integer.valueOf(attempt))) {
        continue; // already picked at some point, try again
      }
      if (canBind(attempt) && canConnect(attempt)) return attempt;
    }
  }

  private static int getNonEphemeralPort() {
    while (true) {
      int p = random.nextInt(MAX - 1024) + 1024;
      if (!exclude.isInRange(p)) { return p; }
    }
  }

}
