/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of possible test categories.
 */
public enum TestCategory {
  /**
   * A production test that has been vetted by QA.
   */
  PRODUCTION,

  /**
   * A test that has been broken and is quarantined.
   */
  QUARANTINED,

  /**
   * A test that is new and has not yet been vetted by QA.
   */
  TRIAGED,

  /**
   * A test that has not been categorized. In the current monkey staging process, uncategorized tests are treated as
   * triaged.
   */
  UNCATEGORIZED;

  private static final Map<String, TestCategory> stringToCategory = new HashMap();
  static {
    for (TestCategory category : values()) {
      stringToCategory.put(category.toString().toUpperCase(), category);
    }
  }

}
