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

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TCVoterMainTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testOverrideVote() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    TCVoter voter = mock(TCVoter.class);
    voterMain.voter = voter;

    String overrideTarget = "foo:1234";
    String[] args = new String[] {"-o", overrideTarget};
    voterMain.processArgs(args);

    verify(voter).overrideVote(overrideTarget);
  }

  @Test
  public void testServerOpt() throws Exception {
    String hostPort = "foo:1234";
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected void startVoter(String... hostPorts) {
        assertThat(hostPorts, arrayContaining(hostPort));
      }
    };

    String[] args = new String[] {"-s", hostPort};
    voterMain.processArgs(args);
  }

  @Test
  public void testMultipleServerOptArgs() throws Exception {
    String hostPort1 = "foo:1234";
    String hostPort2 = "bar:2345";
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected void startVoter(String... hostPorts) {
        assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));
      }
    };

    String[] args = new String[] {"-s", hostPort1 + "," + hostPort2};
    voterMain.processArgs(args);
  }

  @Test
  public void testMultipleServerOpts() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();

    String[] args = new String[] {"-s", "foo:1234", "-s", "bar:2345"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Usage of multiple -s options not supported");
    voterMain.processArgs(args);
  }

  @Test
  public void testConfigFileOpt() throws Exception {
    String hostPort1 = "foo:1234";
    String hostPort2 = "bar:2345";
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected void startVoter(String... hostPorts) {
        assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));
      }
    };

    String[] args = new String[] {"-f", "src/test/resources/tc-config.xml"};
    voterMain.processArgs(args);
  }

  @Test
  public void testMultipleConfigFileOpts() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();

    String[] args = new String[] {"-f", "foo", "-f", "bar"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Usage of multiple -f options not supported");
    voterMain.processArgs(args);
  }

  @Test
  public void testZeroArguments() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[0];

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Neither the override option -o nor the regular options -s or -f provided");
    voterMain.processArgs(args);
  }

  @Test
  public void testTargetServerAndConfigFileOptions() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[] {"-s", "bar:1234", "-f", "baz"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Both -s and -f options provided. Use either one and not both together.");
    voterMain.main(args);
  }

  @Test
  public void testInvalidTargetHostPort() throws Exception {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[] {"-s", "bar:baz"};

    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("Invalid host:port combination provided");
    voterMain.processArgs(args);
  }

}