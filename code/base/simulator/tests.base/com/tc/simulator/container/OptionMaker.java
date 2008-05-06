/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;


class OptionMaker {

  public OptionMaker withLongOpt(String opt) {
    OptionBuilder.withLongOpt(opt);
    return this;
  }

  public OptionMaker withValueSeparator() {
    OptionBuilder.withValueSeparator();
    return this;
  }

  public OptionMaker isRequired() {
    OptionBuilder.isRequired();
    return this;
  }

  public OptionMaker withArgName(String name) {
    OptionBuilder.withArgName(name);
    return this;
  }

  public OptionMaker withDescription(String desc) {
    OptionBuilder.withDescription(desc);
    return this;
  }

  public OptionMaker withValueSeparator(char sep) {
    OptionBuilder.withValueSeparator(sep);
    return this;
  }

  public OptionMaker hasArg() {
    OptionBuilder.hasArg();
    return this;
  }

  public Option create() {
    return OptionBuilder.create();
  }

  public Option create(char c) {
    return OptionBuilder.create(c);
  }
}