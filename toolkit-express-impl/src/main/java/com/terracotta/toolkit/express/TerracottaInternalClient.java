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
package com.terracotta.toolkit.express;


public interface TerracottaInternalClient {

  /**
   * Initialize the client. This will start the client to connect to the L2.
   */
  void init();

  /**
   * Instantiates a class using an internal instrumentation capable class loader.
   * <p>
   * Class loaded through an instrumentation capable loader can interact directly through class scoped linkage with the
   * toolkit API.
   * 
   * @param <T> a public java super-type or interface of {@code className}
   * @param className concrete class to instantiate
   * @param cstrArgTypes array of constructor argument types
   * @param cstrArgs array of constructor arguments
   * @return newly constructed cluster loader java object
   * @throws Exception if the class could not be loaded or instantiated
   */
  <T> T instantiate(String className, Class[] cstrArgTypes, Object[] cstrArgs) throws Exception;

  /**
   * Return the class using a capable class loader.
   */
  Class loadClass(String className) throws ClassNotFoundException;

  /**
   * Shuts down the client
   */
  void shutdown();

  /**
   * Returns whether this client has been shutdown or not
   */
  boolean isShutdown();

  /**
   * Returns the PlatformService.
   */
  Object getPlatformService();

  /**
   * Returns whether this client is online or not
   */
  boolean isOnline();

  /**
   * Returns whether this client is initialized or not
   */
  boolean isInitialized();

  Object getAbortableOperationManager();

  String getUuid();

}
