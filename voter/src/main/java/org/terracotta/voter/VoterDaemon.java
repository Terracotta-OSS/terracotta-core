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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.tc.voter.VoterManager.HEARTBEAT_RESPONSE;
import static com.tc.voter.VoterManager.INVALID_VOTER_RESPONSE;
import static java.util.stream.Collectors.toList;

public class VoterDaemon implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(VoterDaemon.class);

  private final String ACTIVE_COORDINATOR   = "ACTIVE-COORDINATOR";

  private static final long HEARTBEAT_INTERVAL = 1000L;
  private static final long REG_RETRY_INTERVAL = 5000L;

  private final String id;
  private final Thread voter;

  public VoterDaemon(String id, String... hostPorts) {
    this.id = id;
    this.voter = voterThread(hostPorts);
    this.voter.start();
  }

  private Thread voterThread(String... hostPorts) {
    return new Thread(() -> {
      ExecutorService executorService = Executors.newFixedThreadPool(hostPorts.length);
      List<ClientVoterManager> voterManagers = Stream.of(hostPorts).map(ClientVoterManagerImpl::new).collect(toList());
      try {
        while (!Thread.currentThread().isInterrupted()) {
          AtomicLong currentTerm = registerWithActive(executorService, voterManagers);
          LOGGER.info("Registered the cluster. Current term: {}", currentTerm.get());
          registerAndHeartbeat(executorService, currentTerm, voterManagers);
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Voter daemon interrupted");
        executorService.shutdownNow();
      }
    });
  }

  AtomicLong registerWithActive(ExecutorService executorService, List<ClientVoterManager> voterManagers) throws InterruptedException {
    AtomicLong currentTerm = new AtomicLong(-1);
    CountDownLatch registrationLatch = new CountDownLatch(1);

    voterManagers.forEach(voterManager -> executorService.submit(() -> {
      voterManager.connect();
      try {
        while (currentTerm.get() < 0) {
          try {
            String serverState = voterManager.getServerState();
            if (serverState.equals(ACTIVE_COORDINATOR)) {
              long response = voterManager.registerVoter(id);
              if (response >= 0) {
                currentTerm.set(response);
                registrationLatch.countDown();
                break;
              }
            }
          } catch (TimeoutException e) {
            voterManager.close();
            voterManager.connect();
          }

          Thread.sleep(REG_RETRY_INTERVAL);
          LOGGER.info("Retrying to register with {}", voterManager.getTargetHostPort());
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Registration with {} interrupted", voterManager.getTargetHostPort());
      } finally {
        voterManager.close();
      }
    }));

    LOGGER.info("Waiting to get registered with the active");
    registrationLatch.await();

    return currentTerm;
  }

  public void registerAndHeartbeat(ExecutorService executorService, AtomicLong currentTerm, List<ClientVoterManager> voterManagers) throws InterruptedException {
    CountDownLatch rejectionLatch = new CountDownLatch(voterManagers.size());
    //Try to connect and register with all the servers
    voterManagers.forEach(voterManager -> executorService.submit(() -> {
      try {
        voterManager.connect();
        long beatResponse = HEARTBEAT_RESPONSE;

        try {
          while (voterManager.registerVoter(id) < 0) {
            Thread.sleep(REG_RETRY_INTERVAL);
            LOGGER.info("Retrying voter registration");
          }
        } catch (TimeoutException e) {
          voterManager.close();
          voterManager.connect();
        }

        while (true) {
          try {
            if (beatResponse == HEARTBEAT_RESPONSE) {
              beatResponse = voterManager.heartbeat(id);
              Thread.sleep(HEARTBEAT_INTERVAL);
            } else if (beatResponse > 0) {  //Election term number. This is the election request from the server
              long term = beatResponse;
              LOGGER.info("Vote request received for term: {}. Current term: {}", term, currentTerm);
              long current = currentTerm.get();
              if (term > current && currentTerm.compareAndSet(current, term)) {
                //Make sure that you vote only once for one term
                LOGGER.info("Voting for term {}", term);
                beatResponse = voterManager.vote(id, term);
              } else {
                beatResponse = voterManager.heartbeat(id);
                LOGGER.info("Heartbeating instead of voting: {}", beatResponse);
              }
              Thread.sleep(HEARTBEAT_INTERVAL);
            } else if (beatResponse == INVALID_VOTER_RESPONSE) {
              LOGGER.info("Server rejected this voter as invalid. Attempting to re-register with the cluster.");
              rejectionLatch.countDown();
              break;
            } else {
              LOGGER.error("Unexpected response received: {}", beatResponse);
              break;
            }
          } catch (TimeoutException e) {
            LOGGER.info("Request timed-out. Attempting to reconnect to: {}", voterManager.getTargetHostPort());
            voterManager.close();
            voterManager.connect();
            LOGGER.info("Reconnected to: {}", voterManager.getTargetHostPort());
          }
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Heart-beating with {} interrupted", voterManager.getTargetHostPort());
      } finally {
        voterManager.close();
      }
    }));

    rejectionLatch.await();
    LOGGER.warn("Rejected by all servers. Attempting to re-register");
  }

  public void stop() {
    this.voter.interrupt();
  }

  @Override
  public void close() {
    stop();
  }
}
