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
package org.terracotta.voter.bin;

import com.tc.test.BaseScriptTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

/**
 * Tests for {@code start-tc-server} scripts.
 */
public class StartTcVoterScriptTest extends BaseScriptTest {

  private static final int PATH_NAME_SEGMENT_LENGTH = 14;

  public StartTcVoterScriptTest() {
    super(PATH_NAME_SEGMENT_LENGTH);
  }

  @Override
  protected void testScript(File installRoot, String stdOutContains, int exitCode) throws Exception {
    Duration timeout = Duration.ofSeconds(2L);

    Path basePath = Paths.get("voter");

    Path binDir = createInstallDir(installRoot, basePath.resolve("bin"));
    Path scriptPath = installScript(CURRENT_OPERATING_SYSTEM.appendScriptExtension("/voter/bin/start-tc-voter"), binDir);
    installScript(CURRENT_OPERATING_SYSTEM.appendScriptExtension("/setenv"), binDir);

    createJar("test.jar",
        createInstallDir(installRoot, basePath.resolve("lib")),
        true, "org.terracotta.voter.TCVoterMain");

    Map<String, String> environment = new LinkedHashMap<>();
    environment.put("JAVA_OPTS", "-Dfoo=bar");

    String[] arguments = { "arg1", "arg2" };

    ScriptResult scriptResult = null;
    try {
      scriptResult = execScript(installRoot, timeout, environment, CURRENT_OPERATING_SYSTEM.quoteCommand(scriptPath.toString()), arguments);

      // this is an odd Windows bug/issue where it can't find a valid path, e.g. C:\Users\jkok\AppData\Local\Temp\junit5343304611924326144\z;;.),{+!{$=$Q
      if (scriptResult.getStderrAsString().contains("The system cannot find the path specified.")) {
        return;
      }
      assertThat(scriptResult.getStderrAsString(), isEmptyString());
      assertThat(scriptResult.getStdoutAsString(), containsString(stdOutContains));
      assertThat(scriptResult.getCode(), is(exitCode));

      if (exitCode == 0) {
        assertThat(scriptResult.arguments(), is(arrayContaining(arguments)));

        Map<String, String> properties = scriptResult.properties();
        assertTrue(Files.isSameFile(Paths.get(properties.get("user.dir")), Paths.get(installRoot.toString())));
        assertThat(properties, hasEntry("foo", "bar"));
      }

    } catch (Throwable t) {
      showFailureOutput(System.err, t, scriptResult);
      throw t;
    }
  }
}
