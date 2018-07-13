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

import com.tc.test.EqualityChecker;
import com.tc.test.TCExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit test for {@link LicenseType}.
 */
@ExtendWith(TCExtension.class)
public class LicenseTypeTest {

  @Test
  public void testAll() throws Exception {
    Object[] arr1 = new Object[] { LicenseType.TRIAL, LicenseType.PRODUCTION, LicenseType.NONE };
    Object[] arr2 = new Object[] { LicenseType.TRIAL, LicenseType.PRODUCTION, LicenseType.NONE };

    EqualityChecker.checkArraysForEquality(arr1, arr2);

    assertFalse(LicenseType.TRIAL.equals(null));
    assertFalse(LicenseType.TRIAL.equals("trial"));
    assertFalse(LicenseType.TRIAL.equals("foo"));

    LicenseType.TRIAL.toString();
    LicenseType.PRODUCTION.toString();
    LicenseType.NONE.toString();
  }

}
