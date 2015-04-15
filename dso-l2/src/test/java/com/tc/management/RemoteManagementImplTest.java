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
package com.tc.management;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.ServiceID;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.handler.ServerManagementHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteManagementImplTest {

  @Test
  public void testGetAllClientIDs() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    when(channelManager.getAllClientIDs()).thenReturn(new HashSet() {{
      add(new ClientID(0L));
      add(new ClientID(1L));
      add(new ClientID(2L));
    }});

    Set<NodeID> allClientIDs = remoteManagement.getAllClientIDs();
    assertThat(allClientIDs.size(), is(3));
  }

  @Test
  public void testListRegisteredServicesNormalCase() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    final ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    ListRegisteredServicesMessage listRegisteredServicesMessage = mock(ListRegisteredServicesMessage.class);
    when(messageChannel.createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE)).thenReturn(listRegisteredServicesMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ListRegisteredServicesResponseMessage responseMessage = mock(ListRegisteredServicesResponseMessage.class);
        when(responseMessage.getRemoteCallDescriptors()).thenReturn(new HashSet<RemoteCallDescriptor>() {{
          add(remoteCallDescriptor);
        }});

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Set<RemoteCallDescriptor> remoteCallDescriptors = remoteManagement.listRegisteredServices(clientID, 1, TimeUnit.MILLISECONDS);
    assertThat(remoteCallDescriptors.size(), is(1));
    assertThat(remoteCallDescriptors.iterator().next(), is(remoteCallDescriptor));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test(expected = RemoteManagementException.class)
  public void testListRegisteredServicesNoResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    final ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    ListRegisteredServicesMessage listRegisteredServicesMessage = mock(ListRegisteredServicesMessage.class);
    when(messageChannel.createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE)).thenReturn(listRegisteredServicesMessage);

    remoteManagement.listRegisteredServices(clientID, 1, TimeUnit.MILLISECONDS);
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testListRegisteredServicesInterrupted() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    final ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    ListRegisteredServicesMessage listRegisteredServicesMessage = mock(ListRegisteredServicesMessage.class);
    when(messageChannel.createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE)).thenReturn(listRegisteredServicesMessage);

    Thread.currentThread().interrupt();
    try {
      remoteManagement.listRegisteredServices(clientID, 1, TimeUnit.MILLISECONDS);
      fail("expected RemoteManagementException");
    } catch (RemoteManagementException rme) {
      assertThat(rme.getCause(), instanceOf(InterruptedException.class));
    }
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getReturnsResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
        ResponseHolder responseHolder = new ResponseHolder(response);
        when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
        when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    assertThat(f.get(), CoreMatchers.<Object>equalTo(response));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getThrowsException() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final Exception response = new IllegalArgumentException("this is an error");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
        ResponseHolder responseHolder = new ResponseHolder(response);
        when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
        when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      f.get();
      fail("expected ExecutionException");
    } catch (ExecutionException ee) {
      assertThat(ee.getCause().getCause(), instanceOf(IllegalArgumentException.class));
    }
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getInterrupted() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final Exception response = new IllegalArgumentException("this is an error");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
        ResponseHolder responseHolder = new ResponseHolder(response);
        when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
        when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      Thread.currentThread().interrupt();
      f.get();
      fail("expected InterruptedException");
    } catch (InterruptedException ie) {
      // expected
    }
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutReturnsResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
        ResponseHolder responseHolder = new ResponseHolder(response);
        when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
        when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    assertThat(f.get(1, TimeUnit.MILLISECONDS), CoreMatchers.<Object>equalTo(response));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutTimesOut() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(invokeRegisteredServiceMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(999));
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());

    // test that get() times out
    try {
      f.get(1, TimeUnit.MILLISECONDS);
      fail("expected TimeoutException");
    } catch (TimeoutException te) {
      // expected
    }

    assertThat(f.isDone(), is(false));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(0)).unregisterResponseListener(any(ManagementRequestID.class));

    // now that no response arrived yet, cancel
    assertThat(f.cancel(true), is(true));
    try {
      f.get(1, TimeUnit.MILLISECONDS);
      fail("expected CancellationException");
    } catch (CancellationException ce) {
      // expected
    }

    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(true));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutInterruptedBeforeResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    final AtomicReference<ManagementResponseListener> managementResponseListenerAtomicReference = new AtomicReference<ManagementResponseListener>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListenerAtomicReference.set(managementResponseListener);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      Thread.currentThread().interrupt();
      f.get();
      fail("expected InterruptedException");
    } catch (InterruptedException ie) {
      // expected
    }
    assertThat(f.isDone(), is(false));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(0)).unregisterResponseListener(any(ManagementRequestID.class));

    // send the response now
    InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
    ResponseHolder responseHolder = new ResponseHolder(response);
    when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
    when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));
    managementResponseListenerAtomicReference.get().onResponse(responseMessage);

    // check that we get the response, and that cancelling after that has no effect
    assertThat(f.get(), CoreMatchers.<Object>equalTo(response));
    assertThat(f.cancel(true), is(false));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutInterruptedBeforeResponseThenCancelAfterResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    final AtomicReference<ManagementResponseListener> managementResponseListenerAtomicReference = new AtomicReference<ManagementResponseListener>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListenerAtomicReference.set(managementResponseListener);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      Thread.currentThread().interrupt();
      f.get();
      fail("expected InterruptedException");
    } catch (InterruptedException ie) {
      // expected
    }
    assertThat(f.isDone(), is(false));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(0)).unregisterResponseListener(any(ManagementRequestID.class));

    // send the response now
    InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
    ResponseHolder responseHolder = new ResponseHolder(response);
    when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
    when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));
    managementResponseListenerAtomicReference.get().onResponse(responseMessage);

    // try to cancel, cannot be done as the response arrived
    assertThat(f.cancel(true), is(false));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));

    assertThat(f.get(), CoreMatchers.<Object>equalTo(response));

    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutInterruptedBeforeResponseThenCancelBeforeResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    final AtomicReference<ManagementResponseListener> managementResponseListenerAtomicReference = new AtomicReference<ManagementResponseListener>();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListenerAtomicReference.set(managementResponseListener);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      Thread.currentThread().interrupt();
      f.get();
      fail("expected InterruptedException");
    } catch (InterruptedException ie) {
      // expected
    }
    assertThat(f.isDone(), is(false));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(0)).unregisterResponseListener(any(ManagementRequestID.class));

    // try to cancel, cannot be done as the response arrived
    assertThat(f.cancel(true), is(true));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(true));

    // send the response now
    InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
    ResponseHolder responseHolder = new ResponseHolder(response);
    when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
    when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));
    managementResponseListenerAtomicReference.get().onResponse(responseMessage);

    // make sure get fails as the request was cancelled before the response arrived
    try {
      assertThat(f.get(), CoreMatchers.<Object>equalTo(response));
      fail("expected CancellationException");
    } catch (CancellationException ce) {
      // expected
    }

    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(2)).unregisterResponseListener(any(ManagementRequestID.class)); // times(2) because cancel + response both call unregisterResponseListener
  }

  @Test
  public void testAsyncRemoteCall_getWithTimeoutInterruptedAfterResponse() throws Exception {
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerManagementHandler serverManagementHandler = mock(ServerManagementHandler.class);
    RemoteManagementImpl remoteManagement = new RemoteManagementImpl(channelManager, serverManagementHandler, null);

    ClientID clientID = new ClientID(0L);
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);

    InvokeRegisteredServiceMessage invokeRegisteredServiceMessage = mock(InvokeRegisteredServiceMessage.class);
    when(messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE)).thenReturn(invokeRegisteredServiceMessage);

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(clientID, new ServiceID("myClass", 0), "myMethod", new String[0]);
    final String response = "this is a response";
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        InvokeRegisteredServiceResponseMessage responseMessage = mock(InvokeRegisteredServiceResponseMessage.class);
        ResponseHolder responseHolder = new ResponseHolder(response);
        when(responseMessage.getResponseHolder()).thenReturn(responseHolder);
        when(responseMessage.getManagementRequestID()).thenReturn(new ManagementRequestID(0L));

        ManagementResponseListener managementResponseListener = (ManagementResponseListener)invocation.getArguments()[1];
        managementResponseListener.onResponse(responseMessage);
        return null;
      }
    }).when(serverManagementHandler).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));

    Future<Object> f = remoteManagement.asyncRemoteCall(remoteCallDescriptor, getClass().getClassLoader());
    try {
      Thread.currentThread().interrupt();
      f.get(1, TimeUnit.MILLISECONDS);
      fail("expected InterruptedException");
    } catch (InterruptedException ie) {
      // expected
    }
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));

    assertThat(f.get(1, TimeUnit.MILLISECONDS), CoreMatchers.<Object>equalTo(response));
    assertThat(f.isDone(), is(true));
    assertThat(f.isCancelled(), is(false));
    verify(serverManagementHandler, times(1)).registerResponseListener(any(ManagementRequestID.class), any(ManagementResponseListener.class));
    verify(serverManagementHandler, times(1)).unregisterResponseListener(any(ManagementRequestID.class));
  }

}
