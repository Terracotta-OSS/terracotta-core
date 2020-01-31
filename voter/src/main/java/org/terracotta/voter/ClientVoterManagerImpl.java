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

import com.terracotta.diagnostic.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import static com.tc.voter.VoterManagerMBean.MBEAN_NAME;

public class ClientVoterManagerImpl implements ClientVoterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientVoterManagerImpl.class);

  public static final String REQUEST_TIMEOUT = "Request Timeout";

  private final String hostPort;
  private Connection connection;
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
    if (connection != null) {
      return;
    }
    URI uri = URI.create("diagnostic://" + hostPort);
    Properties properties = connectionProps.orElse(new Properties());
    try {
      Connection temp = ConnectionFactory.connect(uri, properties);
      synchronized (this) {
        if (connection != null) {
          connection.close();
        }
        connection = temp;
        EntityRef<Diagnostics, Object, Properties> ref = connection.getEntityRef(Diagnostics.class, 1L, "root");;
        this.diagnostics = ref.fetchEntity(properties);        
      }
      LOGGER.info("Connected to {}", hostPort);
    } catch (IOException | ConnectionException | EntityNotProvidedException | EntityNotFoundException | EntityVersionMismatchException e) {
      throw new RuntimeException("Unable to connect to " + hostPort, e);
    }
  }

  @Override
  public long registerVoter(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", id));
    long nr = Long.parseLong(result);
    return nr;
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
  public String getTopology() throws TimeoutException {
    return processInvocation(diagnostics.invoke("TopologyMBean", "getTopology"));
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
      if (this.connection != null) {
        this.connection.close();
        LOGGER.info("Connection closed to {}", hostPort);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to close the connection: {}", connection);
    } finally {
      this.connection = null;
    }
  }
  
  @Override
  public synchronized boolean isConnected() {
    return this.connection != null;
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
    return "ClientVoterManagerImpl{" + "hostPort=" + hostPort + ", connection=" + connection + '}';
  }
}
