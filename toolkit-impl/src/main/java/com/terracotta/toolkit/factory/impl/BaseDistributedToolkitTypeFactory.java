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
package com.terracotta.toolkit.factory.impl;

import static com.terracotta.toolkit.config.ConfigUtil.distributeInStripes;

import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;
import com.terracotta.toolkit.type.DistributedToolkitTypeFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Eugene Shelestovich
 */
public abstract class BaseDistributedToolkitTypeFactory<K extends Serializable, V extends Serializable> implements
    DistributedToolkitTypeFactory<ToolkitCacheImpl<K, V>, InternalToolkitMap<K, V>> {
  private static final TCLogger logger = TCLogging.getLogger(BaseDistributedToolkitTypeFactory.class);

  private static final UnclusteredConfiguration[] EMPTY_CONFIG_ARRAY = new UnclusteredConfiguration[0];
  private static final ServerMap[] EMPTY_SERVER_MAP_ARRAY = new ServerMap[0];
  private static final String SEARCH_ATTR_TYPES_MAP_SUFFIX = "searchAttributeTypesMap";

  private final SearchFactory searchBuilderFactory;
  private final ServerMapLocalStoreFactory serverMapLocalStoreFactory;

  protected BaseDistributedToolkitTypeFactory(SearchFactory searchBuilderFactory,
                                              ServerMapLocalStoreFactory serverMapLocalStoreFactory) {
    this.searchBuilderFactory = searchBuilderFactory;
    this.serverMapLocalStoreFactory = serverMapLocalStoreFactory;
  }

  protected abstract void validateNewConfiguration(Configuration configuration);

  protected abstract Configuration getDefaultConfiguration();

  protected abstract Set<InternalCacheConfigurationType> getAllSupportedConfigs();

  @Override
  public ToolkitCacheImpl<K, V> createDistributedType(final ToolkitInternal toolkit, ToolkitObjectFactory factory,
                                                      DistributedClusteredObjectLookup<InternalToolkitMap<K, V>> lookup,
                                                      final String name,
                                                      ToolkitObjectStripe<InternalToolkitMap<K, V>>[] stripeObjects,
                                                      Configuration configuration, PlatformService platformService, ToolkitLock configMutationLock) {
    validateNewConfiguration(configuration);
    validateExistingClusterWideConfigs(stripeObjects, configuration);
    Callable<ToolkitMap<String, String>> schemaCreator = new Callable() {
      @Override
      public ToolkitMap<String, String> call() throws Exception {
        return toolkit.getMap(name + '|' + SEARCH_ATTR_TYPES_MAP_SUFFIX, String.class, String.class);
      }
    };

    AggregateServerMap aggregateServerMap = new AggregateServerMap(factory.getManufacturedToolkitObjectType(),
                                                                   searchBuilderFactory, lookup, name, stripeObjects,
                                                                   configuration, schemaCreator,
                                                                   serverMapLocalStoreFactory, platformService, configMutationLock);
    return new ToolkitCacheImpl<K, V>(factory, name, aggregateServerMap, platformService, toolkit);
  }

  @Override
  public ToolkitObjectStripe<InternalToolkitMap<K, V>>[] createStripeObjects(final String name,
                                                                             final Configuration config,
                                                                             final int numStripes,
                                                                             PlatformService platformService) {
    // creating stripe objects for first time
    ToolkitObjectStripe<InternalToolkitMap<K, V>>[] rv = new ToolkitObjectStripe[numStripes];
    Configuration[] stripesConfig = distributeConfigAmongStripes(config, numStripes);
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new ToolkitObjectStripeImpl<InternalToolkitMap<K, V>>(
          stripesConfig[i], getComponentsForStripe(name, stripesConfig[i], platformService));
    }
    return rv;
  }

  private ServerMap[] getComponentsForStripe(final String name, final Configuration config,
                                             PlatformService platformService) {
    int numSegments = (Integer)InternalCacheConfigurationType.CONCURRENCY.getValueIfExistsOrDefault(config);
    if (numSegments <= 0) return EMPTY_SERVER_MAP_ARRAY;

    final ServerMap[] rv = new ServerMap[numSegments];
    final Configuration[] segmentConfigs = distributeConfigAmongStripes(config, numSegments);
    for (Configuration serverMapConfig : segmentConfigs) {
      Integer concurrency = (Integer)InternalCacheConfigurationType.CONCURRENCY.getValueIfExists(serverMapConfig);
      if (concurrency == null) {
        throw new IllegalArgumentException("Concurrency field missing in config");
      } else if (concurrency != 1) {
        // TODO: write a unit test instead, also check/test for other types
        throw new AssertionError("Configuration for one ServerMap should have concurrency=1");
      }
    }
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new ServerMap(segmentConfigs[i], name, platformService);
    }
    return rv;
  }

  @Override
  public void validateExistingLocalInstanceConfig(ToolkitCacheImpl<K, V> existingCache, Configuration newConfig) {
    final Configuration oldConfig = existingCache.getConfiguration();
    for (InternalCacheConfigurationType configType : getAllSupportedConfigs()) {
      Object existingValue = configType.getValueIfExists(oldConfig);
      // doing this instead of getExistingValueOrException to report better msg
      if (existingValue == null && !hasConflictingField(oldConfig, configType.getConfigString())) {
        throw new IllegalArgumentException('\'' + configType.getConfigString()
                                           + "' cannot be null for already existing values in local node");
      } else if (configType.getValueIfExists(newConfig) != null) {
        // only validate if user passed in a value
        configType.validateExistingMatchesValueFromConfig(existingValue, newConfig);
      }
    }
  }

  @Override
  public void validateConfig(Configuration config) {
    for (InternalCacheConfigurationType configType : getAllSupportedConfigs()) {
      if (config.hasField(configType.getConfigString())) {
        configType.validateLegalValue(config.getObjectOrNull(configType.getConfigString()));
      }
    }
  }

  @Override
  public Configuration newConfigForCreationInCluster(final Configuration userConfig) {
    final UnclusteredConfiguration newConfig = new UnclusteredConfiguration(userConfig);
    final Configuration defaultConfiguration = getDefaultConfiguration();
    for (String key : defaultConfiguration.getKeys()) {
      if (!newConfig.hasField(key) && !newConfig.hasConflictingField(key)) {
        // add the values from default configuration
        newConfig.setObject(key, defaultConfiguration.getObjectOrNull(key));
      }
    }
    return newConfig;
  }

  @Override
  public Configuration newConfigForCreationInLocalNode(String name, ToolkitObjectStripe<InternalToolkitMap<K, V>>[] existingStripedObjects,
                                                       Configuration userConfig) {
    final UnclusteredConfiguration newConfig = new UnclusteredConfiguration(userConfig);
    Collection<Configuration> configurations = getConfigurations(existingStripedObjects);
    for (InternalCacheConfigurationType configType : getAllSupportedConfigs()) {
      String field = configType.getConfigString();
      Serializable existingValue = configType.getAggregatedConfigValue(configurations);
      if ((!newConfig.hasField(field) && !newConfig.hasConflictingField(field)
          && !hasConflictingField(existingStripedObjects[0].getConfiguration(), field))) {
        // missing in newConfig, merge from existing value
        configType.setValue(newConfig, existingValue);
      } else if (configType.acceptOverride() && !existingValue.equals(configType.getValueIfExists(newConfig))) {
        logger.warn("Overriding configuration value '" + configType.getValueIfExists(newConfig) + "' for field '"
                    + field + "' from TSA with value '" + existingValue + "' for cache '" + name + "'.");
        configType.setValue(newConfig, existingValue);
      }
    }
    return newConfig;
  }

  private boolean hasConflictingField(Configuration config, String name) {
    // TODO: change this when hasConflictingField is moved in Configuration or AbstractConfiguration
    return new UnclusteredConfiguration(config).hasConflictingField(name);
  }

  protected UnclusteredConfiguration[] distributeConfigAmongStripes(Configuration config, int numberStripes) {
    if (numberStripes <= 0) return EMPTY_CONFIG_ARRAY;

    final UnclusteredConfiguration[] configurations = new UnclusteredConfiguration[numberStripes];
    for (int i = 0; i < configurations.length; i++) {
      configurations[i] = new UnclusteredConfiguration(config);
    }

    final int overallConcurrency = config.getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME);
    // distribute concurrency across stripes
    int[] concurrencies = distributeInStripes(overallConcurrency, numberStripes);
    if (concurrencies.length != numberStripes) { throw new AssertionError(); }
    for (int i = 0; i < concurrencies.length; i++) {
      configurations[i].setInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME, concurrencies[i]);
    }
    return configurations;
  }

  protected static Collection<Configuration> getConfigurations(final ToolkitObjectStripe[] stripeObjects) {
    return Lists.transform(Arrays.asList(stripeObjects), new Function<ToolkitObjectStripe, Configuration>() {
      @Override
      public Configuration apply(final ToolkitObjectStripe input) {
        return input.getConfiguration();
      }
    });
  }

  protected void validateExistingClusterWideConfigs(final ToolkitObjectStripe[] stripeObjects, final Configuration newConfig) {
    Collection<Configuration> configurations = getConfigurations(stripeObjects);

    for (InternalCacheConfigurationType configType : getAllSupportedConfigs()) {
      final Object existingValue = configType.getAggregatedConfigValue(configurations);
      configType.validateExistingMatchesValueFromConfig(existingValue, newConfig);
    }
  }
}
