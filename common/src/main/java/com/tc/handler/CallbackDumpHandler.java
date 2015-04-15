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
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CallbackDumpHandler implements CallbackOnExitHandler {
  private final List<CallbackDumpAdapter> dumpObjectList = new CopyOnWriteArrayList<CallbackDumpAdapter>();

  public void registerForDump(CallbackDumpAdapter dumpObject) {
    this.dumpObjectList.add(dumpObject);
  }

  public void dump() {
    for (CallbackDumpAdapter dumpObject : this.dumpObjectList) {
      dumpObject.callbackOnExit(null);
    }
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    dump();
  }
}
