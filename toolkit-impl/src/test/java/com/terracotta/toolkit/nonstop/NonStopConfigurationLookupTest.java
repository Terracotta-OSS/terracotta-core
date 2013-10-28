/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.ToolkitObjectType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class NonStopConfigurationLookupTest {

  private NonStopContext nonStopContext = mock(NonStopContext.class);

  @Before
  public void setUp() {
    when(nonStopContext.getNonStopConfigurationRegistry()).thenReturn(new NonStopConfigRegistryImpl());
    when(nonStopContext.isEnabledForCurrentThread()).thenReturn(true);
  }

  @Test
  public void testEnabledForCurrentThreadGetNonStopConfiguration() throws Exception {
    NonStopConfigurationLookup lookup = new NonStopConfigurationLookup(nonStopContext, ToolkitObjectType.CACHE, "testName");
    assertThat(lookup.getNonStopConfiguration().isEnabled(), is(true));
  }

  @Test
  public void testDisabledForCurrentThreadGetNonStopConfiguration() throws Exception {
    when(nonStopContext.isEnabledForCurrentThread()).thenReturn(false);
    NonStopConfigurationLookup lookup = new NonStopConfigurationLookup(nonStopContext, ToolkitObjectType.CACHE, "testName");
    assertThat(lookup.getNonStopConfiguration().isEnabled(), is(false));
  }

}
