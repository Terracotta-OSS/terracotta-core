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
package org.terracotta.functional;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class CustomLoggingConfigIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).withClientReconnectWindowTime(30)
          // activate custom logging with system property
          .withSystemProperty("CUSTOM_LOGGING", "true")
          .inline(false)
          .in(Paths.get(System.getProperty("galvan.dir")))
      .build();

  @Test
  public void testCustomLogging() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
  }

}
