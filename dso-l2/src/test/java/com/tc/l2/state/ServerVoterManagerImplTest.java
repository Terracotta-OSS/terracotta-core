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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ServerVoterManagerImplTest {

  @Test
  public void testCanAcceptVoterWhenEmpty() throws Exception {
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1);
    assertThat(manager.canAcceptVoter(), is(true));
  }

  @Test
  public void testCanAcceptVoterWhenVotersFull() throws Exception {
    TestTimeSource timeSource = new TestTimeSource(0);
    ServerVoterManagerImpl manager = new ServerVoterManagerImpl(1, timeSource);
    manager.voters.put("foo", 1L);
    timeSource.passTime(1L + ServerVoterManagerImpl.VOTEBEAT_TIMEOUT);
    assertThat(manager.canAcceptVoter(), is(true));
  }
}