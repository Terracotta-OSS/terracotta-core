/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import java.util.LinkedHashSet;
import java.util.Set;

public class CallbackPortRange {

  public static final int SYSTEM_ASSIGNED = 0;

  public static Set<Integer> expandRange(String propValue) {
    propValue = propValue.trim().replaceAll("\\s", "");
    String[] parts = propValue.split(",");

    Set<Integer> range = new LinkedHashSet<Integer>();
    for (String part : parts) {
      if (part.length() > 0) {
        if (part.indexOf("-") > 0) {
          String[] lowHigh = part.split("-");
          if (lowHigh.length != 2) { throw new IllegalArgumentException("unparseable port range: " + part); }
          int low = Integer.parseInt(lowHigh[0]);
          int high = Integer.parseInt(lowHigh[1]);

          if (low > high) { throw new IllegalArgumentException("Invalid range: " + part); }

          for (int i = low; i <= high; i++) {
            range.add(Integer.valueOf(i));
          }
        } else {
          range.add(Integer.parseInt(part));
        }
      }
    }

    // verify range that has -1 or 0 only if size() == 1
    if (range.contains(TransportHandshakeMessage.NO_CALLBACK_PORT) || range.contains(SYSTEM_ASSIGNED)) {
      if (range.size() != 1) {
        //
        throw new IllegalArgumentException("port range containing special values must be size 1: " + propValue);
      }
    }

    return range;
  }

}

