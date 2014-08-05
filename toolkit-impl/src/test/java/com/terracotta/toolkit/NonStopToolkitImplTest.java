package com.terracotta.toolkit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.abortable.AbortableOperationManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class NonStopToolkitImplTest {

  @Test
  public void testShutdownWhenToolkitFailsToInitialize() throws Exception {
    @SuppressWarnings("unchecked")
    FutureTask<ToolkitInternal> toolkitDelegateFutureTask = mock(FutureTask.class);
    when(toolkitDelegateFutureTask.get()).thenThrow(new ExecutionException("oops", new RuntimeException()));

    NonStopToolkitImpl nonStopToolkit = new NonStopToolkitImpl(toolkitDelegateFutureTask,
                                                               mock(AbortableOperationManager.class), "uuid");
    nonStopToolkit.shutdown();
  }

  @Test
  public void testShutdownProperlyShutsDownToolkit() throws Exception {
    @SuppressWarnings("unchecked")
    FutureTask<ToolkitInternal> toolkitDelegateFutureTask = mock(FutureTask.class);
    ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
    when(toolkitDelegateFutureTask.get()).thenReturn(toolkitInternal);

    NonStopToolkitImpl nonStopToolkit = new NonStopToolkitImpl(toolkitDelegateFutureTask,
                                                               mock(AbortableOperationManager.class), "uuid");
    nonStopToolkit.shutdown();

    verify(toolkitInternal).shutdown();
  }

}