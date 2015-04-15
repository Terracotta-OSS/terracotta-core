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
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.TransformerLookup;
import org.terracotta.corestorage.bigmemory.ImmutableBigMemoryKeyValueStorageConfig;
import org.terracotta.corestorage.bigmemory.OffHeapStorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;
import org.terracotta.offheapstore.util.MemoryUnit;

import com.tc.objectserver.persistence.offheap.DataStorageConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author twu
 */
public class OffheapStorageManagerFactory implements StorageManagerFactory {
  protected final DataStorageConfig offHeapConfig;

  public OffheapStorageManagerFactory(final DataStorageConfig offHeapConfig) {
    if (!offHeapConfig.enabled()) {
      throw new IllegalArgumentException("Offheap is not enabled.");
    }
    this.offHeapConfig = offHeapConfig;
  }

  @Override
  public StorageManager createStorageManager(Map<String, KeyValueStorageConfig<?, ?>> configMap, TransformerLookup transformerLookup) throws IOException {
    return new OffHeapStorageManager(offHeapConfig.getOffheapSize(), offHeapConfig.getMinChunkSize(), offHeapConfig.getMaxChunkSize(), MemoryUnit.BYTES, configMap, transformerLookup) {
      @Override
      public Collection<MonitoredResource> getMonitoredResources() {
        return wrapMonitoredResources(super.getMonitoredResources(), offHeapConfig.getMaxDataStorageSize());
      }
    };
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final KeyValueStorageConfig<K, V> baseConfig, Type nt) {
    return ImmutableBigMemoryKeyValueStorageConfig.builder(baseConfig)
        .initialPageSize(offHeapConfig.getObjectInitialDataSize())
        .maximalPageSize(offHeapConfig.getMaxMapPageSize())
        .offheapMode(offHeapConfig.getObjectMode(nt))
        .initialTableSize(offHeapConfig.getObjectTableSize()).build();
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final KeyValueStorageConfig<K, V> baseConfig) {
    return ImmutableBigMemoryKeyValueStorageConfig.builder(baseConfig)
        .initialPageSize(offHeapConfig.getMinMapPageSize())
        .maximalPageSize(offHeapConfig.getMaxMapPageSize())
        .offheapMode(offHeapConfig.getMapMode())
        .initialTableSize(offHeapConfig.getMapTableSize())
        .build();
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapObjectDBConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder, Type nt) {
    return wrapObjectDBConfig(builder.concurrency(offHeapConfig.getObjectConcurrency()).build(), nt);
  }

  @Override
  public <K, V> KeyValueStorageConfig<K, V> wrapMapConfig(final ImmutableKeyValueStorageConfig.Builder<K, V> builder) {
    return wrapMapConfig(builder.build());
  }

  protected Collection<MonitoredResource> wrapMonitoredResources(Collection<MonitoredResource> parent, long maxDataSize) {
    List<MonitoredResource> pass = new ArrayList<MonitoredResource>(parent.size());
    for (MonitoredResource rsrc : parent) {
      switch (rsrc.getType()) {
        case DATA:
          pass.add(new DatasizeMonitoredResource(rsrc, maxDataSize));
          break;
        case OFFHEAP:
          pass.add(rsrc);
          break;
        default:
          pass.add(rsrc);
      }
    }
    return Collections.unmodifiableList(pass);
  }

  private static class DatasizeMonitoredResource implements MonitoredResource {

    private final MonitoredResource delegate;
    private final long maxDataSize;

    public DatasizeMonitoredResource(MonitoredResource delegate, long maxsize) {
      this.delegate = delegate;
      this.maxDataSize = maxsize;
    }

    @Override
    public Type getType() {
      return delegate.getType();
    }

    @Override
    public long getVital() {
      return delegate.getVital();
    }

    @Override
    public long getUsed() {
      return delegate.getUsed();
    }

    @Override
    public long getReserved() {
      return delegate.getReserved();
    }

    @Override
    public Runnable addUsedThreshold(Direction direction, long value, Runnable action) {
      return delegate.addUsedThreshold(direction, value, action);
    }

    @Override
    public Runnable removeUsedThreshold(Direction direction, long value) {
      return delegate.removeUsedThreshold(direction, value);
    }

    @Override
    public Runnable addReservedThreshold(Direction direction, long value, Runnable action) {
      return delegate.addReservedThreshold(direction, value, action);
    }

    @Override
    public Runnable removeReservedThreshold(Direction direction, long value) {
      return delegate.removeReservedThreshold(direction, value);
    }

    @Override
    public long getTotal() {
      return maxDataSize;
    }
  }
}
