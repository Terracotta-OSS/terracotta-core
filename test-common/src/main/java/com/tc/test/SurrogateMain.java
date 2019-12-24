/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Core.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */
package com.tc.test;

import static com.tc.test.ScriptTestUtil.showArguments;
import static com.tc.test.ScriptTestUtil.showEnvironment;
import static com.tc.test.ScriptTestUtil.showProperties;

/**
 * Surrogate class used by script testing as a substitute for the main
 * class called by a script under testing.
 *
 * @see BaseScriptTest
 */
public class SurrogateMain {
  public static void main(String[] args) {
    showArguments(System.out, args);
    showProperties(System.out);
    showEnvironment(System.out);
  }
}
