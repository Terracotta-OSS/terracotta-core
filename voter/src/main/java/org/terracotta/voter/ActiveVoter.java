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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static com.tc.voter.VoterManager.HEARTBEAT_RESPONSE;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

  private final static String ACTIVE_COORDINATOR = "ACTIVE-COORDINATOR";

  private static final long HEARTBEAT_INTERVAL = 1000L;
  private static final long REG_RETRY_INTERVAL = 5000L;

  private final Thread voter;
  private final String id;
  private final String[] servers;

  private volatile boolean active = false;

  public ActiveVoter(String id, CompletableFuture<VoterStatus> voterStatus, Optional<Properties> connectionProps, String... hostPorts) {
    this.voter = voterThread(voterStatus, connectionProps, hostPorts);
    this.id = id;
    this.servers = hostPorts;
  }
  
  public ActiveVoter start() {
    this.voter.start();
    return this;
  }

  private Thread voterThread(CompletableFuture<VoterStatus> voterStatus, Optional<Properties> connectionProps, String... hostPorts) {
    return new Thread(() -> {
      ScheduledExecutorService executorService = Executors.newScheduledThreadPool(hostPorts.length);
      Set<String> registrationLatch = new HashSet<>();
      registrationLatch.addAll(Arrays.asList(hostPorts));
      List<ClientVoterManager> voterManagers = Stream.of(hostPorts).map(ClientVoterManagerImpl::new).collect(toList());
      try {
        while (!Thread.currentThread().isInterrupted()) {
          ClientVoterManager currentActive = registerWithActive(id, executorService, voterManagers, connectionProps);

          active = true;
          LOGGER.info("{} registered with the active: {}", this, currentActive.getTargetHostPort());
          voterStatus.complete(new VoterStatus() {
            @Override
            public boolean isActive() {
              return active;
            }

            @Override
            public void awaitRegistrationWithAll() throws InterruptedException {
              synchronized(registrationLatch) {
                while (!registrationLatch.isEmpty()) {
                  registrationLatch.wait();
                }
              }
            }

            @Override
            public void awaitRegistrationWithAll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
              long current = System.currentTimeMillis();
              long finish = current + unit.toMillis(timeout);
              synchronized(registrationLatch) {
                while (!registrationLatch.isEmpty()) {
                  current = System.currentTimeMillis();
                  if (System.currentTimeMillis() >= finish) {
                    throw new TimeoutException("Registration timed out");
                  }
                  registrationLatch.wait(finish - current);
                }
              }
            }
          });

          registerAndHeartbeat(executorService, currentActive, voterManagers, connectionProps, registrationLatch);
          active = false;
        }
      } catch (InterruptedException e) {
        LOGGER.warn("{} interrupted", this);
      }
      active = false;
      executorService.shutdownNow();
      try {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException ie) {
        LOGGER.warn("{} interrupted", this);
      }
    });
  }

  ClientVoterManager registerWithActive(String id, ScheduledExecutorService executorService,
                                        List<ClientVoterManager> voterManagers, Optional<Properties> connectionProps) throws InterruptedException {
    CompletableFuture<ClientVoterManager> registrationLatch = new CompletableFuture<>();

    List<ScheduledFuture<?>> futures = voterManagers.stream().map(voterManager -> executorService.scheduleAtFixedRate(() -> {
      if (!voterManager.isConnected()) {
        voterManager.connect(connectionProps);
      }

      if (!registrationLatch.isDone()) {
        try {
          String serverState = voterManager.getServerState();
          if (serverState.equals(ACTIVE_COORDINATOR)) {
            long response = voterManager.registerVoter(id);
            if (response >= 0) {
              registrationLatch.complete(voterManager);
            } else {
              LOGGER.warn("Registration with {} in state {} failed. Retrying...", voterManager.getTargetHostPort(), voterManager.getServerState());
            }
          } else {
            LOGGER.info("State of {}: {}. Continuing the search for an active server.", voterManager.getTargetHostPort(), serverState);
          }
        } catch (TimeoutException e) {
          voterManager.close();
        }
      }
    }, 0, REG_RETRY_INTERVAL, TimeUnit.MILLISECONDS)).collect(Collectors.toList());

    LOGGER.info("{} waiting to get registered with the active", this);
    try {
      return registrationLatch.get();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      futures.forEach(f->f.cancel(true));
    }
  }

  public void registerAndHeartbeat(ExecutorService executorService, ClientVoterManager currentActive,
                                   List<ClientVoterManager> voterManagers, Optional<Properties> connectionProps,
                                   Set<String> registrationLatch) throws InterruptedException {
    AtomicReference<ClientVoterManager> voteOwner = new AtomicReference<>();
    //Try to connect and register with all the servers
    voteOwner.set(currentActive);
    List<Future<?>> futures = voterManagers.stream().map(voterManager -> executorService.submit(() -> {
      try {
        ClientVoterManager owner = voteOwner.get();
        while (owner != null && !executorService.isShutdown()) {          
          try {
            voterManager.connect(connectionProps);
            long lastVotedElection = 0;
            // if the current vote owner is the active, allowed to reconnect
            long registration = voterManager.registerVoter(id);
            LOGGER.debug("Registration {} for {}", registration, voterManager);
            if (registration > 0) {
              if (voterManager == owner) {
            // the vote was re-registered on the active, this is not allowed, voting cycle 
            // must start over
                if (voteOwner.compareAndSet(owner, null)) {
                  voterManager.deregisterVoter(id);
                }
                voterManager.close();
              } else {
                if (owner.isVoting()) {
                  voterManager.deregisterVoter(id);
                  voterManager.close();
                } else {
                  synchronized(registrationLatch) {
                    registrationLatch.remove(voterManager.getTargetHostPort());
                    if (registrationLatch.isEmpty()) {
                      registrationLatch.notifyAll();
                    }
                  }
                }
              }
            } else if (registration == 0) {
              synchronized(registrationLatch) {
                registrationLatch.remove(voterManager.getTargetHostPort());
                if (registrationLatch.isEmpty()) {
                  registrationLatch.notifyAll();
                }
              }
              //  everything is good, continue to operate  
            } else {
            //  unable to register
              if (voterManager == owner) {
            // the vote was re-registered on the active, this is not allowed, voting cycle 
            // must start over
                if (voteOwner.compareAndSet(owner, null)) {
                  voterManager.deregisterVoter(id);
                }
              }
              voterManager.close();
            }

            // heartbeat with the server until a vote is requested
            while (voterManager.isConnected()) {
              long election = heartbeat(voterManager);
              if (election < 0) {
                voterManager.close();
              } else if (owner == voterManager) {
                  // owner of the vote, go ahead and vote
                if (lastVotedElection > election) {
                  LOGGER.warn("{} term revoted last voted: {} current: {}", voterManager.getTargetHostPort(), lastVotedElection, election);
                }
                lastVotedElection = election;
                long result = voterManager.vote(id, election);
                LOGGER.info("Own the vote, voting for {} for term: {}, result: {}", voterManager.getTargetHostPort(), election, result);
              } else if (owner.isConnected()) {
                // ignore, back to heartbeating
                LOGGER.info("Not the vote owner and the owner is still connected, rejecting the vote request from {} for election term {}", voterManager.getTargetHostPort(), election);
                if (owner.isVoting()) {
                // if the owner is voting, this voter must zombie, cannot vote in this generation
                  voterManager.zombie();
                }
                // can never steal the vote back having voted in a previous vote tally
              } else if (lastVotedElection < election && voteOwner.compareAndSet(owner, voterManager)) {
                // stole ownership of the vote
                owner.zombie();
                long result = voterManager.vote(id, election);
                LOGGER.info("Stole the vote from {}, voting for {} for term: {}, result: {}", owner.getTargetHostPort(), voterManager.getTargetHostPort(), election, result);
                break;
              } else {
                LOGGER.info("Failed to steal the vote from {}, rejecting the vote request from {} for term {}, last voted election: {}", owner.getTargetHostPort(), voterManager.getTargetHostPort(), election, lastVotedElection);
                break;
                // failed to steal, back to heartbeating
              }
            }
          } catch (TimeoutException to) {
            LOGGER.warn("Heart-beating with {} timed-out", voterManager.getTargetHostPort());
            voterManager.close();
          } catch (InterruptedException e) {
            LOGGER.warn("Heart-beating with {} stopped", voterManager.getTargetHostPort());
            voterManager.close();            
          } catch (RuntimeException run) {
            LOGGER.warn("Heart-beating with {} not connected", voterManager.getTargetHostPort());
            voterManager.close(); 
          }
          owner = voteOwner.get();
          LOGGER.info("owner is " + owner);
        }
      } finally {
        voterManager.close();
      }
    })).collect(Collectors.toList());

    int tryCount = 0;
    String state;
    while (tryCount < 10) {
      ClientVoterManager vm = voteOwner.get();
      if (vm == null) {
        break;
      }
      try {
        state = vm.isConnected() ? voteOwner.get().getServerState() : "Not Connected";
      } catch (TimeoutException to) {
        state = "Timeout";
      }
      LOGGER.info("{} Vote owner state: {}, Try count: {}", this, state, tryCount);
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
        Optional<ClientVoterManager> om = voterManagers.stream().filter(ActiveVoter::isActive).findFirst();
        om.ifPresent(target->voteOwner.compareAndSet(vm, target));
        if (!om.isPresent()) {
          sleepFor10();
        }
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
  
  private static void sleepFor10() {
    try {
      TimeUnit.SECONDS.sleep(10);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
  
  private static boolean isActive(ClientVoterManager vm) {
    try {
      return vm.isConnected() && vm.getServerState().equals(ACTIVE_COORDINATOR);
    } catch (TimeoutException to) {
      return false;
    }
  }
  
  private long heartbeat(ClientVoterManager voter) throws TimeoutException, InterruptedException {
    long beatResponse = HEARTBEAT_RESPONSE;
    while (voter.isConnected() && beatResponse == HEARTBEAT_RESPONSE) {
      TimeUnit.MILLISECONDS.sleep(HEARTBEAT_INTERVAL);
      LOGGER.debug("Heartbeating with {}", voter.getTargetHostPort());
      beatResponse = voter.heartbeat(id);
    }
    LOGGER.info("Heartbeat broken. Response: {}", beatResponse);
    return beatResponse;
  }

  public void stop() {
    LOGGER.info("Stopping {}", this);
    this.voter.interrupt();
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public String toString() {
    return "ActiveVoter{" + Arrays.toString(servers) + "}";
  }

  public static void main(String[] args) {
    TCVoter voter = new TCVoterImpl();
    voter.register(UUID.getUUID().toString(), args);
  }
}
