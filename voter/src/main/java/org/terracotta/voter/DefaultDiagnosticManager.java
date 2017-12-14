package org.terracotta.voter;

import com.terracotta.connection.api.DiagnosticConnectionService;
import com.terracotta.diagnostic.Diagnostics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public class DefaultDiagnosticManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDiagnosticManager.class);

  private final DiagnosticConnectionService diagnosticConnectionService = new DiagnosticConnectionService();

  public ConnectionCloseableDiagnosticsEntity getEntity(String hostPort) throws ConnectionException, EntityNotProvidedException, EntityVersionMismatchException, EntityNotFoundException {
    LOGGER.debug("getEntity called with: {}", hostPort);
    URI uri = URI.create("diagnostic://" + hostPort);
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, System.getProperty("com.terracottatech.tools.clustertool.timeout", "30000"));

    Connection connection = diagnosticConnectionService.connect(uri, properties);
    EntityRef<Diagnostics, Object, Properties> ref = connection.getEntityRef(Diagnostics.class, 1L, "root");;
    Diagnostics diagnostics = ref.fetchEntity(null);
    return new ConnectionCloseableDiagnosticsEntity(diagnostics, connection);
  }

  public static final class ConnectionCloseableDiagnosticsEntity implements Closeable {
    private final Diagnostics diagnostics;
    private final Connection connection;

    public ConnectionCloseableDiagnosticsEntity(Diagnostics diagnostics, Connection connection) {
      this.diagnostics = diagnostics;
      this.connection = connection;
    }

    public Diagnostics getDiagnostics() {
      return diagnostics;
    }

    @Override
    public void close() throws IOException {
      LOGGER.debug("close called");
      connection.close();
    }
  }

  public static void main(String[] args) throws ConnectionException, EntityVersionMismatchException, EntityNotProvidedException, EntityNotFoundException {
    DefaultDiagnosticManager diagnosticManager = new DefaultDiagnosticManager();
    ConnectionCloseableDiagnosticsEntity entity = diagnosticManager.getEntity("localhost:9410");
    Diagnostics diagnostics = entity.getDiagnostics();
    System.out.println(diagnostics.invokeWithArg("VoterManager", "registerVoter", "foo"));
    System.out.println(diagnostics.invokeWithArg("VoterManager", "confirmVoter", "foo"));
    System.out.println(diagnostics.invokeWithArg("VoterManager", "deregisterVoter", "foo"));
  }
}
