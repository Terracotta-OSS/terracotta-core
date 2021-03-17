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
package org.terracotta.voter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static com.tc.voter.VoterManagerMBean.MBEAN_NAME;
import java.net.InetSocketAddress;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;

public class ClientVoterManagerImpl implements ClientVoterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientVoterManagerImpl.class);

  public static final String REQUEST_TIMEOUT = "Request Timeout";

  private final String hostPort;
  Diagnostics diagnostics;
  
  private volatile boolean voting = false;
  private volatile long generation = 0;

  public ClientVoterManagerImpl(String hostPort) {
    this.hostPort = hostPort;
  }

  @Override
  public String getTargetHostPort() {
    return this.hostPort;
  }

  @Override
  public void connect(Optional<Properties> connectionProps) {
    String[] split = this.hostPort.split(":");
    InetSocketAddress addr = InetSocketAddress.createUnresolved(split[0], Integer.parseInt(split[1]));
    Properties properties = connectionProps.orElse(new Properties());
    try {
      Diagnostics temp = DiagnosticsFactory.connect(addr, properties);
      synchronized (this) {
        if (diagnostics != null) {
          diagnostics.close();
        }
        diagnostics = temp;
      }
      LOGGER.info("Connected to {}", hostPort);
    } catch (ConnectionException e) {
      throw new RuntimeException("Unable to connect to " + hostPort, e);
    }
  }

  @Override
  public long registerVoter(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", id));
    try {
      return Long.parseLong(result);
    } catch (NumberFormatException ne) {
      LOGGER.info("unexpected value returned for register voter", result);
      throw new RuntimeException("register voter error");
    }
  }

  @Override
  public long heartbeat(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", id));
    long nr = Long.parseLong(result);
    if (nr <= 0) {
      voting = false;
      generation = 0;
    } else {
      if (!voting && generation == nr) {
        //  already zombied for this generation, cannot vote, just heartbeat
        return 0;
      } else {
        voting = true;
        generation = nr;
      }
    }
    return nr;
  }

  @Override
  public long vote(String id, long term) throws TimeoutException {
    if (!voting) {
      return -1;
    }
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "vote", id + ":" + term));
    return Long.parseLong(result);
  }

  @Override
  public boolean overrideVote(String id) {
    String result = diagnostics.invokeWithArg(MBEAN_NAME, "overrideVote", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public boolean deregisterVoter(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", id));
    return Boolean.parseBoolean(result);
  }

  @Override
  public String getServerState() throws TimeoutException {
    return processInvocation(diagnostics.getState());
  }

  @Override
  public String getServerConfig() throws TimeoutException {
    return processInvocation(diagnostics.getConfig());
  }

  @Override
  public Set<String> getTopology() throws TimeoutException {
    Set<String> resServers = new HashSet<>();
    String res = processInvocation(diagnostics.invoke("TopologyMBean", "getTopology"));
    String[] resHostPorts = res.split(",");
    for (int i = 0; i < resHostPorts.length; ++i) {
      resServers.add(resHostPorts[i]);
    }
    return resServers;
  }

  String processInvocation(String invocation) throws TimeoutException {
    if (invocation == null) {
      return "UNKNOWN";
    }
    if (invocation.equals(REQUEST_TIMEOUT)) {
      throw new TimeoutException("Request timed out");
    }
    return invocation;
  }

  @Override
  public synchronized void close() {
    try {
      if (this.diagnostics != null) {
        this.diagnostics.close();
        LOGGER.info("Connection closed to {}", hostPort);
      }
    } catch (Throwable t) {
        LOGGER.info("Connection trouble closing", t);
    } finally {
      this.diagnostics = null;
    }
  }
  
  @Override
  public synchronized boolean isConnected() {
    return this.diagnostics != null;
  }
  
  @Override
  public boolean isVoting() {
    return voting;
  }
  
  @Override
  public void zombie() {
    LOGGER.debug("Zombied {} for generation {}", getTargetHostPort(), generation);
    voting = false;
  }  

  @Override
  public String toString() {
    return "ClientVoterManagerImpl{" + "hostPort=" + hostPort + ", connection=" + diagnostics + '}';
  }
}
