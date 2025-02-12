/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.services;


import com.tc.objectserver.api.ManagedEntity;
import org.junit.After;
import org.junit.Before;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceProvider;


public class DelegatingServiceRegistryTest {
  DelegatingServiceRegistry registry;
  ServiceProvider provider1;
  ServiceProvider provider2;
  ImplementationProvidedServiceProvider inside1;
  
  Class<?> fakeInterface = FakeInterface.class;

  @Before
  public void setUp() throws Exception {
    provider1 = mock(ServiceProvider.class);   
    provider2 = mock(ServiceProvider.class);   
    inside1 = mock(ImplementationProvidedServiceProvider.class);
    
    when(provider1.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
    ServiceProvider[] standard = new ServiceProvider[] {provider1, provider2};
    when(provider1.getService(anyLong(), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {});

    this.registry = new DelegatingServiceRegistry(0, standard, new ImplementationProvidedServiceProvider[] {inside1});
  }

  @After
  public void tearDown() throws Exception {


  }
  
  @Test(expected=ServiceException.class)
  public void testMultipleServicesException() throws Exception {
    when(provider2.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
    when(provider2.getService(anyLong(), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {});
    this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class));
  }
  
  @Test(expected=ServiceException.class)
  public void testMultipleServicesInsideOutside() throws Exception {
    when(inside1.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
    when(inside1.getService(anyLong(), Mockito.<ManagedEntity>any(), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {});
    this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class));
  }
  
  @Test
  public void testGetService() throws Exception {
    Assert.assertNotNull(this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class)));
  }
  
  interface FakeInterface {
    
  }
}
