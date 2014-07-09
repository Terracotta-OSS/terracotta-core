package com.tc.admin;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TCStopTest {
  @Test
  public void testUnknownError() throws Exception {
    WebTarget target = mockWebTarget("localhost", 12323);
    responseCode(target, 403);
    String errorMessage = "critical failure";
    when(target.request(MediaType.APPLICATION_JSON_TYPE)
        .post(any(Entity.class))
        .readEntity(any(Class.class))).thenReturn(Collections
        .singletonMap("error", errorMessage));
    try {
      TCStop.restStop(target, false);
      fail();
    } catch (IOException e) {
      assertThat(e.getMessage(), containsString(errorMessage));
    }
  }

  @Test(expected = IOException.class)
  public void testAuthenticationFailure() throws Exception {
    WebTarget target = mockWebTarget("localhost", 12323);
    responseCode(target, 401);
    TCStop.restStop(target, false);
  }

  @Test(expected = IOException.class)
  public void testFourOhFour() throws Exception {
    WebTarget target = mockWebTarget("localhost", 12323);
    responseCode(target, 404);
    TCStop.restStop(target, false);
  }

  @Test
  public void testForceStop() throws Exception {
    WebTarget target = mockWebTarget("localhost", 12323);
    responseCode(target, 200);
    TCStop.restStop(target, true);
    verify(target.request(MediaType.APPLICATION_JSON_TYPE)).post(
        argThat(entityWithContent(Collections.singletonMap("forceStop", true), MediaType.APPLICATION_JSON_TYPE)));
  }

  @Test
  public void testNoForceStop() throws Exception {
    WebTarget target = mockWebTarget("localhost", 12323);
    responseCode(target, 200);
    TCStop.restStop(target, false);
    verify(target.request(MediaType.APPLICATION_JSON_TYPE)).post(
        argThat(entityWithContent(Collections.singletonMap("forceStop", false), MediaType.APPLICATION_JSON_TYPE)));
  }

  private void responseCode(WebTarget target, int responseCode) {
    when(target.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(null)).getStatus()).thenReturn(responseCode);
  }

  private WebTarget mockWebTarget(String host, int port) throws URISyntaxException {
    Response response = mock(Response.class);
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(builder.post(any(Entity.class))).thenReturn(response);
    WebTarget target = mock(WebTarget.class);
    when(target.getUri()).thenReturn(new URI("http://" + host + ":" + port));
    when(target.path(anyString())).thenReturn(target);
    when(target.request((MediaType[]) anyVararg())).thenReturn(builder);
    return target;
  }

  private static <T> ArgumentMatcher<Entity<T>> entityWithContent(final T entity, final MediaType mediaType) {
    return new ArgumentMatcher<Entity<T>>() {
      @Override
      public boolean matches(final Object argument) {
        if (argument instanceof Entity) {
          Entity match = (Entity) argument;
          return match.getMediaType() == mediaType && entity.equals(match.getEntity());
        }
        return false;
      }
    };
  }
}