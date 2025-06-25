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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.testing.config.DefaultLegacyConfigBuilder;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class ReflectorFunctionIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2)
          .inline(false)
          .startupBuilder(()->new DefaultStartupCommandBuilder() {
            @Override
            public String[] build() {
              List<String> def = new ArrayList<>();
              if (!getServerName().equals("testServer0")) {
                Iterator<String> args = Arrays.asList(super.build()).iterator();
                def.add(args.next());
                while (args.hasNext()) {
                  if (!args.hasNext()) {
                    def.add("-r");
                  } else {
                    def.add(args.next());
                  }
                }
              } else {
                def.addAll(Arrays.asList(super.build()));
              }
              return def.toArray(new String[0]);
            }
          })
          .withClientReconnectWindowTime(30).build();



  @Test
  public void testStart() throws Exception {
    Thread.sleep(30000);
  }
}
