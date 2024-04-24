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
package org.terracotta.functional;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
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
