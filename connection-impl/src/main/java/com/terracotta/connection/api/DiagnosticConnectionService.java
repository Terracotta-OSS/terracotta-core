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
package com.terracotta.connection.api;

import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.TerracottaInternalClientFactoryImpl;
import java.util.Collections;
import org.terracotta.entity.EndpointConnector;

/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class DiagnosticConnectionService extends AbstractConnectionService {
  private static final String SCHEME = "diagnostic";

  public DiagnosticConnectionService() {
    super(Collections.singletonList(SCHEME));
  }

  public DiagnosticConnectionService(EndpointConnector endpointConnector) {
    super(Collections.singletonList(SCHEME), endpointConnector, new TerracottaInternalClientFactoryImpl());
  }

  public DiagnosticConnectionService(EndpointConnector endpointConnector, TerracottaInternalClientFactory clientFactory) {
    super(Collections.singletonList(SCHEME), endpointConnector, clientFactory);
  }
}
