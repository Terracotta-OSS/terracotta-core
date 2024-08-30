/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.net.core.ProductID;
import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.TerracottaInternalClientFactoryImpl;
import java.util.Arrays;
import java.util.List;
import org.terracotta.entity.EndpointConnector;


/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class TerracottaConnectionService extends AbstractConnectionService {
  private static final List<String> SCHEMES = Arrays.asList("terracotta", ProductID.INFORMATIONAL.toString(), ProductID.SERVER.toString(), ProductID.STRIPE.toString());

  public TerracottaConnectionService() {
    super(SCHEMES);
  }

  public TerracottaConnectionService(EndpointConnector endpointConnector) {
    super(SCHEMES, endpointConnector, new TerracottaInternalClientFactoryImpl());
  }

  public TerracottaConnectionService(EndpointConnector endpointConnector, TerracottaInternalClientFactory clientFactory) {
    super(SCHEMES, endpointConnector, clientFactory);
  }
}
