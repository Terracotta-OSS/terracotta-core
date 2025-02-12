/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.terracotta.testing.support;

import org.terracotta.utilities.test.net.PortManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class PortTool {
  /**
   * Determine and reserve, if necessary, the designated debug ports.
   * <p>
   * Debug ports, if requested, are reserved from a specified base port.  It's possible that
   * the designated debug port, or one or more of the consecutive ports, is not allocable by
   * PortManager.  In this case, we "use" the port without reserving it.
   * @param portManager the non-{@code null}, {@code PortManager} instance to use
   * @param serverDebugStartPort the starting debug port number; must be {@code >= 0} and {@code <= 65535}
   * @param serverCount the number of servers for which debug ports are allocated/reserved; must be positive
   * @param debugPortRefs the list into which the {@code PortRef} instance for each allocated debug port is
   *                      added; {@code null} entries are added when a reservation is not made/necessary
   * @param serverDebugPorts the list into which the port debug port numbers are added
   * @throws IllegalArgumentException if {@code serverDebugPortStart} is out of range or {@code serverCount}
   *      is not positive
   * @throws IllegalStateException if a debug port is already reserved
   */
  public static void assignDebugPorts(PortManager portManager, int serverDebugStartPort, int serverCount,
                                      List<PortManager.PortRef> debugPortRefs, List<Integer> serverDebugPorts) {
    Objects.requireNonNull(portManager, "portManager");
    Objects.requireNonNull(debugPortRefs, "debugPortRefs");
    Objects.requireNonNull(serverDebugPorts, "serverDebugPorts");
    if (serverCount <= 0) {
      throw new IllegalArgumentException("serverCount must be greater than zero");
    }
    if (serverDebugStartPort == 0) {
      debugPortRefs.clear();
      IntStream.generate(() -> 0).limit(serverCount).boxed().forEach(serverDebugPorts::add);
    } else if (serverDebugStartPort < 0 || serverDebugStartPort > 65535) {
      throw new IllegalArgumentException("serverDebugStartPort " + serverDebugStartPort + " is out of range");
    } else {
      for (int i = 0; i < serverCount; i++, serverDebugStartPort++) {
        int candidatePort = serverDebugStartPort;
        try {
          debugPortRefs.add(portManager.reserve(candidatePort)
              .orElseThrow(() -> new IllegalStateException("Debug candidate port " + candidatePort + " cannot be reserved")));
        } catch (IllegalArgumentException e) {
          // Unable to reserve port; either bad port designation or not reservable -- "use" port without reserving
          debugPortRefs.add(null);
        }
        serverDebugPorts.add(candidatePort);
      }
    }
  }
}
