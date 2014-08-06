package com.terracotta.management.service.impl.util;

import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.SubGenericType;

import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.service.impl.TimeoutServiceImpl;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class RemoteManagementSourceTest {

  @Test
  public void testGetFromRemoteL2_works() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    remoteManagementSource.getFromRemoteL2("server1", new URI("/xyz"), Collection.class, String.class);

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).get(eq(new SubGenericType<Collection, String>(Collection.class, String.class)));
  }

  @Test
  public void testGetFromRemoteL2_throwsException() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    when(builder.get(any(GenericType.class))).thenThrow(WebApplicationException.class);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    try {
      remoteManagementSource.getFromRemoteL2("server1", new URI("/xyz"), Collection.class, String.class);
      fail("expected ManagementSourceException");
    } catch (ManagementSourceException mse) {
      assertNotNull(mse.getErrorEntity());
    }

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).get(eq(new SubGenericType<Collection, String>(Collection.class, String.class)));
  }

  @Test
  public void testPost1ToRemoteL2_works() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"));

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).post(Matchers.<Entity<Object>>eq(null));
  }

  @Test
  public void testPost1ToRemoteL2_fails() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    when(builder.post(any(Entity.class))).thenThrow(WebApplicationException.class);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    try {
      remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"));
      fail("expected ManagementSourceException");
    } catch (ManagementSourceException mse) {
      assertNotNull(mse.getErrorEntity());
    }

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).post(Matchers.<Entity<Object>>eq(null));
  }

  @Test
  public void testPost2ToRemoteL2_works() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"), (Collection)Collections.singleton("aaa"), String.class);

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).post(argument.capture(), eq(String.class));
    assertEquals(Collections.singleton("aaa"), argument.getValue().getEntity());
  }

  @Test
  public void testPost2ToRemoteL2_fails() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    when(builder.post(any(Entity.class), any(Class.class))).thenThrow(WebApplicationException.class);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    try {
      remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"), (Collection)Collections.singleton("aaa"), String.class);
      fail("expected ManagementSourceException");
    } catch (ManagementSourceException mse) {
      assertNotNull(mse.getErrorEntity());
    }

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).post(argument.capture(), eq(String.class));
    assertEquals(Collections.singleton("aaa"), argument.getValue().getEntity());
  }

  @Test
  public void testPost3ToRemoteL2_works() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"), Collection.class, String.class);

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).post((Entity<?>)eq(null), eq(new SubGenericType<Collection, String>(Collection.class, String.class)));
  }

  @Test
  public void testPost3ToRemoteL2_fails() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);

    when(builder.post(any(Entity.class), any(SubGenericType.class))).thenThrow(WebApplicationException.class);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);
    try {
      remoteManagementSource.postToRemoteL2("server1", new URI("/xyz"), Collection.class, String.class);
      fail("expected ManagementSourceException");
    } catch (ManagementSourceException mse) {
      assertNotNull(mse.getErrorEntity());
    }

    verify(client).target(eq(new URI("http://server-host1:9540/xyz")));
    verify(builder).post((Entity<?>)eq(null), eq(new SubGenericType<Collection, String>(Collection.class, String.class)));
  }

  @Test
  public void testAddTsaEventListener_callsOnEvent() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);
    AsyncInvoker asyncInvoker = mock(AsyncInvoker.class);
    when(builder.async()).thenReturn(asyncInvoker);
    when(asyncInvoker.get(any(InvocationCallback.class))).thenReturn(mock(Future.class));


    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client) {
      @Override
      protected long eventReadFailureRetryDelayInMs() {
        return 1L;
      }
    };

    RemoteManagementSource.RemoteTSAEventListener listener = mock(RemoteManagementSource.RemoteTSAEventListener.class);
    remoteManagementSource.addTsaEventListener(listener);

    ArgumentCaptor<InvocationCallback> argument = ArgumentCaptor.forClass(InvocationCallback.class);
    verify(asyncInvoker).get(argument.capture());
    InvocationCallback callback = argument.getValue();

    EventInput eventInput = mock(EventInput.class);
    final AtomicInteger counter = new AtomicInteger();
    when(eventInput.read()).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        if (counter.getAndIncrement() < 5) {
          return mock(InboundEvent.class);
        }
        return null;
      }
    });

    callback.completed(eventInput);

    verify(asyncInvoker, times(2)).get(any(InvocationCallback.class));
    verify(listener, times(0)).onError(any(Throwable.class));
    verify(listener, times(5)).onEvent(any(InboundEvent.class));
  }

  @Test
  public void testAddTsaEventListener_retryOnException() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);
    AsyncInvoker asyncInvoker = mock(AsyncInvoker.class);
    when(builder.async()).thenReturn(asyncInvoker);
    when(asyncInvoker.get(any(InvocationCallback.class))).thenReturn(mock(Future.class));


    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client) {
      @Override
      protected long eventReadFailureRetryDelayInMs() {
        return 1L;
      }
    };

    RemoteManagementSource.RemoteTSAEventListener listener = mock(RemoteManagementSource.RemoteTSAEventListener.class);
    remoteManagementSource.addTsaEventListener(listener);

    ArgumentCaptor<InvocationCallback> argument = ArgumentCaptor.forClass(InvocationCallback.class);
    verify(asyncInvoker).get(argument.capture());
    InvocationCallback callback = argument.getValue();

    callback.failed(new Exception());

    verify(asyncInvoker, times(2)).get(any(InvocationCallback.class));
    verify(listener, times(0)).onError(any(Throwable.class));
    verify(listener, times(0)).onEvent(any(InboundEvent.class));
  }

  @Test
  public void testAddTsaEventListener_abortOnWebAppException401() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);
    AsyncInvoker asyncInvoker = mock(AsyncInvoker.class);
    when(builder.async()).thenReturn(asyncInvoker);
    when(asyncInvoker.get(any(InvocationCallback.class))).thenReturn(mock(Future.class));


    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client) {
      @Override
      protected long eventReadFailureRetryDelayInMs() {
        return 1L;
      }
    };

    RemoteManagementSource.RemoteTSAEventListener listener = mock(RemoteManagementSource.RemoteTSAEventListener.class);
    remoteManagementSource.addTsaEventListener(listener);

    ArgumentCaptor<InvocationCallback> argument = ArgumentCaptor.forClass(InvocationCallback.class);
    verify(asyncInvoker).get(argument.capture());
    InvocationCallback callback = argument.getValue();

    callback.failed(new WebApplicationException(401));

    verify(listener, times(1)).onError(any(WebApplicationException.class));
    verify(listener, times(0)).onEvent(any(InboundEvent.class));
  }

  @Test
  public void testAddTsaEventListener_abortOnInterruptedException() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);

    when(localManagementSource.getRemoteServerUrls()).thenReturn(new HashMap<String, String>() {{
      put("server1", "http://server-host1:9540");
    }});

    WebTarget webTarget = mock(WebTarget.class);
    when(webTarget.register(any(Class.class))).thenReturn(webTarget);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), any())).thenReturn(builder);
    when(client.target(any(URI.class))).thenReturn(webTarget);
    AsyncInvoker asyncInvoker = mock(AsyncInvoker.class);
    when(builder.async()).thenReturn(asyncInvoker);
    when(asyncInvoker.get(any(InvocationCallback.class))).thenReturn(mock(Future.class));


    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client) {
      @Override
      protected long eventReadFailureRetryDelayInMs() {
        return 1L;
      }
    };

    RemoteManagementSource.RemoteTSAEventListener listener = mock(RemoteManagementSource.RemoteTSAEventListener.class);
    remoteManagementSource.addTsaEventListener(listener);

    ArgumentCaptor<InvocationCallback> argument = ArgumentCaptor.forClass(InvocationCallback.class);
    verify(asyncInvoker).get(argument.capture());
    InvocationCallback callback = argument.getValue();

    callback.failed(new InterruptedException());

    verify(listener, times(1)).onError(any(InterruptedException.class));
    verify(listener, times(0)).onEvent(any(InboundEvent.class));
  }


  @Test
  public void testCollectEntitiesFromFutures_futureReturningNullIsNotAddedToResultingCollection() throws Exception {
    LocalManagementSource localManagementSource = mock(LocalManagementSource.class);
    Client client = mock(Client.class);
    Future<MyRepresentable> future = mock(Future.class);

    RemoteManagementSource remoteManagementSource = new RemoteManagementSource(localManagementSource, new TimeoutServiceImpl(1000L), new DfltSecurityContextService(), client);

    Collection<MyRepresentable> result = remoteManagementSource.collectEntitiesFromFutures(Collections.singletonMap("server1", future), 1000, "myMethod", 1000);
    assertThat(result.isEmpty(), is(true));
  }

  static class MyRepresentable implements Representable {
    private String agentId;
    @Override
    public String getAgentId() {
      return agentId;
    }
    @Override
    public void setAgentId(String agentId) {
      this.agentId = agentId;
    }
  }

}
