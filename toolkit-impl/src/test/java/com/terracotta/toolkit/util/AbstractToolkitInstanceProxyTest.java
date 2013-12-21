package com.terracotta.toolkit.util;

import org.junit.Before;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.nonstop.NonStopClusterListener;
import com.terracotta.toolkit.nonstop.NonStopContext;
import com.terracotta.toolkit.nonstop.NonStopManager;
import com.terracotta.toolkit.nonstop.NonstopTimeoutBehaviorResolver;
import com.terracotta.toolkit.nonstop.ToolkitObjectLookup;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public abstract class AbstractToolkitInstanceProxyTest {

  protected NonStopConfiguration nonStopConfiguration;
  protected NonStopConfigurationRegistry nonStopConfigurationRegistry;
  protected NonStopContext nonStopContext;

  @Before
  public void setUp() throws Exception {
    nonStopConfiguration = nonStopConfiguration();
    nonStopConfigurationRegistry = nonStopConfigurationRegistry(nonStopConfiguration);
    nonStopContext = nonStopContext(nonStopConfigurationRegistry);
  }

  private NonStopConfiguration nonStopConfiguration() {
    NonStopConfiguration nonStopConfig = mock(NonStopConfiguration.class);
    when(nonStopConfig.getReadOpNonStopTimeoutBehavior()).thenReturn(NonStopConfigurationFields.NonStopReadTimeoutBehavior.EXCEPTION);
    when(nonStopConfig.getWriteOpNonStopTimeoutBehavior()).thenReturn(NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION);
    when(nonStopConfig.isEnabled()).thenReturn(true);
    return nonStopConfig;
  }

  private NonStopConfigurationRegistry nonStopConfigurationRegistry(NonStopConfiguration nonStopConfiguration) {
    NonStopConfigurationRegistry nonStopConfigurationRegistry = mock(NonStopConfigurationRegistry.class);
    when(nonStopConfigurationRegistry.getConfigForInstanceMethod(anyString(), anyString(), any(ToolkitObjectType.class))).thenReturn(nonStopConfiguration);
    return nonStopConfigurationRegistry;
  }

  private NonStopContext nonStopContext(NonStopConfigurationRegistry nonStopConfigurationRegistry) {
    NonStopContext nonStopContext = mock(NonStopContext.class);
    NonStopManager nonStopManager = mock(NonStopManager.class);
    NonStopClusterListener clusterListener = mock(NonStopClusterListener.class);
    when(nonStopContext.getNonStopConfigurationRegistry()).thenReturn(nonStopConfigurationRegistry);
    when(nonStopContext.getNonStopManager()).thenReturn(nonStopManager);
    when(nonStopContext.getNonStopClusterListener()).thenReturn(clusterListener);
    when(nonStopContext.getNonstopTimeoutBehaviorResolver()).thenReturn(new NonstopTimeoutBehaviorResolver());
    when(nonStopContext.isEnabledForCurrentThread()).thenReturn(true);
    return nonStopContext;
  }

  protected <T extends ToolkitObject> ToolkitObjectLookup<T> lookup(T object) {
    ToolkitObjectLookup<T> toolkitObjectLookup = mock(ToolkitObjectLookup.class);
    when(toolkitObjectLookup.getInitializedObject()).thenReturn(object);
    when(toolkitObjectLookup.getInitializedObjectOrNull()).thenReturn(object);
    return toolkitObjectLookup;
  }
}
