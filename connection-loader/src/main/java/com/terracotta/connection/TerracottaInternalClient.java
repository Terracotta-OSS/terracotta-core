/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;


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
  <T> T instantiate(String className, Class<?>[] cstrArgTypes, Object[] cstrArgs) throws Exception;

  /**
   * Return the class using a capable class loader.
   */
  Class<?> loadClass(String className) throws ClassNotFoundException;

  /**
   * Shuts down the client
   */
  void shutdown();

  /**
   * Returns whether this client has been shutdown or not
   */
  boolean isShutdown();

  /**
   * Returns whether this client is initialized or not
   */
  boolean isInitialized();

  String getUuid();
  
  /**
   * Required for TerracottaConnectionService Connection instantiation.
   * @return The client entity manager for end-points managed by this client.
   */
  ClientEntityManager getClientEntityManager();
  
  /**
   * Required for TerracottaConnectionService Connection instantiation.
   * @return The client lock manager for end-points managed by this client.
   */
  ClientLockManager getClientLockManager();
}
