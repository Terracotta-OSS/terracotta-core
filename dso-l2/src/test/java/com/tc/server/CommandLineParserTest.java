/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Configuration.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 *
 */
package com.tc.server;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;

import org.terracotta.config.ConfigurationProvider;

import static com.tc.server.CommandLineParser.Opt.CONSISTENT_STARTUP;
import static com.tc.server.CommandLineParser.Opt.SERVER_NAME;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandLineParserTest {

  private static final String TEST_SERVER = "test-server";

  @Rule
  public ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

  @Rule
  public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testParserWithShortNames() {
    String[] args = {
        "provider-option-1",
        SERVER_NAME.getShortOption(), TEST_SERVER,
        "provider-option-2",
        CONSISTENT_STARTUP.getShortOption()
    };

    ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);

    CommandLineParser parser = new CommandLineParser(args, configurationProvider);

    assertThat(parser.getServerName(), is(TEST_SERVER));
    assertThat(parser.consistentStartup(), is(true));
    assertThat(parser.getProviderArgs(), containsInAnyOrder("provider-option-1", "provider-option-2"));
  }

  @Test
  public void testParserWithLongNames() {
    String[] args = {
        "provider-option-1",
        SERVER_NAME.getLongOption(), TEST_SERVER,
        "provider-option-2",
        CONSISTENT_STARTUP.getLongOption()
    };

    CommandLineParser parser = new CommandLineParser(args, mock(ConfigurationProvider.class));

    assertThat(parser.getServerName(), is(TEST_SERVER));
    assertThat(parser.consistentStartup(), is(true));
    assertThat(parser.getProviderArgs(), containsInAnyOrder("provider-option-1", "provider-option-2"));
  }

  @Test
  public void testParserWithEmptyArgs() {
    CommandLineParser parser = new CommandLineParser(new String[0], mock(ConfigurationProvider.class));

    assertThat(parser.getServerName(), nullValue());
    assertThat(parser.consistentStartup(), is(false));
    assertThat(parser.getProviderArgs(), empty());
  }

  @Test
  public void testParserWithOnlyServerNameOption() {
    String[] args = {SERVER_NAME.getShortOption(), TEST_SERVER };

    CommandLineParser parser = new CommandLineParser(args, mock(ConfigurationProvider.class));

    assertThat(parser.getServerName(), is(TEST_SERVER));
    assertThat(parser.consistentStartup(), is(false));
    assertThat(parser.getProviderArgs(), empty());
  }

  @Test
  public void testParserWithOnlyConsistencyOption() {
    String[] args = {CONSISTENT_STARTUP.getShortOption()};

    CommandLineParser parser = new CommandLineParser(args, mock(ConfigurationProvider.class));

    assertThat(parser.getServerName(), nullValue());
    assertThat(parser.consistentStartup(), is(true));
    assertThat(parser.getProviderArgs(), empty());
  }

  @Test
  public void testParserWithOnlyProviderArgs() {
    String[] args = {"provider-option-1", "provider-option-2"};

    CommandLineParser parser = new CommandLineParser(args, mock(ConfigurationProvider.class));

    assertThat(parser.getServerName(), nullValue());
    assertThat(parser.consistentStartup(), is(false));
    assertThat(parser.getProviderArgs(), containsInAnyOrder("provider-option-1", "provider-option-2"));
  }

  @Test
  public void testParserWithNoServerNameOptions() {
    String[] args = {SERVER_NAME.getShortOption()};

    expectedException.expectMessage("Unable to parse command-line arguments");
    new CommandLineParser(args, mock(ConfigurationProvider.class));
  }

  @Test
  public void testParserWithHelpOption() {
    String[] args = { CommandLineParser.Opt.HELP.getShortOption() };
    ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
    when(configurationProvider.getConfigurationParamsDescription()).thenReturn("test help");

    systemOutRule.clearLog();
    expectedSystemExit.expectSystemExitWithStatus(0);

    new CommandLineParser(args, configurationProvider);

    assertThat(systemOutRule.getLog(), containsString("test help"));
  }

  @Test
  public void testParserWithHelpLongOption() {
    String[] args = { CommandLineParser.Opt.HELP.getLongOption() };
    ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
    when(configurationProvider.getConfigurationParamsDescription()).thenReturn("test help");

    systemOutRule.clearLog();
    expectedSystemExit.expectSystemExitWithStatus(0);

    new CommandLineParser(args, configurationProvider);

    assertThat(systemOutRule.getLog(), containsString("test help"));
  }

  @Test
  public void testParserWithHelpWithOtherOptions() {
    String[] args = {
        SERVER_NAME.getShortOption(),
        TEST_SERVER,
        CommandLineParser.Opt.HELP.getLongOption()
    };
    ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
    when(configurationProvider.getConfigurationParamsDescription()).thenReturn("test help");

    systemOutRule.clearLog();
    expectedSystemExit.expectSystemExitWithStatus(0);

    new CommandLineParser(args, configurationProvider);

    assertThat(systemOutRule.getLog(), containsString("test help"));
  }
}