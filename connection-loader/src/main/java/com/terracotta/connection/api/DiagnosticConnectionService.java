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

package com.terracotta.connection.api;

import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.TerracottaInternalClientFactoryImpl;

/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class DiagnosticConnectionService extends AbstractConnectionService {
  private static final String SCHEME = "diagnostic";

  public DiagnosticConnectionService() {
    super(SCHEME);
  }

  public DiagnosticConnectionService(EndpointConnector endpointConnector) {
    super(SCHEME, endpointConnector, new TerracottaInternalClientFactoryImpl());
  }

  public DiagnosticConnectionService(EndpointConnector endpointConnector, TerracottaInternalClientFactory clientFactory) {
    super(SCHEME, endpointConnector, clientFactory);
  }
}
