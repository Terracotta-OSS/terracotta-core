/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.cli;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import com.terracottatech.config.BindPort;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.ws.rs.client.WebTarget;

import static org.hamcrest.MatcherAssert.assertThat;

public class ManagementToolUtilTest {
  @Test
  public void testDefaultTarget() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(cliBuilder(new String[0]));
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItem(hostAndPort("localhost", 9540)));
  }

  @Test
  public void testAllServersInConfig() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(
        cliBuilder(new String[] { "-f", createTempTcConfig(2, 2).getAbsolutePath()}), true);
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItems(
        hostAndPort("boundHostName", 1), hostAndPort("boundHostName", 2), hostAndPort("boundHostName", 3),
        hostAndPort("boundHostName", 4)));
  }

  @Test
  public void testSingleServerWithName() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(
        cliBuilder(new String[] { "-f", createTempTcConfig(1, 2).getAbsolutePath(), "-n", "server1"}));
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItem(hostAndPort("boundHostName", 1)));
  }

  @Test
  public void testSingleServerConfig() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(
        cliBuilder(new String[] { "-f", createTempTcConfig(1, 1).getAbsolutePath()}));
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItem(hostAndPort("boundHostName", 1)));
  }

  @Test
  public void testNonOptionList() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(
        cliBuilder(new String[] { "foo,bar:111", "baz:99"}));
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItems(
        hostAndPort("foo", 9540), hostAndPort("bar", 111), hostAndPort("baz", 99)));
  }

  @Test
  public void testServerList() throws Exception {
    Collection<WebTarget> targets = ManagementToolUtil.getTargets(
        cliBuilder(new String[] { "-servers", "foo:321,bar:1234"}));
    assertThat(targets, IsCollectionContaining.<WebTarget>hasItems(
        hostAndPort("foo", 321), hostAndPort("bar", 1234)));
  }

  private CommandLineBuilder cliBuilder(String[] args) {
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder("mine", args);
    ManagementToolUtil.addConnectionOptionsTo(commandLineBuilder);
    commandLineBuilder.parse();
    return commandLineBuilder;
  }

  private Matcher<WebTarget> hostAndPort(final String host, final int port) {
    return new TypeSafeMatcher<WebTarget>() {
      @Override
      protected boolean matchesSafely(final WebTarget item) {
        return item.getUri().getHost().equals(host) && item.getUri().getPort() == port;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText(host + ":" + port);
      }
    };
  }

  private TcConfigDocument createConfig(int groups, int serversPerGroup) {
    TcConfigDocument document = TcConfigDocument.Factory.newInstance();
    Servers servers = document.addNewTcConfig().addNewServers();
    int serverNumber = 1;
    for (int i = 0; i < groups; i++) {
      MirrorGroup mirrorGroup = servers.addNewMirrorGroup();
      for (int j = 0; j < serversPerGroup; j++) {
        Server server = mirrorGroup.addNewServer();
        server.setName("server" + serverNumber);
        BindPort managementPort = server.addNewManagementPort();
        managementPort.setBind("boundHostName");
        managementPort.setIntValue(serverNumber);
        server.addNewTsaPort().setIntValue(9999);
        serverNumber++;
      }
    }
    return document;
  }

  private File createTempTcConfig(int groups, int serversPerGroup) throws IOException {
    TcConfigDocument tcConfigDocument = createConfig(groups, serversPerGroup);
    File file = File.createTempFile("tc-config", ".xml");
    file.deleteOnExit();
    FileUtils.writeStringToFile(file, tcConfigDocument.toString());
    return file;
  }
}