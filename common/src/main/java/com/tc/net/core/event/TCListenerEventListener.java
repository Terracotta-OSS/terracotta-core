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
 */
package com.tc.net.core.event;

/**
 * Interface for those interested in listening to events of TCListeners
 * 
 * @author teck
 */
public interface TCListenerEventListener {

  /**
   * Called once (and only once) after a listener is actually closed
   * 
   * @param event
   */
  public void closeEvent(TCListenerEvent event);
}
