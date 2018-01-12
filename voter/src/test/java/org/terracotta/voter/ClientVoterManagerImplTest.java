package org.terracotta.voter;

import com.terracotta.diagnostic.Diagnostics;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

import static com.tc.voter.VoterManager.INVALID_VOTER_RESPONSE;
import static com.tc.voter.VoterManagerMBean.MBEAN_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.voter.ClientVoterManager.TIMEOUT_RESPONSE;
import static org.terracotta.voter.ClientVoterManagerImpl.REQUEST_TIMEOUT;

public class ClientVoterManagerImplTest {

  private ClientVoterManagerImpl manager = new ClientVoterManagerImpl("foo:6543");
  private Diagnostics diagnostics = mock(Diagnostics.class);

  @Before
  public void setUp() throws Exception {
    manager.diagnostics = diagnostics;
  }

  @Test
  public void testRegister() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn("123");
    assertThat(manager.registerVoter("foo"), is(123L));
  }

  @Test
  public void testRegisterFailure() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn("-1");
    assertThat(manager.registerVoter("foo"), is(INVALID_VOTER_RESPONSE));
  }

  @Test
  public void testRegisterTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.registerVoter("foo"), is(TIMEOUT_RESPONSE));
  }

  @Test
  public void testHeartbeat() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn("123");
    assertThat(manager.heartbeat("foo"), is(123L));
  }

  @Test
  public void testHeartbeatInvalidVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn("-1");
    assertThat(manager.heartbeat("foo"), is(INVALID_VOTER_RESPONSE));
  }

  @Test
  public void testHeartbeatTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.heartbeat("foo"), is(TIMEOUT_RESPONSE));
  }

  @Test
  public void testVote() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn("123");
    assertThat(manager.vote("foo", 123L), is(123L));
  }

  @Test
  public void testVoteInvalidVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn("-1");
    assertThat(manager.vote("foo", 123L), is(INVALID_VOTER_RESPONSE));
  }

  @Test
  public void testVoteTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.vote("foo", 123L), is(TIMEOUT_RESPONSE));
  }

  @Test
  public void testVetoVote() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vetoVote", "foo")).thenReturn("true");
    assertThat(manager.vetoVote("foo"), is(true));
  }

  @Test
  public void testVetoVoteFailure() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vetoVote", "foo")).thenReturn("false");
    assertThat(manager.vetoVote("foo"), is(false));
  }

  @Test
  public void testVetoVoteTimeout() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vetoVote", "foo")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.vetoVote("foo"), is(false));
  }

  @Test
  public void testDeregisterVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", "foo")).thenReturn("true");
    assertThat(manager.deregisterVoter("foo"), is(true));
  }

  @Test
  public void testDeregisterVoterTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", "foo")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.deregisterVoter("foo"), is(false));
  }

}