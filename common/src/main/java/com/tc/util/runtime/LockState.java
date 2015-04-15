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
package com.tc.util.runtime;

public class LockState {
  public static final LockState HOLDING    = new LockState("HOLDING LOCK");
  public static final LockState WAITING_ON = new LockState("WAITING ON LOCK");
  public static final LockState WAITING_TO = new LockState("WAITING TO LOCK");

  private final String          state;

  private LockState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return this.state;
  }

}
