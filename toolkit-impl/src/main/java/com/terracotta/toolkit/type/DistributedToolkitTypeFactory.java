/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.type;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;

/**
 * A factory thats responsible for creating objects necessary for an AggregateToolkitType
 * 
 * @param T The type of the aggregate
 * @param C The type of the clustered object component of the aggregate type
 */
public interface DistributedToolkitTypeFactory<T extends DistributedToolkitType<S>, S extends TCToolkitObject> {

  /**
   * Create the actual <tt>AggregateToolkitType</tt> with the specified objects from stripes
   *
   * @param name
   * @param configMutationLock
   */
  T createDistributedType(ToolkitInternal toolkit, ToolkitObjectFactory factory,
                          DistributedClusteredObjectLookup<S> lookup, String name,
                          ToolkitObjectStripe<S>[] stripeObjects, Configuration configuration,
                          PlatformService platformService, ToolkitLock configMutationLock);

  /**
   * Create the stripe objects
   */
  ToolkitObjectStripe<S>[] createStripeObjects(String name, Configuration configuration, int numStripes,
                                               PlatformService platformService);

  /**
   * @throws IllegalArgumentException if the existingObject has conflicting configuration with the passed in parameter
   */
  void validateExistingLocalInstanceConfig(T existingObject, Configuration config) throws IllegalArgumentException;

  /**
   * Creates and returns a new Configuration for creating this distributed type first time in the <b>cluster</b>.
   * Populates values that already exists in the passed configuration. For missing values in the config, populates
   * default values
   */
  Configuration newConfigForCreationInCluster(Configuration userConfig);

  /**
   * Creates and returns a new Configuration for creating this distributed type first time in <b>current node</b>.
   * Populates values that already exists in the passed configuration. For missing values in the config, uses existing
   * values from existing cluster instance configuration
   */
  Configuration newConfigForCreationInLocalNode(String name, ToolkitObjectStripe<S>[] existingStripedObjects,
                                                Configuration userConfig);

  /**
   * @throws IllegalArgumentException if the configuration passed has Illegal Values which are not Supported.
   */
  void validateConfig(Configuration config) throws IllegalArgumentException;

}
