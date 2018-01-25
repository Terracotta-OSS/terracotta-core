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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tc.config.schema.setup.ConfigurationSetupException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TCVoterMainTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testVetoVote() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    TCVoter voter = mock(TCVoter.class);
    voterMain.voter = voter;

    String vetoTarget = "foo:1234";
    String[] args = new String[] {"-v", vetoTarget};
    voterMain.main(args);

    verify(voter).vetoVote(vetoTarget);
  }

  @Test
  public void testVoter() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    TCVoter voter = mock(TCVoter.class);
    voterMain.voter = voter;

    String vetoTarget = "foo:1234";
    String clusterName = "bar";
    String[] args = new String[] {"-n", clusterName, "-s", vetoTarget};
    voterMain.main(args);

    verify(voter).register(clusterName, vetoTarget);
  }

  @Test
  public void testZeroArguments() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[0];

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Neither the veto option -v nor the regular options with -n and -s or -f provided");
    voterMain.main(args);
  }

  @Test
  public void testNoTargetServerWithClusterName() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[] {"-n", "foo"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Neither -s nor -f option provided with -n option");
    voterMain.main(args);
  }

  @Test
  public void testNoTargetServerAndConfigFileWithClusterName() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[] {"-n", "foo", "-s", "bar:1234", "-f", "baz"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Both -s and -f options provided. Use either one and not both together");
    voterMain.main(args);
  }

  @Test
  public void testInvalidTargetHostPort() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[] {"-n", "foo", "-s", "bar:baz"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Invalid host:port combination provided");
    voterMain.main(args);
  }

}