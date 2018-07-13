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
package com.tc.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.tc.test.TCExtension;

@ExtendWith(TCExtension.class)
public class PortChooserTest {

  private PortChooser portChooser;

  @BeforeEach
  protected void setUp() throws Exception {
    this.portChooser = new PortChooser();
  }

  @Test
  public void testChooseRandomPorts() {
    int numOfPorts = 100;
    int portNum = this.portChooser.chooseRandomPorts(numOfPorts);

    for (int i = 0; i < numOfPorts; i++) {
      Assert.assertTrue(this.portChooser.isPortUsed(portNum + i));
    }
  }

  @Test
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
