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

import com.tc.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.toList;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  protected final String id = UUID.getUUID().toString();
  private final TCConfigParserUtil parser = new TCConfigParserUtil();
  private final Map<String, List<ActiveVoter>> registeredClusters = new ConcurrentHashMap<>();

  public TCVoterImpl() {
    LOGGER.info("Voter ID: {}", id);
  }

  @Override
  public boolean overrideVote(String hostPort) {
    ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
    voterManager.connect();
    boolean override;
    try {
      override = voterManager.overrideVote(id);
    } catch (TimeoutException e) {
      LOGGER.error("Override vote to {} timed-out", hostPort);
      return false;
    }

    if (override) {
      LOGGER.info("Successfully cast an override vote to {}", hostPort);
    } else {
      LOGGER.info("Override vote rejected by {}", hostPort);
    }
    return override;
  }

  @Override
  public Future<VoterStatus> register(String clusterName, String... hostPorts) {
    TreeSet<List<String>> cluster = extractStripesFromHostPorts(hostPorts); // Using a TreeSet here to establish a defined order in which the stripes are processed by different voters.
    validateStripesLimit(cluster.size(), hostPorts);
    List<ActiveVoter> voters = new ArrayList<>(cluster.size());
    List<CompletableFuture<VoterStatus>> voterStatuses = new ArrayList<>(cluster.size());
    for (List<String> stripe : cluster) {
      CompletableFuture<VoterStatus> stripeVoterStatus = new CompletableFuture<>();
      voterStatuses.add(stripeVoterStatus);
      ActiveVoter activeVoter = new ActiveVoter(id, stripeVoterStatus, stripe.toArray(new String[stripe.size()])).start();
      voters.add(activeVoter);
    }

    if (registeredClusters.putIfAbsent(clusterName, voters) != null) {
      throw new RuntimeException("Another cluster is already registered with the name: " + clusterName);
    }

    return new Future<VoterStatus>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return voterStatuses.stream().allMatch(CompletableFuture::isDone);
      }

      @Override
      public VoterStatus get() {
        CompletableFuture.allOf(voterStatuses.toArray(new CompletableFuture[voterStatuses.size()])).join();
        return new VoterStatus() {
          @Override
          public boolean isActive() {
            return voterStatuses.stream().allMatch(cf -> {
              try {
                return cf.get().isActive();
              } catch (InterruptedException | ExecutionException e) {
                return false;
              }
            });
          }
        };
      }

      @Override
      public VoterStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoterStatus> voterStatusCompletableFuture = CompletableFuture.supplyAsync(this::get);
        return voterStatusCompletableFuture.get(timeout, unit);
      }
    };
  }

  private TreeSet<List<String>> extractStripesFromHostPorts(String... hostPorts) {
    TreeSet<List<String>> cluster = new TreeSet<>(Comparator.comparing(List::toString));
    List<CompletableFuture<Void>> completableFutures = new ArrayList<>(hostPorts.length);
    for (String hostPort : hostPorts) {
      CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
        ClientVoterManagerImpl voterManager = new ClientVoterManagerImpl(hostPort);
        voterManager.connect();
        try {
          String serverConfig = voterManager.getServerConfig();
          try {
            String[] stripe = parser.parseHostPorts(new ByteArrayInputStream(serverConfig.getBytes("UTF-8")));
            cluster.add(Arrays.asList(stripe));
          } catch (SAXException | IOException e) {
            throw new AssertionError("Received invalid config from server: " + serverConfig);
          }
        } catch (TimeoutException e) {
          // Ignore
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
