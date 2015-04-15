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