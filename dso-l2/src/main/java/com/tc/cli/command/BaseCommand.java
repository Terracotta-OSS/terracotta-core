/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cli.command;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseCommand implements Command {
  
  protected Writer writer;
  
  public BaseCommand(Writer writer) {
    this.writer = writer;
  }
  
  protected void println(String message) {
    try {
      writer.write(message);
      writer.write("\n");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
