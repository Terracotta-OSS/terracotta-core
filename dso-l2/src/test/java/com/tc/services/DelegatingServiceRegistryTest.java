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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceProvider;

import com.tc.objectserver.api.ManagedEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
                 

public class DelegatingServiceRegistryTest {
  DelegatingServiceRegistry registry;
  ServiceProvider provider1;
  ServiceProvider provider2;
  ImplementationProvidedServiceProvider inside1;
  
  Class<?> fakeInterface = FakeInterface.class;

  @BeforeEach
  public void setUp() throws Exception {
    provider1 = mock(ServiceProvider.class);   
    provider2 = mock(ServiceProvider.class);   
    inside1 = mock(ImplementationProvidedServiceProvider.class);
    
    when(provider1.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
    ServiceProvider[] standard = new ServiceProvider[] {provider1, provider2};
    when(provider1.getService(anyLong(), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {});

    this.registry = new DelegatingServiceRegistry(0, standard, new ImplementationProvidedServiceProvider[] {inside1});
  }

  @AfterEach
  public void tearDown() throws Exception {


  }

  @Test
  public void testMultipleServicesException() throws Exception {
    Throwable t = assertThrows(ServiceException.class, ()-> {
      when(provider2.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
      when(provider2.getService(anyLong(), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {});
      this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class));
    });
  }
  
  @Test
  public void testMultipleServicesInsideOutside() throws Exception {
    Throwable t = assertThrows(ServiceException.class, ()-> {
      when(inside1.getProvidedServiceTypes()).thenReturn(Arrays.asList(fakeInterface));
      when(inside1.getService(anyLong(), any(ManagedEntity.class), any(ServiceConfiguration.class))).thenReturn(new FakeInterface() {
      });
      this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class));
    });
  }
  
  @Test
  public void testGetService() throws Exception {
    assertNotNull(this.registry.getService(new BasicServiceConfiguration<>(FakeInterface.class)));
  }
  
  interface FakeInterface {
    
  }
}
