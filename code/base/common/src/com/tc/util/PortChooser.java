/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.net.EphemeralPorts;
import com.tc.net.EphemeralPorts.Range;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class PortChooser {
  public static final int     MAX          = 65535;

  private static final Object VM_WIDE_LOCK = (PortChooser.class.getName() + "LOCK").intern();
  private static final Set    chosen       = new HashSet();
  private static final Random random       = new Random();
  private static final Range  exclude      = EphemeralPorts.getRange();

  public int chooseRandomPort() {
    synchronized (VM_WIDE_LOCK) {
      return choose();
    }
  }

  private int choose() {
    while (true) {
      final Integer attempt = new Integer(getNonEphemeralPort());
      boolean added = chosen.add(attempt);
      if (!added) {
        continue; // already picked at some point, try again
      }

      ServerSocket ss = null;
      try {
        int port = attempt.intValue();
        ss = new ServerSocket(port);
        return port;
      } catch (BindException be) {
        continue; // port in use, try another
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        if (ss != null) {
          while (!ss.isClosed()) {
            try {
              ss.close();
            } catch (IOException e) {
              // ignore
            }
          }
        }
      }
    }
  }

  private static int getNonEphemeralPort() {
    while (true) {
      int p = random.nextInt(MAX - 1024) + 1024;
      if (p < exclude.getLower() || p > exclude.getUpper()) { return p; }
    }
  }

}
