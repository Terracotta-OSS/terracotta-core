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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static com.tc.voter.VoterManager.HEARTBEAT_RESPONSE;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
/**
 * ActiveVoter votes only for the active it originally connects to.  If the active disconnects,
 * vote for the next server requesting vote.  If the server with the new vote becomes the new active, 
 * continue voting for that active.  If the server with the new vote does not become active 
 * in a reasonable amount of time, disconnect from all server and hunt for the new active until found.
 */
public class ActiveVoter implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveVoter.class);

  private final String ACTIVE_COORDINATOR   = "ACTIVE-COORDINATOR";

  private static final long HEARTBEAT_INTERVAL = 1000L;
  private static final long REG_RETRY_INTERVAL = 5000L;

  private final Thread voter;
  private final String id;
  
  public ActiveVoter(String id, String... hostPorts) {
    this.voter = voterThread(hostPorts);
    this.id = id;
  }
  
  public ActiveVoter start() {
    this.voter.start();
    return this;
  }

  private Thread voterThread(String... hostPorts) {
    return new Thread(() -> {
      ScheduledExecutorService executorService = Executors.newScheduledThreadPool(hostPorts.length);
      List<ClientVoterManager> voterManagers = Stream.of(hostPorts).map(ClientVoterManagerImpl::new).collect(toList());
      try {
        while (!Thread.currentThread().isInterrupted()) {
          // each generation needs a new registration ID so re-registers don't occur during an active vote
          //  find the active that this voter has successfully registered to
          ClientVoterManager currentActive = registerWithActive(id, executorService, voterManagers);
          LOGGER.info("Registered the cluster. Current active: {}", currentActive.getTargetHostPort());
          registerAndHeartbeat(executorService, currentActive, voterManagers);
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Voter daemon interrupted");
        executorService.shutdownNow();
      }
    });
  }

  ClientVoterManager registerWithActive(String id, ScheduledExecutorService executorService, List<ClientVoterManager> voterManagers) throws InterruptedException {
    CompletableFuture<ClientVoterManager> registrationLatch = new CompletableFuture<>();

    List<ScheduledFuture<?>> futures = voterManagers.stream().map(voterManager -> executorService.scheduleAtFixedRate(() -> {
      voterManager.connect();
      if (!registrationLatch.isDone()) {
        LOGGER.info("Trying to register with {}", voterManager.getTargetHostPort());
        try {
          String serverState = voterManager.getServerState();
          if (serverState.equals(ACTIVE_COORDINATOR)) {
            long response = voterManager.registerVoter(id);
            if (response >= 0) {
              registrationLatch.complete(voterManager);
            }
          }
        } catch (TimeoutException e) {
          voterManager.close();
          voterManager.connect();
        }
      }
    }, 0, REG_RETRY_INTERVAL, TimeUnit.MILLISECONDS)).collect(Collectors.toList());

    LOGGER.info("Waiting to get registered with the active");
    try {
      return registrationLatch.get();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      futures.forEach(f->f.cancel(true));
    }
  }

  public void registerAndHeartbeat(ExecutorService executorService, ClientVoterManager currentActive, List<ClientVoterManager> voterManagers) throws InterruptedException {    
    AtomicReference<ClientVoterManager> voteOwner = new AtomicReference<>();
    //Try to connect and register with all the servers
    voteOwner.set(currentActive);
    List<Future<?>> futures = voterManagers.stream().map(voterManager -> executorService.submit(() -> {
      try {
        ClientVoterManager owner = voteOwner.get();
        while (owner != null) {          
          try {
            if (owner.isConnected() && owner.getServerState().equals(ACTIVE_COORDINATOR)) {
              long lastVotedElection = 0;
              // if the current vote owner is the active, allowed to reconnect
              voterManager.connect();
              // register as a voter, this should not take too long since have already established as voter on the active
              registerAsVoter(voterManager);
              // heartbeat with the server until a vote is requested
              while (voterManager.isConnected()) {
                long election = heartbeat(voterManager);
                if (election < 0) {
                  voterManager.close();
                } else if (owner == voterManager) {
                    // owner of the vote, go ahead and vote
                  lastVotedElection = election;
                  long result = voterManager.vote(id, election);
                  LOGGER.info("own the vote, voting {} {} {}", voterManager.getTargetHostPort(), election, result);
                } else if (owner.isConnected()) {
                  // ignore, back to heartbeating
                  LOGGER.info("not the vote owner and the owner is still connected, rejecting the request for vote {} {}", voterManager.getTargetHostPort(), election);
                  // can never steal the vote back having voted in a previous vote tally
                } else if (lastVotedElection == 0 && voteOwner.compareAndSet(owner, voterManager)) {
                  // stole ownership of the vote
                  lastVotedElection = election;
                  long result = voterManager.vote(id, election);
                  LOGGER.info("stole the vote from {}, voting {} {} {}", owner.getTargetHostPort(), voterManager.getTargetHostPort(), election, result);
                  owner = voterManager;
                } else {
                  owner = voteOwner.get();
                  LOGGER.info("failed steal the vote from {}, rejecting the request for vote {} {}", owner.getTargetHostPort(), voterManager.getTargetHostPort(), election);
                  // failed to steal, back to heartbeating
                }
              }
            } else {
              TimeUnit.SECONDS.sleep(5);
            }
          } catch (TimeoutException to) {
            LOGGER.warn("Heart-beating with {} timedout", voterManager.getTargetHostPort());
            voterManager.close();
          } catch (InterruptedException e) {
            LOGGER.warn("Heart-beating with {} stopped", voterManager.getTargetHostPort());
            voterManager.close();            
          }
          owner = voteOwner.get();
        }
      } finally {
        voterManager.close();
      }
    })).collect(Collectors.toList());

    int tryCount = 0;
    String state;
    while (tryCount < 10) {
      try {
        ClientVoterManager voter = voteOwner.get();
        state = voter.isConnected() ? voteOwner.get().getServerState() : "Not Connected";
      } catch (TimeoutException to) {
        state = "Timeout";
      }
      LOGGER.info(state + " " + tryCount);
      if (state.equals(ACTIVE_COORDINATOR)) {
      // expected that the vote owner is active
        TimeUnit.SECONDS.sleep(5);
        tryCount = 0;
      } else if (state.equals("PASSIVE-STANDBY")) {
      // expected that the vote owner will be active soon
        TimeUnit.SECONDS.sleep(1);
      } else if (state.startsWith("PASSIVE-SYNCING")) {
      // expected that the vote owner will never be active because active is somewhere else
        break;
      } else {
      // unknown state, try again shortly, vote should be stolen soon
        TimeUnit.SECONDS.sleep(10);
        tryCount++;
      }
    }
//  by here, the server being voted for seems to not have won the election.  
//  reset everything and start over
    voteOwner.set(null);
    futures.forEach(f->f.cancel(true));
    voterManagers.stream().forEach(ClientVoterManager::close);
    LOGGER.warn("Rejected by all servers. Attempting to re-register");
  }
  
  private void registerAsVoter(ClientVoterManager voterManager) throws TimeoutException, InterruptedException {
    while (voterManager.registerVoter(id) < 0) {
      TimeUnit.MILLISECONDS.sleep(REG_RETRY_INTERVAL);
      LOGGER.info("Retrying voter registration {}", voterManager.getTargetHostPort());
    }
  }
  
  private long heartbeat(ClientVoterManager voter) throws TimeoutException, InterruptedException {
    long beatResponse = HEARTBEAT_RESPONSE;
    while (voter.isConnected() && beatResponse == HEARTBEAT_RESPONSE) {
      TimeUnit.MILLISECONDS.sleep(HEARTBEAT_INTERVAL);
      beatResponse = voter.heartbeat(id);
    }
    return beatResponse;
  }

  public void stop() {
    this.voter.interrupt();
  }

  @Override
  public void close() {
    stop();
  }

  public static void main(String[] args) {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("root");
    logger.setLevel(ch.qos.logback.classic.Level.INFO);
    TCVoter voter = new TCVoterImpl();
    voter.register(UUID.getUUID().toString(), args);
  }
}
