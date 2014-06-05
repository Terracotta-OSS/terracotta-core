package com.terracotta.toolkit;

import org.junit.Test;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.platform.PlatformService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NonStopToolkitImplTest {

  @Test
  public void testShutdownWhenToolkitFailsToInitialize() throws Exception {
    @SuppressWarnings("unchecked")
    FutureTask<ToolkitInternal> toolkitDelegateFutureTask = mock(FutureTask.class);
    when(toolkitDelegateFutureTask.get()).thenThrow(new ExecutionException("oops", new RuntimeException()));

    NonStopToolkitImpl nonStopToolkit = new NonStopToolkitImpl(toolkitDelegateFutureTask, mock(PlatformService.class));
    nonStopToolkit.shutdown();
  }

  @Test
  public void testShutdownProperlyShutsDownToolkit() throws Exception {
    @SuppressWarnings("unchecked")
    FutureTask<ToolkitInternal> toolkitDelegateFutureTask = mock(FutureTask.class);
    ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
    when(toolkitDelegateFutureTask.get()).thenReturn(toolkitInternal);

    NonStopToolkitImpl nonStopToolkit = new NonStopToolkitImpl(toolkitDelegateFutureTask, mock(PlatformService.class));
    nonStopToolkit.shutdown();

    verify(toolkitInternal).shutdown();
  }

}