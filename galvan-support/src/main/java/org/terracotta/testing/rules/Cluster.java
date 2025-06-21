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
package org.terracotta.testing.rules;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import org.junit.rules.ExternalResource;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.testing.config.ClusterInfo;

/**
 *
 * @author cdennis
 */
public abstract class Cluster extends ExternalResource {

  public abstract URI getConnectionURI();

  public abstract String[] getClusterHostPorts();

  public abstract Connection newConnection() throws ConnectionException;

  public abstract ClusterControl getClusterControl();

  public abstract TestManager getTestManager();

  public abstract void expectCrashes(boolean yes);

  public abstract CompletionStage<Void> manualStart(String display);
  
  public abstract ClusterInfo getClusterInfo();
}
