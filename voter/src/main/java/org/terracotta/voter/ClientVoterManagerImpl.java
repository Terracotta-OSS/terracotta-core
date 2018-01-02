package org.terracotta.voter;

import com.terracotta.connection.api.DiagnosticConnectionService;
import com.terracotta.diagnostic.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public class ClientVoterManagerImpl implements ClientVoterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientVoterManagerImpl.class);

  private static final DiagnosticConnectionService CONNECTION_SERVICE = new DiagnosticConnectionService();

  private final Connection connection;
  private final Diagnostics diagnostics;

  public ClientVoterManagerImpl(String hostPort) {
    URI uri = URI.create("diagnostic://" + hostPort);
    Properties properties = new Properties();
    try {
      connection = CONNECTION_SERVICE.connect(uri, properties);
      EntityRef<Diagnostics, Object, Properties> ref = connection.getEntityRef(Diagnostics.class, 1L, "root");;
      this.diagnostics = ref.fetchEntity(properties);
    } catch (ConnectionException | EntityNotProvidedException | EntityNotFoundException | EntityVersionMismatchException e) {
      throw new RuntimeException("Unable to connect to server: " + hostPort, e);
    }
  }

  @Override
  public boolean registerVoter(String id) {
    String result = diagnostics.invokeWithArg("VoterManager", "registerVoter", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public boolean confirmVoter(String id) {
    String result = diagnostics.invokeWithArg("VoterManager", "confirmVoter", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public long heartbeat(String id) {
    return transformBeatResponse(diagnostics.invokeWithArg("VoterManager", "heartbeat", id));
  }

  @Override
  public long vote(String id, long term) {
    return transformBeatResponse(diagnostics.invokeWithArg("VoterManager", "vote", id));
  }

  @Override
  public long vote(String id) {
    return transformBeatResponse(diagnostics.invokeWithArg("VoterManager", "vote", id));
  }

  private long transformBeatResponse(String response) {
    try {
      return Long.parseLong(response);
    } catch (NumberFormatException e) {
      return Long.MIN_VALUE;
    } }

  @Override
  public boolean reconnect(String id) {
    String result = diagnostics.invokeWithArg("VoterManager", "reconnect", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public boolean deregisterVoter(String id) {
    String result = diagnostics.invokeWithArg("VoterManager", "deregisterVoter", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public void close() {
    try {
      this.connection.close();
    } catch (IOException e) {
      LOGGER.error("Failed to close the connection: {}", connection);
    }
  }
}
