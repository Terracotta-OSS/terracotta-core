/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.express.TerracottaInternalClientImpl.ClientShutdownException;

public interface TerracottaInternalClient {
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
   * Returns true if the client is rejoin enabled client. Otherwise false
   */
  boolean isDedicatedClient();

  /**
   * Join the same client, introspecting modules from the specified classes
   */
  void join() throws ClientShutdownException;

  /**
   * Shuts down the client
   */
  void shutdown();
}
