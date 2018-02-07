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
package org.terracotta.voter;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toList;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  protected final String id;
  private final Map<String, List<ActiveVoter>> registeredClusters = new ConcurrentHashMap<>();

  public TCVoterImpl() {
    this.id = UUID.getUUID().toString();
    LOGGER.info("Voter ID: {}", id);
  }

  @Override
  public boolean vetoVote(String hostPort) {
    ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
    voterManager.connect();
    String id = UUID.getUUID().toString();
    boolean veto;
    try {
      veto = voterManager.vetoVote(id);
    } catch (TimeoutException e) {
      LOGGER.error("Veto vote to {} timed-out", hostPort);
      return false;
    }

    if (veto) {
      LOGGER.info("Successfully cast a veto vote to {}", hostPort);
    } else {
      LOGGER.info("Veto vote rejected by {}", hostPort);
    }
    return veto;
  }

  @Override
  public void register(String clusterName, String... hostPorts) {
    TreeSet<List<String>> cluster = extractStripesFromHostPorts(hostPorts); // Using a TreeSet here to establish a defined order in which the stripes are processed by different voters.
    validateStripesLimit(cluster.size(), hostPorts);
    List<ActiveVoter> voters = new ArrayList<>(cluster.size());
    for (List<String> stripe : cluster) {
      ActiveVoter activeVoter = new ActiveVoter(id, stripe.toArray(new String[stripe.size()])).start();
      voters.add(activeVoter);
    }

    if (registeredClusters.putIfAbsent(clusterName, voters) != null) {
      throw new RuntimeException("Another cluster is already registered with the name: " + clusterName);
    }
  }

  private TreeSet<List<String>> extractStripesFromHostPorts(String... hostPorts) {
    TreeSet<List<String>> cluster = new TreeSet<>(Comparator.comparing(List::toString));
    List<CompletableFuture<Void>> completableFutures = new ArrayList<>(hostPorts.length);
    for (String hostPort : hostPorts) {
      CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
        ClientVoterManagerImpl voterManager = new ClientVoterManagerImpl(hostPort);
        voterManager.connect();
        String serverConfig;
        try {
          serverConfig = voterManager.getServerConfig();
          TcConfiguration config;

          try {
            config = TCConfigurationParser.parse(serverConfig);
          } catch (SAXException | IOException e) {
            throw new AssertionError("Received invalid config from server: " + serverConfig);
          }

          List<String> stripe = config.getPlatformConfiguration().getServers().getServer().stream()
              .map(s -> s.getHost() + ":" + s.getTsaPort().getValue())
              .sorted()
              .collect(toList());
          cluster.add(stripe);

        } catch (TimeoutException e) {

        }
      });
      completableFutures.add(completableFuture);
    }

    CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()])).join();
    return cluster;
  }

  protected void validateStripesLimit(int stripes, String... hostPorts) {
    if (stripes > 1) {
      throw new RuntimeException(Arrays.toString(hostPorts) + " do not belong to a single stripe and multiple stripes are not supported");
    }
  }

  @Override
  public void deregister(String clusterName) {
    List<ActiveVoter> voters = registeredClusters.remove(clusterName);
    if (voters != null) {
      for (AutoCloseable voter : voters) {
        try {
          voter.close();
        } catch (Exception exp) {
          throw new RuntimeException(exp);
        }
      }
    } else {
      throw new RuntimeException("A cluster with the given name: " + clusterName + " is not registered with this voter");
    }
  }

}
