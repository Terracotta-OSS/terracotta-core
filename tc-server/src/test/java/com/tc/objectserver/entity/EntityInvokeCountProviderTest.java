/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.objectserver.api.ManagedEntity;
import java.util.Collection;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.monitoring.EntityInvokeMetrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityInvokeCountProviderTest {

  private EntityManagerImpl manager;
  private EntityInvokeCountProvider provider;

  @Before
  public void setUp() {
    manager = mock(EntityManagerImpl.class);
    provider = new EntityInvokeCountProvider(manager);
  }

  @SuppressWarnings("unchecked")
  private static <T> ServiceConfiguration<T> configFor(Class<T> type) {
    ServiceConfiguration<T> config = mock(ServiceConfiguration.class);
    when(config.getServiceType()).thenReturn(type);
    return config;
  }

  @Test
  public void testProvidedServiceTypes() {
    Collection<Class<?>> types = provider.getProvidedServiceTypes();
    assertThat(types, contains((Class<?>)EntityInvokeMetrics.class));
  }

  @Test
  public void testReturnsMetricsForSupportedType() {
    EntityInvokeMetrics metrics = provider.getService(1L, mock(ManagedEntity.class), configFor(EntityInvokeMetrics.class));
    assertThat(metrics, is(notNullValue()));
    assertThat(metrics, is(instanceOf(EntityInvokeMetrics.class)));
  }

  @Test
  public void testReturnsMetricsWithNullOwningEntity() {
    assertThat(provider.getService(1L, null, configFor(EntityInvokeMetrics.class)), is(notNullValue()));
  }

  @Test
  public void testReturnsNullForUnsupportedType() {
    assertThat(provider.getService(1L, mock(ManagedEntity.class), configFor(String.class)), is(nullValue()));
  }

  @Test
  public void testGlobalCountDelegatesToManager() {
    when(manager.getInvokeCount(null, null)).thenReturn(OptionalLong.of(42L));
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    assertThat(metrics.getInvokeCount(), is(42L));
  }

  @Test
  public void testTypeCountDelegatesToManager() {
    when(manager.getInvokeCount("some.Type", null)).thenReturn(OptionalLong.of(7L));
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    assertThat(metrics.getInvokeCountForType("some.Type"), is(OptionalLong.of(7L)));
  }

  @Test
  public void testTypeCountEmptyWhenUnknown() {
    when(manager.getInvokeCount("unknown.Type", null)).thenReturn(OptionalLong.empty());
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    assertThat(metrics.getInvokeCountForType("unknown.Type"), is(OptionalLong.empty()));
  }

  @Test
  public void testIdCountDelegatesToManager() {
    when(manager.getInvokeCount("some.Type", "name")).thenReturn(OptionalLong.of(3L));
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    assertThat(metrics.getInvokeCountForId("some.Type", "name"), is(OptionalLong.of(3L)));
  }

  @Test
  public void testIdCountEmptyWhenUnknown() {
    when(manager.getInvokeCount("some.Type", "missing")).thenReturn(OptionalLong.empty());
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    assertThat(metrics.getInvokeCountForId("some.Type", "missing"), is(OptionalLong.empty()));
  }

  @Test
  public void testMetricsReflectLiveValues() {
    EntityInvokeMetrics metrics = provider.getService(1L, null, configFor(EntityInvokeMetrics.class));
    when(manager.getInvokeCount(null, null)).thenReturn(OptionalLong.of(1L));
    assertThat(metrics.getInvokeCount(), is(1L));
    when(manager.getInvokeCount(null, null)).thenReturn(OptionalLong.of(2L));
    assertThat(metrics.getInvokeCount(), is(2L));
  }

  @Test
  public void testClearAndActiveAreNoOps() throws Exception {
    provider.clear();
    provider.serverDidBecomeActive();
  }
}
