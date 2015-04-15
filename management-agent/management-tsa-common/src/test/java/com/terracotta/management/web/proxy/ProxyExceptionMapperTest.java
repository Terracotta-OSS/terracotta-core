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
package com.terracotta.management.web.proxy;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.management.ServiceLocator;

import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class ProxyExceptionMapperTest {

  @Before
  public void setUp() throws Exception {
    ServiceLocator.unload();
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.remove();
  }

  @After
  public void tearDown() throws Exception {
    ServiceLocator.unload();
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.remove();
  }

  @Test
  public void testGet_noCompressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("GET");
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.get()).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(false));
    verify(builder).get();

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testGet_compressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("GET");
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>() {{ put("Accept-Encoding", Arrays.asList("deflate")); }});
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.get()).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(true));
    verify(builder).get();

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testPost_noCompressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequest requestContext = mock(ContainerRequest.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("POST");
    ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
    when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(requestContext.readEntity(any(Class.class))).thenReturn("the request data".getBytes());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.post(any(Entity.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(false));

    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).post(argument.capture());
    assertTrue(Arrays.deepEquals(new Object[] { "the request data".getBytes() }, new Object[] { argument.getValue()
        .getEntity() }));

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testPost_compressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequest requestContext = mock(ContainerRequest.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("POST");
    ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>() {{ put("Accept-Encoding", Arrays.asList("deflate")); }});
    when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(requestContext.readEntity(any(Class.class))).thenReturn("the request data".getBytes());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.post(any(Entity.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(true));
    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).post(argument.capture());
    assertTrue(Arrays.deepEquals(new Object[] { "the request data".getBytes() }, new Object[] { argument.getValue()
        .getEntity() }));

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testPut_noCompressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequest requestContext = mock(ContainerRequest.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("PUT");
    ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
    when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(requestContext.readEntity(any(Class.class))).thenReturn("the request data".getBytes());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.put(any(Entity.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(false));
    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).put(argument.capture());
    assertTrue(Arrays.deepEquals(new Object[] { "the request data".getBytes() }, new Object[] { argument.getValue().getEntity() }));

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testPut_compressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequest requestContext = mock(ContainerRequest.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("PUT");
    ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>() {{ put("Accept-Encoding", Arrays.asList("deflate")); }});
    when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
    when(requestContext.readEntity(any(Class.class))).thenReturn("the request data".getBytes());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.put(any(Entity.class))).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(true));
    ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
    verify(builder).put(argument.capture());
    assertTrue(Arrays.deepEquals(new Object[] { "the request data".getBytes() }, new Object[] { argument.getValue()
        .getEntity() }));

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testDelete_noCompressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("DELETE");
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.delete()).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(false));
    verify(builder).delete();

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testDelete_compressionWorks() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    RemoteManagementSource remoteManagementSource = mock(RemoteManagementSource.class);
    locator.loadService(RemoteManagementSource.class, remoteManagementSource);
    ServiceLocator.load(locator);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("DELETE");
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>() {{ put("Accept-Encoding", Arrays.asList("deflate")); }});
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(remoteManagementSource.resource(any(URI.class), anyBoolean())).thenReturn(builder);
    Response response = mock(Response.class);
    when(builder.delete()).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.getHeaders()).thenReturn(new MultivaluedHashMap<String, Object>());
    when(response.readEntity(any(Class.class))).thenReturn("the response data".getBytes());


    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    verify(remoteManagementSource).resource(eq(new URI("http://active-server:9540/tc-mgmt-api/v2/agents")), eq(true));
    verify(builder).delete();

    assertTrue(Arrays.deepEquals(new Object[] { proxyResponse.getEntity() }, new Object[] { "the response data".getBytes() }));
    assertEquals(200, proxyResponse.getStatus());
  }

  @Test
  public void testUnsupportedOperation() throws Exception {
    ServiceLocator locator = new ServiceLocator();
    ServiceLocator.load(locator);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestContextFilter.CONTAINER_REQUEST_CONTEXT_THREAD_LOCAL.set(requestContext);
    when(requestContext.getMethod()).thenReturn("HEAD");
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI("http://passive-server:9640/tc-mgmt-api/v2/agents"));
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<String, String>());

    ProxyExceptionMapper proxyExceptionMapper = new ProxyExceptionMapper();
    Response proxyResponse = proxyExceptionMapper.toResponse(new ProxyException("http://active-server:9540"));

    assertEquals(500, proxyResponse.getStatus());
  }

}
