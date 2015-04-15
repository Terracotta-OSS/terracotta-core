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
package com.tc.async.api;

/**
 * <p>
 * This interface is created to handle all cyclic reference dependency nightmares. In an ideal world, there shouldn't be
 * any cyclic references. But in the real world where we live, you have to deal with cyclic references. Its a chicken
 * and egg problem. This interface is an attempt to make it slightly less painful.
 * <p>
 * Generally Handlers in the SEDA system implement this interface. Any Manager that need references to objects that are
 * created later in the initialization sequence can also implement this interface.
 * <p>
 * The cool thing about this is that this initialization method is called in the main thread before any stage threads
 * are created. Hence you can safely read non-volatile references set in this initialize method without fearing about
 * NPE.
 * <p>
 */
public interface PostInit {

  /**
   * Initialize the state post creation
   * 
   * @param context
   */
  //TODO::rename
  public void initializeContext(ConfigurationContext context);

}
