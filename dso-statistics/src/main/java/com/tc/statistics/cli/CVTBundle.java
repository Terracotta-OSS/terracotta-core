/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

import java.util.ListResourceBundle;

public class CVTBundle  extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"error.argument.missing", "Missing argument ''{0}''.\n"},
    {"error.option.missing", "Missing option ''{0}''.\n"},
    {"option.help", "shows this text."},
    {"option.host", "host name or address of the gatherer (defaults to localhost)."},
    {"option.port", "JMX port of the gatherer (defaults to 9520)."},
    {"option.file", "file name of the script with commands to play."}
  };
}