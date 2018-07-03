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
package com.tc.config.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.tc.test.TCExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@ExtendWith(TCExtension.class)
public class L2InfoTest {

  public L2InfoTest() {
    //
  }

  @Test
  public void testEquals() {
    L2Info inst1 = new L2Info("primary", "localhost", 9520, 9510, null, 9530, 9540, null);
    L2Info inst2 = new L2Info("primary", "localhost", 9520, 9510, null, 9530, 9540, null);
    assertEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "", 9530, 9540, null);
    inst2 = new L2Info("secondary", "localhost", 9520, 9510, "", 9530, 9540, null);
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "", 9530, 9540, "");
    inst2 = new L2Info("primary", "localhost", 9521, 9511, "", 9531, 9541, "");
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "", 9530, 9540, null);
    inst2 = new L2Info("primary", "127.0.0.1", 9520, 9510, "", 9530, 9540, null);
    assertNotEquals(inst1, inst2);
  }
}
