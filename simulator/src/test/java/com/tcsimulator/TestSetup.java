/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.simulator.distrunner.ArgException;
import com.tc.simulator.distrunner.ArgParser;
import com.tc.simulator.distrunner.SpecFactoryImpl;
import com.tcsimulator.distrunner.ServerSpecImpl;

public class TestSetup {

  private static TestSpec testSpec;

  public static void main(String[] args) throws ArgException {
    ArgParser parser = new ArgParser(args, new SpecFactoryImpl(), false, false);
    testSpec = new TestSpec(parser.getTestClassname(), parser.getIntensity(), parser.getClientSpecs(), parser
        .getServerSpecs());
    if (false) {
      // this is here to suppress warnings.
      testSpec.equals("foo");
    }
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig cfg) {
    cfg.addRoot("testSpec", TestSetup.class.getName() + ".testSpec");
    // XXX: sorry this breaks the factory encapsulation, oh well
    cfg.addIncludePattern(ServerSpecImpl.class.getName());
    cfg.addIncludePattern(ClientSpecImpl.class.getName());
  }

}
