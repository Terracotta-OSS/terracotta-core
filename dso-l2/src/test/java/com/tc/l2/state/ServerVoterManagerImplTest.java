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
package com.tc.l2.state;

import com.tc.services.TestTimeSource;
import org.junit.Test;

import static com.tc.voter.VoterManager.INVALID_VOTER_RESPONSE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ServerVoterManagerImplTest {

  TestTimeSource timeSource = new TestTimeSource(0);

  @Test
  public void testCanAcceptVoterWhenEmpty() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    assertThat(manager.canAcceptVoter(), is(true));
  }

  @Test
  public void testCanAcceptVoterWhenVotersFull() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    assertThat(manager.canAcceptVoter(), is(false));
  }

  @Test
  public void testCanAcceptVoterWhenVotersExpired() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    timeSource.passTime(1L + ServerVoterManagerImpl.VOTEBEAT_TIMEOUT);
    assertThat(manager.canAcceptVoter(), is(true));
  }

  @Test
  public void testRegisterVoterIdempotent() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    assertThat(manager.registerVoter("foo"), is(0L));
    assertThat(manager.registerVoter("foo"), is(0L));
  }

  @Test
  public void testRegisterVoterWhenVotersFull() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    assertThat(manager.registerVoter("foo"), is(0L));
    assertThat(manager.registerVoter("bar"), is(-1L));
  }

  @Test
  public void testRegisterVoterFailsWhenVotingInProgress() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.startVoting(5L);
    assertThat(manager.registerVoter("foo"), is(-1L));
    manager.stopVoting();
    assertThat(manager.registerVoter("foo"), is(5L));
  }

  @Test
  public void testDeregisterVoter() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    assertThat(manager.registerVoter("foo"), is(0L));
    assertThat(manager.deregisterVoter("foo"), is(true));
    assertThat(manager.registerVoter("bar"), is(0L));
  }

  @Test
  public void testHeartbeatFromInvalidVoter() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    assertThat(manager.heartbeat("bar"), is(-1L));
  }

  @Test
  public void testHeartbeatDuringElection() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    manager.startVoting(5L);
    assertThat(manager.heartbeat("foo"), is(5L));
  }

  @Test
  public void testVoting() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    manager.voters.put("bar", 1L);
    manager.voters.put("baz", 1L);
    manager.startVoting(5L);
    assertThat(manager.vote("foo", 5L), is(0L));
    assertThat(manager.vote("bar", 5L), is(0L));
    assertThat(manager.getVoteCount(), is(2));
  }

  @Test
  public void testVoteFromInvalidClientIgnored() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    manager.startVoting(5L);
    assertThat(manager.vote("bar", 5L), is(INVALID_VOTER_RESPONSE));
    assertThat(manager.getVoteCount(), is(0));
  }

  @Test
  public void testDuplicateVotesNotCounted() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    manager.startVoting(5L);
    assertThat(manager.vote("foo", 5L), is(0L));
    assertThat(manager.vote("foo", 5L), is(0L));  //duplicate
    assertThat(manager.getVoteCount(), is(1));
  }

  @Test
  public void testVotesNotCountedWhenNotInElection() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.voters.put("foo", 1L);
    assertThat(manager.vote("foo", 5L), is(0L));
    assertThat(manager.getVoteCount(), is(0));
  }

  @Test
  public void testOverrideVote() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    manager.startVoting(5L);
    assertThat(manager.overrideVote("foo"), is(true));
    assertThat(manager.overrideVoteReceived(), is(true));
  }

  @Test
  public void testOverrideVoteIgnoredWhenNotInElection() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource, false);
    assertThat(manager.overrideVote("foo"), is(false));
    assertThat(manager.overrideVoteReceived(), is(false));
  }
}