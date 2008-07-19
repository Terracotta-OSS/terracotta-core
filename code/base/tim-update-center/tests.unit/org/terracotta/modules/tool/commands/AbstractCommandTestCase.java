/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import com.tc.test.TCTestCase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for command test cases.
 */
public abstract class AbstractCommandTestCase extends TCTestCase {
  protected AbstractCommand command;
  protected StringWriter    commandOut;
  protected StringWriter    commandErr;

  protected void setUp() throws Exception {
    super.setUp();
    command = createCommand();
    commandOut = new StringWriter();
    commandErr = new StringWriter();
    command.out = new PrintWriter(commandOut);
    command.err = new PrintWriter(commandErr);
  }

  public void testHelpNotNull() {
    assertNotNull(command.help());
  }

  protected abstract AbstractCommand createCommand();

  protected void assertOutMatches(String regex) {
    assertMatches(commandOut, regex);
  }

  protected void assertErrMatches(String regex) {
    assertMatches(commandErr, regex);
  }

  private void assertMatches(StringWriter out, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(out.toString());
    assertTrue(matcher.find());
  }
}
