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
package com.tctest.restart;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestThreadGroup extends ThreadGroup {

  public TestThreadGroup(ThreadGroup parent, String name) {
    super(parent, name);
  }

  private final Set throwables = Collections.synchronizedSet(new HashSet());

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    super.uncaughtException(thread, throwable);
    throwables.add(throwable);
  }
  
  public Collection getErrors() {
    return new HashSet(throwables);
  }
}