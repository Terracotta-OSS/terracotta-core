/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
