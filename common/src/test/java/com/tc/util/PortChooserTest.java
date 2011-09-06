/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

public class PortChooserTest extends TCTestCase {

  private PortChooser portChooser;

  @Override
  protected void setUp() throws Exception {
    this.portChooser = new PortChooser();
  }

  public void testChooseRandomPorts() {
    int numOfPorts = 100;
    int portNum = this.portChooser.chooseRandomPorts(numOfPorts);

    for (int i = 0; i < numOfPorts; i++) {
      Assert.assertTrue(this.portChooser.isPortUsed(portNum + i));
    }
  }

  public void testAll() {
    int portNum1 = this.portChooser.chooseRandomPort();

    int portNum2 = this.portChooser.chooseRandom2Port();
    Assert.assertTrue(this.portChooser.isPortUsed(portNum2));
    Assert.assertTrue(this.portChooser.isPortUsed(portNum2 + 1));

    int numOfPorts = 1000;
    int portNum3 = this.portChooser.chooseRandomPorts(numOfPorts);
    for (int i = 0; i < numOfPorts; i++) {
      Assert.assertTrue(this.portChooser.isPortUsed(portNum3 + i));
    }

    Assert.assertTrue(portNum1 != portNum2);
    Assert.assertTrue(portNum2 != portNum3);
    Assert.assertTrue(portNum3 != portNum1);

    Assert.assertTrue(portNum1 != portNum2 + 1);
    
    for (int i = 0; i < numOfPorts; i++) {
      Assert.assertTrue(portNum1 != portNum3 + i);
      Assert.assertTrue(portNum2 != portNum3 + i);
      Assert.assertTrue(portNum2 + 1 != portNum3 + i);
    }
  }
}
