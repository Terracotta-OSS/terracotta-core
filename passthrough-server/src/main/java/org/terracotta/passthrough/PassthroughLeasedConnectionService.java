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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import com.tc.classloader.OverrideService;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.lease.connection.LeasedConnection;
import org.terracotta.lease.connection.LeasedConnectionService;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

@OverrideService("org.terracotta.lease.connection.LeasedConnectionServiceImpl")
public class PassthroughLeasedConnectionService implements LeasedConnectionService {
  @Override
  public boolean handlesURI(URI uri) {
    return uri.getScheme().equals("passthrough") ||
            uri.getScheme().equals("terracotta") ||
            uri.getScheme().equals("mock"); // for the tests which are using mock connection service
  }

  @Override
  public LeasedConnection connect(URI uri, Properties properties) throws ConnectionException {
    return new PassthroughLeasedConnection(ConnectionFactory.connect(uri, properties));
  }

  private static class PassthroughLeasedConnection implements LeasedConnection {

    private final Connection connection;

    PassthroughLeasedConnection(Connection connection) {
      this.connection = connection;
    }

    @Override
    public <T extends Entity, C, U> EntityRef<T, C, U> getEntityRef(Class<T> cls, long version, String name) throws EntityNotProvidedException {
      return connection.getEntityRef(cls, version, name);
    }

    @Override
    public void close() throws IOException {
      connection.close();
    }
  }
}
