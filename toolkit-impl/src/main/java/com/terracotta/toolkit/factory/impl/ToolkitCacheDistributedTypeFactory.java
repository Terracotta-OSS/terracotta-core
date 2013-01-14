/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import static com.terracotta.toolkit.config.ConfigUtil.distributeInStripes;

import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeType;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.InternalToolkitMap;
import com.terracotta.toolkit.collections.map.ServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.config.ConfigUtil;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectStripe;
import com.terracotta.toolkit.object.ToolkitObjectStripeImpl;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.type.DistributedClusteredObjectLookup;
import com.terracotta.toolkit.type.DistributedToolkitTypeFactory;

import java.io.Serializable;

/**
 * An implementation of {@link DistributedToolkitTypeFactory} for ClusteredMap's
 */
public class ToolkitCacheDistributedTypeFactory<K extends Serializable, V extends Serializable> implements
    DistributedToolkitTypeFactory<ToolkitCacheImpl<K, V>, InternalToolkitMap<K, V>> {

  private static final ServerMap[]         EMPTY_SERVER_MAP_ARRAY = new ServerMap[0];
  private static final Configuration[]     EMPTY_CONFIG_ARRAY     = new Configuration[0];
  private static final String              SEARCH_ATTR_TYPES_MAP_SUFFIX = "searchAttributeTypesMap";

  private final SearchFactory       searchBuilderFactory;
  private final ServerMapLocalStoreFactory serverMapLocalStoreFactory;

  public ToolkitCacheDistributedTypeFactory(SearchFactory searchBuilderFactory,
                                            ServerMapLocalStoreFactory serverMapLocalStoreFactory) {
    this.searchBuilderFactory = searchBuilderFactory;
    this.serverMapLocalStoreFactory = serverMapLocalStoreFactory;
  }


  @Override
  public ToolkitCacheImpl<K, V> createDistributedType(ToolkitInternal toolkit, ToolkitObjectFactory factory,
                                                      DistributedClusteredObjectLookup<InternalToolkitMap<K, V>> lookup,
                                                      String name,
                                                      ToolkitObjectStripe<InternalToolkitMap<K, V>>[] stripeObjects,
                                                      Configuration configuration, PlatformService platformService) {
    validateExistingClusterWideConfigs(stripeObjects, configuration);
    ToolkitMap<String, ToolkitAttributeType> attrSchema = toolkit.getMap(name + "|" + SEARCH_ATTR_TYPES_MAP_SUFFIX,
                                                                         String.class, ToolkitAttributeType.class);
    AggregateServerMap aggregateServerMap = new AggregateServerMap(factory.getManufacturedToolkitObjectType(),
                                                                   searchBuilderFactory, lookup, name, stripeObjects,
                                                                   configuration, attrSchema, serverMapLocalStoreFactory,
                                                                   platformService);
    return new ToolkitCacheImpl<K, V>(factory, toolkit, name, aggregateServerMap);
  }

  @Override
  public ToolkitObjectStripe<InternalToolkitMap<K, V>>[] createStripeObjects(String name, Configuration mapConfig,
                                                                             int numStripes) {
    // creating stripe objects for first time
    ToolkitObjectStripe<InternalToolkitMap<K, V>>[] rv = new ToolkitObjectStripe[numStripes];
    Configuration[] stripesConfig = createConfigForStripes(numStripes, mapConfig);
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new ToolkitObjectStripeImpl<InternalToolkitMap<K, V>>(stripesConfig[i],
                                                                    getComponentsForStripe(name, stripesConfig[i]));
    }
    return rv;
  }

  private Configuration[] createConfigForStripes(int numStripes, Configuration mapConfig) {
    return distributeConfigAmongStripes(mapConfig, numStripes);
  }

  private ServerMap[] getComponentsForStripe(String name, Configuration config) {
    int numSegments = (Integer) InternalCacheConfigurationType.CONCURRENCY.getValueIfExistsOrDefault(config);

    if (numSegments <= 0) { return EMPTY_SERVER_MAP_ARRAY; }

    ServerMap[] rv = new ServerMap[numSegments];
    Configuration[] segmentConfigs = createConfigForStripes(numSegments, config);
    for (Configuration serverMapConfig : segmentConfigs) {
      Integer concurrency = (Integer) InternalCacheConfigurationType.CONCURRENCY.getValueIfExists(serverMapConfig);
      if (concurrency == null) {
        throw new IllegalArgumentException("Concurrency field missing in config");
      } else if (concurrency != 1) {
        // TODO: write a unit test instead, also check/test for other types
        throw new AssertionError("Configuration for one ServerMap should have concurrency=1");
      }
    }
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new ServerMap(segmentConfigs[i], name);
    }
    return rv;
  }

  @Override
  public void validateExistingLocalInstanceConfig(ToolkitCacheImpl<K, V> existingCache, Configuration newConfig) {
    Configuration oldConfig = existingCache.getConfiguration();
    for (InternalCacheConfigurationType configType : InternalCacheConfigurationType.values()) {
      Object existingValue = configType.getValueIfExists(oldConfig);
      // doing this instead of getExistingValueOrException to report better msg
      if (existingValue == null) {
        throw new IllegalArgumentException("'" + configType.getConfigString()
                                           + "' cannot be null for already existing values in local node");
      } else if (configType.getValueIfExists(newConfig) != null) {
        // only validate if user passed in a value
        configType.validateExistingMatchesValueFromConfig(existingValue, newConfig);
      }
    }
  }

  @Override
  public void validateConfig(Configuration config) {
    for (InternalCacheConfigurationType configType : InternalCacheConfigurationType.values()) {
      if (config.hasField(configType.name())) {
        configType.validateLegalValue(config.getObjectOrNull(configType.name()));
      }

    }
  }

  @Override
  public Configuration newConfigForCreationInCluster(Configuration configuration) {
    UnclusteredConfiguration newConfig = new UnclusteredConfiguration(ConfigUtil.getDefaultCacheConfig());
    for (String key : configuration.getKeys()) {
      // update values passed in user config
      newConfig.setObject(key, configuration.getObjectOrNull(key));
    }
    return newConfig;
  }

  @Override
  public Configuration newConfigForCreationInLocalNode(ToolkitObjectStripe<InternalToolkitMap<K, V>>[] existingStripedObjects,
                                                       Configuration userConfig) {
    UnclusteredConfiguration newConfig = new UnclusteredConfiguration();
    for (String key : userConfig.getKeys()) {
      // copy all config values passed in user config
      newConfig.setObject(key, userConfig.getObjectOrNull(key));
    }
    mergeMissingConfigsFromExistingConfig(existingStripedObjects, newConfig);
    return newConfig;
  }

  private static void validateExistingClusterWideConfigs(ToolkitObjectStripe[] stripeObjects, Configuration newConfig) {
    int concurrency = 0;
    int maxCount = 0;
    for (ToolkitObjectStripe stripeObject : stripeObjects) {
      Configuration oldConfig = stripeObject.getConfiguration();
      for (InternalCacheConfigurationType configType : InternalCacheConfigurationType.getClusterWideConfigs()) {
        Object existingValue = configType.getValueIfExists(oldConfig);
        // doing this instead of getExistingValueOrException to report better msg
        if (existingValue == null) { throw new IllegalArgumentException(
                                                                        "'"
                                                                            + configType.getConfigString()
                                                                            + "' cannot be null in existing config from cluster"); }
        switch (configType) {
          case CONCURRENCY: {
            concurrency += ((Integer) existingValue);
            break;
          }
          case MAX_TOTAL_COUNT: {
            maxCount += ((Integer) existingValue);
            break;
          }
          default: {
            configType.validateExistingMatchesValueFromConfig(existingValue, newConfig);
          }
        }
      }
    }

    InternalCacheConfigurationType.CONCURRENCY.validateExistingMatchesValueFromConfig(concurrency, newConfig);
    InternalCacheConfigurationType.MAX_TOTAL_COUNT.validateExistingMatchesValueFromConfig(maxCount < 0 ? -1 : maxCount, newConfig);
  }

  private static void mergeMissingConfigsFromExistingConfig(ToolkitObjectStripe[] stripeObjects,
                                                            UnclusteredConfiguration newConfig) {
    for (InternalCacheConfigurationType configType : InternalCacheConfigurationType.values()) {
      if (!newConfig.hasField(configType.getConfigString())) {
        // missing in newConfig, merge from existing value
        Serializable existingValue = getExistingValueOrException(configType, stripeObjects);
        configType.setValue(newConfig, existingValue);
      }
    }
  }

  private static Serializable getExistingValueOrException(InternalCacheConfigurationType configType,
                                                          ToolkitObjectStripe[] stripeObjects) {
    int concurrency = 0;
    int maxTotalCount = 0;
    for (ToolkitObjectStripe cos : stripeObjects) {
      Object existingValue = configType.getValueIfExists(cos.getConfiguration());
      if (existingValue == null) { throw new IllegalArgumentException(
                                                                      "'"
                                                                          + configType.getConfigString()
                                                                          + "' cannot be null in existing config from cluster"); }
      switch (configType) {
        case CONCURRENCY: {
          concurrency += (Integer) existingValue;
          break;
        }
        case MAX_TOTAL_COUNT: {
          maxTotalCount += (Integer) existingValue;
          break;
        }
        default:
          // just use the first stripe to get config as it should be same everywhere
          // TODO: assert that all stripes has same config?
          return configType.getValueIfExists(cos.getConfiguration());
      }
    }
    switch (configType) {
      case CONCURRENCY:
        return concurrency;
      case MAX_TOTAL_COUNT:
        return maxTotalCount < 0 ? -1 : maxTotalCount;
      default:
        throw new AssertionError("not reachable, something wrong");
    }
  }

  // used in tests
  protected static Configuration[] distributeConfigAmongStripes(Configuration config, int numberStripes) {
    if (numberStripes <= 0) { return EMPTY_CONFIG_ARRAY; }
    UnclusteredConfiguration[] configurations = new UnclusteredConfiguration[numberStripes];
    for (int i = 0; i < configurations.length; i++) {
      configurations[i] = new UnclusteredConfiguration(config);
    }

    final int overallConcurrency = config.getInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME);
    final int overallMaxTotalCount = config.getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME);

    // distribute concurrency across stripes
    int[] concurrencies = distributeInStripes(overallConcurrency, numberStripes);
    if (concurrencies.length != numberStripes) { throw new AssertionError(); }
    for (int i = 0; i < concurrencies.length; i++) {
      configurations[i].setInt(ToolkitConfigFields.CONCURRENCY_FIELD_NAME, concurrencies[i]);
    }

    // divide maxTotalCount using overallConcurrency in case its smaller than numberStripes
    int divisor = overallConcurrency < numberStripes ? overallConcurrency : numberStripes;
    int[] maxTotalCounts = distributeInStripes(overallMaxTotalCount, divisor);
    if (maxTotalCounts.length != divisor) { throw new AssertionError(); }
    for (int i = 0; i < configurations.length; i++) {
      if (overallMaxTotalCount < 0) {
        configurations[i].setInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, -1);
      } else if (i < maxTotalCounts.length) {
        configurations[i].setInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, maxTotalCounts[i]);
      } else {
        // use 0 in case numberStripes more than overallConcurrency for non-participating stripes
        configurations[i].setInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, 0);
      }
    }

    return configurations;
  }
}
