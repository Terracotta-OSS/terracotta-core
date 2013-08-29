/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

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
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.util.Assert;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.lang.reflect.Method;
import java.util.Iterator;

@Category(CheckShorts.class)
public class NonStopSubTypeInvocationHandlerTest {

  private NonStopSubTypeInvocationHandler<Iterator> nonStopSubTypeInvocationHandler;
  private Method                                    method;
  private boolean                                   nonstopExeptionOccurred;
  private Object                                    methodReturnValue;
  @Mock  private NonStopContext                            context;
  @Mock private NonStopConfigurationLookup                nonStopConfigurationLookup;
  @Mock private NonStopConfiguration                      nonStopConfiguration;
  @Mock private NonStopManager nonStopManager;
  @Mock private Iterator<String> delegate;
  @Mock
  private NonStopClusterListener                    clusterListener;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    method = Iterator.class.getMethod("next");
    nonStopSubTypeInvocationHandler = getTestableNonStopSupTypeInvocationHandler();

    when(nonStopConfigurationLookup.getNonStopConfiguration()).thenReturn(nonStopConfiguration);
    when(context.getNonStopManager()).thenReturn(nonStopManager);
    when(nonStopManager.tryBegin(Mockito.anyLong())).thenReturn(true);
    when(nonStopConfiguration.isEnabled()).thenReturn(true);
    when(clusterListener.areOperationsEnabled()).thenReturn(Boolean.TRUE);
    when(context.getNonStopClusterListener()).thenReturn(clusterListener);
    when(nonStopConfiguration.getWriteOpNonStopTimeoutBehavior()).thenReturn(NonStopWriteTimeoutBehavior.EXCEPTION);
  }


  private NonStopSubTypeInvocationHandler<Iterator> getTestableNonStopSupTypeInvocationHandler() {
    return new NonStopSubTypeInvocationHandler<Iterator>(context,
        nonStopConfigurationLookup, delegate, Iterator.class) {

      @Override
      Object resolveTimeoutBehavior(NonStopConfiguration nonStopConfiguration) {
        return new NonstopTimeoutBehaviorResolver().resolveTimeoutBehaviorForSubType(ToolkitObjectType.SET,
                                                                                     nonStopConfiguration, Iterator.class);
      }
    };
  }


  @Test
  public void testInvokeWhenNonStopDisabled() throws Throwable {
    whenNonStopDisabled().invokeMethod().andAssertNonStopTimerNotStarted();
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
  public void testInvokeWhenRejoinOccurs() throws Throwable {
    whenRejoinOccurs().invokeMethod().andAssertNonStopTimerStarted().andAssertNonStopExceptionOccurred();
  }

  private NonStopSubTypeInvocationHandlerTest whenRejoinOccurs() {
    Mockito.doThrow(new RejoinException()).when(clusterListener).waitUntilOperationsEnabled();
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest andImmediateTimeoutIs(boolean b) {
    when(nonStopConfiguration.isImmediateTimeoutEnabled()).thenReturn(b);
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest andAssertNonStopTimerStarted() {
    Mockito.verify(nonStopManager, Mockito.times(1)).tryBegin(Mockito.anyLong());
    Mockito.verify(nonStopManager, Mockito.times(1)).finish();
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest andAssertNonStopExceptionOccurred() {
    Assert.assertTrue(nonstopExeptionOccurred);
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest whenOperationsDisabled() {
    when(clusterListener.areOperationsEnabled()).thenReturn(Boolean.FALSE);
    Mockito.doThrow(new ToolkitAbortableOperationException()).when(clusterListener).waitUntilOperationsEnabled();
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest andAssertNonStopTimerNotStarted() {
    Mockito.verify(nonStopManager, Mockito.times(0)).tryBegin(Mockito.anyLong());
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest whenNonStopDisabled() {
    when(nonStopConfiguration.isEnabled()).thenReturn(false);
    return this;
  }

  private NonStopSubTypeInvocationHandlerTest invokeMethod() throws Throwable {
    try {
      methodReturnValue = nonStopSubTypeInvocationHandler.invoke(null, method, new Object[] {});
    } catch (NonStopException e) {
      nonstopExeptionOccurred = true;
    }
    return this;
  }
}
