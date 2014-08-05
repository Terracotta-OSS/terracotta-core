/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import static org.mockito.Mockito.mock;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.terracotta.toolkit.internal.feature.ToolkitManagementEvent;

import com.tc.management.TCManagementEvent;
import com.tc.platform.PlatformService;

import java.util.concurrent.ExecutorService;

import junit.framework.TestCase;

public class NonStopManagementInternalFeatureImplTest extends TestCase {

  public void test() {
    NonStopManagementInternalFeatureImpl feature = new NonStopManagementInternalFeatureImpl();

    try {
      feature.registerManagementService(null, null);
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }

    try {
      feature.unregisterManagementService(null);
      fail();
    } catch (IllegalStateException ise) {
      // expected
    }

    feature.sendEvent(new ToolkitManagementEvent());

    PlatformService ps = mock(PlatformService.class);
    feature.setPlatformService(ps);
    Mockito.verify(ps, Mockito.times(1)).sendEvent(Matchers.any(TCManagementEvent.class));

    Object service = new Object();
    ExecutorService executor = mock(ExecutorService.class);
    Object serviceID = feature.registerManagementService(service, executor);
    Mockito.verify(ps, Mockito.times(1)).registerManagementService(service, executor);

    feature.unregisterManagementService(serviceID);
    Mockito.verify(ps, Mockito.times(1)).unregisterManagementService(serviceID);
  }

}
