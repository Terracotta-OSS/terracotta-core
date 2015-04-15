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
