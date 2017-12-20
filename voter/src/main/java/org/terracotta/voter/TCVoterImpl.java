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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.tc.voter.VoterManager.ERROR_RESPONSE;
import static com.tc.voter.VoterManager.HEARTBEAT_RESPONSE;
import static com.tc.voter.VoterManager.INVALID_VOTER_RESPONSE;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  private static final long CONNECTION_TIMEOUT = 5000L;
  private static final long HEARTBEAT_INTERVAL = 1000L;

  private volatile ExecutorService executorService;

  @Override
  public void register(String clusterName, String... hostPorts) {
    executorService = Executors.newFixedThreadPool(hostPorts.length);
    String id = UUID.getUUID().toString();
    LOGGER.info("Voter ID generated: {}", id);

    //TODO Rewrite the following code with CompletableFutures if possible

    while (true) {
      //Try to connect and register with all the servers
      Set<Future<ClientVoterManager>> regTasks = new HashSet<>();
      for (String hostPort: hostPorts) {
        regTasks.add(executorService.submit(() -> {
          LOGGER.info("Voter connecting to {}", hostPort);
          ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
          LOGGER.info("Voter connected to {}", id, hostPort);
          boolean registered = voterManager.registerVoter(id);
          while (!registered) {
            Thread.sleep(CONNECTION_TIMEOUT);
            LOGGER.info("Retrying voter registration");
            registered = voterManager.registerVoter(id);
          }
          LOGGER.info("Voter registered with {}", hostPort);
          return voterManager;
        }));
      }

      //Once the registration with all the servers complete, confirm the registration
      Set<Future<Void>> confTasks = new HashSet<>();
      for (Future<ClientVoterManager> task : regTasks) {
        confTasks.add(executorService.submit(() -> {
          ClientVoterManager voterManager = task.get();
          boolean confirmed = voterManager.confirmVoter(id);
          while (!confirmed) {
            Thread.sleep(CONNECTION_TIMEOUT);
            LOGGER.info("Retrying voter confirmation");
            confirmed = voterManager.confirmVoter(id);
          }
          voterManager.close();
          return null;
        }));
      }

      for (Future<Void> task : confTasks) {
        try {
          task.get();
        } catch (InterruptedException | ExecutionException e) {
          executorService.shutdownNow();
          throw new RuntimeException("Voter confirmation failed", e);
        }
      }
      LOGGER.info("Voter confirmed");

      //Once the voter registration is confirmed start heartbeating/votebeating
      Set<Future<Void>> heartBeatTasks = new HashSet<>();
      AtomicLong currentTerm = new AtomicLong(1);
      for (String hostPort : hostPorts) {
        heartBeatTasks.add(executorService.submit(() -> {
          ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
          long beatResponse = 0;
          while (true) {
            if (beatResponse == HEARTBEAT_RESPONSE) {
              beatResponse = voterManager.heartbeat(id);
              Thread.sleep(HEARTBEAT_INTERVAL);
            } else if (beatResponse > 0) {  //Election term number. This is the election request from the server
              long term = beatResponse;
              if (term > currentTerm.get()) {
                //Make sure that you vote only once for one term
                beatResponse = voterManager.vote(id, term);
                currentTerm.set(term);
              } else {
                beatResponse = voterManager.heartbeat(id);
              }
              Thread.sleep(HEARTBEAT_INTERVAL);
            } else if (beatResponse == INVALID_VOTER_RESPONSE) {
              LOGGER.info("Server rejected this voter as invalid. Attempting to re-register with the cluster.");
              return null;
            } else if (beatResponse == ERROR_RESPONSE) {
              LOGGER.info("Closing previous voter manager: {}", voterManager);
              voterManager.close();
              LOGGER.info("Creating new voter manager: {}", voterManager);
              voterManager = new ClientVoterManagerImpl(hostPort);
              if (voterManager.reconnect(id)) {
                LOGGER.info("Reconnected with {}", hostPort);
                beatResponse = HEARTBEAT_RESPONSE;
              } else {
                LOGGER.error("Failed to reconnect with {}. Attempting to re-register cluster.", hostPort);
                voterManager.close();
                return null;
              }
            } else {
              LOGGER.error("Expected a positive term number as the response. But received: {}", beatResponse);
              voterManager.close();
              return null;
            }
            LOGGER.info("Response {}", beatResponse);
          }
        }));
      }

      for (Future<Void> task : heartBeatTasks) {
        try {
          task.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException("Voter failed:", e);
        }
      }
      LOGGER.warn("Heartbeats to all servers stopped. Attempting to re-register");
    }
  }

  @Override
  public void deregister(String clusterName) {
    executorService.shutdownNow();
  }

  public static void main(String[] args) {
    TCVoter voter = new TCVoterImpl();
    voter.register("foo", "localhost:9410", "localhost:9510");
  }

}
