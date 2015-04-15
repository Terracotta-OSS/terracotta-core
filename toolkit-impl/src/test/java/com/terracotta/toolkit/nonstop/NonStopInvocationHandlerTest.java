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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.terracotta.test.categories.CheckShorts;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.util.Assert;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.collections.map.ToolkitCacheImplInterface;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Category(CheckShorts.class)
public class NonStopInvocationHandlerTest {

  private NonStopInvocationHandler<ToolkitCacheImpl> nonStopInvocationHandler;
  private Method                                     method;
  private boolean                                    nonstopExeptionOccurred;
  private Object                                     methodReturnValue;
  @Mock private NonStopContext                          context;
  @Mock private NonStopConfigurationLookup              nonStopConfigurationLookup;
  @Mock private NonStopConfiguration nonStopConfiguration;
  @Mock private ToolkitObjectLookup<ToolkitCacheImpl>      toolkitObjectLookup;
  @Mock private ToolkitCacheImpl<String, String>           cache;
  @Mock private NonStopManager nonStopManager;
  @Mock private NonStopClusterListener clusterListener;

  @Before
  public void setUp() throws Throwable {
    MockitoAnnotations.initMocks(this);
    method = ToolkitCacheImplInterface.class.getMethod("get", Object.class);
    nonstopExeptionOccurred = false;
    nonStopInvocationHandler = getTestableNonStopInvocationHandler();
    when(toolkitObjectLookup.getInitializedObjectOrNull()).thenReturn(cache);
    when(toolkitObjectLookup.getInitializedObject()).thenReturn(cache);
    when(nonStopConfigurationLookup.getNonStopConfigurationForMethod(anyString())).thenReturn(nonStopConfiguration);
    when(nonStopConfigurationLookup.getObjectType()).thenReturn(ToolkitObjectType.CACHE);
    when(context.getNonStopManager()).thenReturn(nonStopManager);
    when(nonStopManager.tryBegin(Mockito.anyLong())).thenReturn(true);
    when(nonStopConfiguration.isEnabled()).thenReturn(true);
    when(clusterListener.areOperationsEnabled()).thenReturn(Boolean.TRUE);
    when(context.getNonStopClusterListener()).thenReturn(clusterListener);
  }


  @Test
  public void testInvokeWhenNonStopDisabled() throws Throwable {
    whenNonStopDisabled().invokeMethod().andAssertNonStopTimerNotStarted();
  }


  @Test
  public void testInvokeForIteratorMethodWhenNonStopDisabled() throws Throwable {
    whenNonStopDisabled().invokeForIteratorMethod().andAssertNonStopTimerNotStarted().andAssertNonStopProxyisReturned();
  }


  @Test
  public void testInvokeForLocalMethods() throws Throwable {
    invokeLocalMethod().andAssertNonStopTimerNotStarted();
  }
  
  
  @Test
  public void testInvokeWhenOpearationsDisabledAndImmediateTimeoutEnabled() throws Throwable {
    whenOperationsDisabled().andImmediateTimeoutIs(true).invokeMethod().andAssertNonStopTimerNotStarted()
        .andAssertNonStopExceptionOccurred();
  }
  
  
  @Test
  public void testInvokeWhenOperationsDisabledAndImmediateTimeoutDisabled() throws Throwable {
    whenOperationsDisabled().andImmediateTimeoutIs(false).invokeMethod().andAssertNonStopTimerStarted()
        .andAssertNonStopExceptionOccurred();
  }

  
  @Test
  public void testInvokeWhenInitializationFailed() throws Throwable {
    whenToolkitObjectInitializationFailed().invokeMethod().andAssertNonStopTimerStarted()
        .andAssertNonStopExceptionOccurred();
  }

  
  @Test
  public void testInvokeWhenRejoinOccurs() throws Throwable {
    whenRejoinOccurs().invokeMethod().andAssertNonStopTimerStarted().andAssertNonStopExceptionOccurred();
  }

  private NonStopInvocationHandlerTest andImmediateTimeoutIs(boolean b) {
    when(nonStopConfiguration.isImmediateTimeoutEnabled()).thenReturn(b);
    return this;
  }

  
  private NonStopInvocationHandlerTest andAssertNonStopTimerStarted() {
    Mockito.verify(nonStopManager, Mockito.times(1)).tryBegin(Mockito.anyLong());
    Mockito.verify(nonStopManager, Mockito.times(1)).finish();
    return this;
  }

  private NonStopInvocationHandlerTest andAssertNonStopExceptionOccurred() {
    Assert.assertTrue(nonstopExeptionOccurred);
    return this;
  }

  private NonStopInvocationHandlerTest andAssertNonStopTimerNotStarted() {
    Mockito.verify(nonStopManager, Mockito.times(0)).tryBegin(Mockito.anyLong());
    return this;
  }

  private NonStopInvocationHandlerTest whenOperationsDisabled() {
    when(clusterListener.areOperationsEnabled()).thenReturn(Boolean.FALSE);
    Mockito.doThrow(new ToolkitAbortableOperationException()).when(clusterListener).waitUntilOperationsEnabled();
    return this;
  }

  private NonStopInvocationHandlerTest invokeMethod() throws Throwable {
    try {
      methodReturnValue = nonStopInvocationHandler.invoke(null, method, new Object[] { "key" });
    } catch (NonStopException e) {
      nonstopExeptionOccurred = true;
    }
    return this;
  }

  private NonStopInvocationHandlerTest invokeLocalMethod() throws Throwable {
    method = ToolkitCacheImpl.class.getMethod("localSize");
    methodReturnValue = nonStopInvocationHandler.invoke(null, method, new Object[] {});
    return this;
  }

  private NonStopInvocationHandlerTest invokeForIteratorMethod() throws Throwable {
    method = ToolkitCacheImpl.class.getMethod("iterator");
    methodReturnValue = nonStopInvocationHandler.invoke(null, method, new Object[] {});
    return this;
  }

  private NonStopInvocationHandlerTest whenNonStopDisabled() {
    when(nonStopConfiguration.isEnabled()).thenReturn(false);
    return this;
  }

  private NonStopInvocationHandlerTest andAssertNonStopProxyisReturned() {
    Assert.assertTrue(methodReturnValue instanceof Proxy);
    return this;
  }

  private NonStopInvocationHandlerTest whenToolkitObjectInitializationFailed() {
    when(toolkitObjectLookup.getInitializedObject()).thenThrow(new NonStopToolkitInstantiationException());
    return this;
  }

  private NonStopInvocationHandlerTest whenRejoinOccurs() {
    when(toolkitObjectLookup.getInitializedObject()).thenThrow(new RejoinException());
    return this;
  }

  private NonStopInvocationHandler<ToolkitCacheImpl> getTestableNonStopInvocationHandler() {
    return new NonStopInvocationHandler<ToolkitCacheImpl>(context, nonStopConfigurationLookup, toolkitObjectLookup) {

      @Override
      Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
        return new NonstopTimeoutBehaviorResolver().resolveExceptionOnTimeoutBehavior(ToolkitObjectType.CACHE);
      }
    };
  }

}
