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
package com.tc.net.protocol.delivery;

/**
 * 
 */
public class AbstractState implements State {

  private final String name;

  public AbstractState(String name) {
    this.name = name;
  }

  @Override
  public void enter() {
    // override me if you want
  }

  @Override
  public void execute(OOOProtocolMessage protocolMessage) {
    // override me if you want
  }

  @Override
  public String toString() {
    return name;
  }
}
