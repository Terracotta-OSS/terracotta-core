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
 *
 */
package com.tc.services;

import com.tc.text.PrettyPrintable;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.StateDumpable;


/**
 * Platform level service provider registry which has instances of all the service provider at the platform level.
 *
 * @author twu
 */
public interface TerracottaServiceProviderRegistry extends StateDumpable, PrettyPrintable {

  /**
   * Initialize each of the service provider with platform configuration
   *
   * @param platformConfiguration platform configuration
   * @param configuration platform configuration which each service provider can query for their service configuration
   * @param loader the classloader used for all services
   *
   */
  void initialize(PlatformConfiguration platformConfiguration, TcConfiguration configuration, ClassLoader loader);

  /**
   * Method to register platform level service provider which don't have life-cycle using SPI interface but otherwise act
   * like user-provided services.
   * Note that this serviceProvider will also be initialized with the same configuration used to initialize the registry.
   *
   * @param serviceProvider platform service provider
   */
  void registerExternal(ServiceProvider serviceProvider);

  /**
   * Method to register platform level service provider which don't have life-cycle using SPI interface and know about the
   * internal details of the implementation.
   *
   * @param serviceProvider platform service provider
   */
  void registerImplementationProvided(ImplementationProvidedServiceProvider serviceProvider);

  /**
   * Creates a entity level service registry which has list of service instances managed by the service providers
   *
   * @param consumerID The unique ID which will be used to name-space services created by this sub-registry
   * @return Service registry which will be used by entities.
   */
  InternalServiceRegistry subRegistry(long consumerID);

  void clearServiceProvidersState();

  /**
   * Normally, a server starts up in a non-active state.  The distinction between active or non-active (passive, but the
   * other intermediary states are treated the same way) is important for some of our implementation-provided services
   * (externally-provided services aren't exposed to this) so we notify them all of that change of state, here.
   */
  void notifyServerDidBecomeActive();
}
