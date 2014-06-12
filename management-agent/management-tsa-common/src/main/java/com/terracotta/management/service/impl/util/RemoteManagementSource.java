/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.Representable;

import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.web.utils.TSAConfig;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Ludovic Orban
 */
public class RemoteManagementSource {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteManagementSource.class);

  private static final String CONNECTION_TIMEOUT_HEADER_NAME = "X-Terracotta-Connection-Timeout";

  private static final String READ_TIMEOUT_HEADER_NAME = "X-Terracotta-Read-Timeout";

  private final LocalManagementSource localManagementSource;
  private final TimeoutService timeoutService;
  private final SecurityContextService securityContextService;
  private final Client client;

  public RemoteManagementSource(LocalManagementSource localManagementSource, TimeoutService timeoutService, SecurityContextService securityContextService) {
    this.localManagementSource = localManagementSource;
    this.timeoutService = timeoutService;
    this.securityContextService = securityContextService;

    
//    ClientConfig clientConfig = new DefaultClientConfig();
//    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//    Client c = Client.create(clientConfig);
//    c.addFilter(new GZIPContentEncodingFilter(false));
    
    
    client = ClientBuilder.newBuilder()
        .register(new JacksonFeature())
        .build();

    
    
//    this.client = c;
  }

  public void shutdown() {
    client.close();
  }

  public <T extends Representable> Collection<T> getFromRemoteL2(String serverName, URI uri, Class<T> type) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    return resource(UriBuilder.fromUri(serverUrl).uri(uri).build())
        .get(new CollectionOfRepresentableGenericType<T>(type));
  }

  public void postToRemoteL2(String serverName, URI uri) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    resource(UriBuilder.fromUri(serverUrl).uri(uri).build())
        .post(null);
  }

  public <T extends Representable> Object postToRemoteL2(String serverName, URI uri, Collection<T> entities) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    return resource(UriBuilder.fromUri(serverUrl).uri(uri).build())
        .post(Entity.entity(entities, MediaType.APPLICATION_JSON_TYPE), Boolean.class);
  }

  public <T extends Representable> Collection<T> postToRemoteL2(String serverName, URI uri, Class<T> type) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    return resource(UriBuilder.fromUri(serverUrl).uri(uri).build())
        .post(null, new CollectionOfRepresentableGenericType<T>(type));
  }

  public Invocation.Builder resource(URI uri) {
    WebTarget resource = client.target(uri);
    resource.property(ClientProperties.CONNECT_TIMEOUT, (int)timeoutService.getCallTimeout());
    resource.property(ClientProperties.READ_TIMEOUT, (int)timeoutService.getCallTimeout());
    resource.register(EncodingFilter.class); // Allow to process encodings
    resource.register(GZipEncoder.class);
    resource.register(DeflateEncoder.class);

    Builder builder = resource.request();

    if (TSAConfig.isSslEnabled()) {
      SecurityContextService.SecurityContext securityContext = securityContextService.getSecurityContext();
      builder = builder.header(IACredentials.REQ_TICKET, securityContext.getRequestTicket())
        .header(IACredentials.SIGNATURE, securityContext.getSignature())
        .header(IACredentials.ALIAS, securityContext.getAlias())
        .header(IACredentials.TC_ID_TOKEN, securityContext.getToken());
    }

    builder = builder.header(CONNECTION_TIMEOUT_HEADER_NAME, timeoutService.getCallTimeout());

    builder = builder.header(READ_TIMEOUT_HEADER_NAME, timeoutService.getCallTimeout());

    return builder;
  }

  public static String toCsv(Set<String> strings) {
    if (strings == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();

    for (String string : strings) {
      sb.append(string);
      sb.append(",");
    }
    if (!strings.isEmpty()) {
      sb.deleteCharAt(sb.length());
    }

    return sb.toString();
  }


  public <T extends Representable> Collection<T> collectEntitiesFromFutures(Map<String, Future<T>> futures, long timeoutInMillis, String methodName, int max) throws Exception {
    return collectEntitiesCollectionFromFutures(FutureAdapter.adapt(futures), timeoutInMillis, methodName, max);
  }

  public <T extends Representable> Collection<T> collectEntitiesCollectionFromFutures(Map<String, Future<Collection<T>>> futures, long timeoutInMillis, String methodName, int max) throws Exception {
    Collection<T> result = new ArrayList<T>();
    long timeLeft = timeoutInMillis;

    List<String> failedServerNames = new ArrayList<String>();
    List<Throwable> exceptions = new ArrayList<Throwable>();

    for (Map.Entry<String, Future<Collection<T>>> entry : futures.entrySet()) {
      String serverName = entry.getKey();
      Future<Collection<T>> future = entry.getValue();

      long before = System.nanoTime();
      try {
        Collection<T> entities = future.get(Math.max(1L, timeLeft), TimeUnit.MILLISECONDS);
        if (entities == null) { continue; }
        if (result.size() < max) {
          result.addAll(entities);
        }
      } catch (Exception e) {
        LOG.debug("Future execution error in {}:{}", serverName, methodName, e);
        exceptions.add(e);
        failedServerNames.add(serverName);
        future.cancel(true);
      } finally {
        timeLeft -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
      }
    }
    if (!exceptions.isEmpty()) {
      //throw new MultiException("Failed to collect data from the following remote endpoint(s): " + failedServerNames, exceptions);
      LOG.debug("Failed to collect data from the following remote endpoint(s): {}", failedServerNames, exceptions);
    }
    return result;
  }

  public void cancelFutures(Collection<?> futures) {
    for (Object o : futures) {
      Future<?> future = (Future<?>)o;
      future.cancel(true);
    }
  }

  public <T> Collection<T> merge(Collection<T> collection1, Collection<T> collection2) {
    Collection<T> result = new ArrayList<T>(collection1.size() + collection2.size());
    result.addAll(collection1);
    result.addAll(collection2);
    return result;
  }

  private static final class FutureAdapter<T> implements Future<Collection<T>> {
    private final Future<T> delegate;

    private FutureAdapter(Future<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
      return delegate.isDone();
    }

    @Override
    public Collection<T> get() throws InterruptedException, ExecutionException {
      T t = delegate.get();
      return Collections.singleton(t);
    }

    @Override
    public Collection<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      T t = delegate.get(timeout, unit);
      return Collections.singleton(t);
    }

    public static <T extends Representable> Map<String, Future<Collection<T>>> adapt(Map<String, Future<T>> futures) {
      Map<String, Future<Collection<T>>> result = new HashMap<String, Future<Collection<T>>>();

      for (Map.Entry<String, Future<T>> entry : futures.entrySet()) {
        String key = entry.getKey();
        Future<T> value = entry.getValue();

        FutureAdapter<T> adapter = new FutureAdapter<T>(value);
        result.put(key, adapter);
      }

      return result;
    }
  }

  private static final class CollectionOfRepresentableGenericType<T extends Representable> extends GenericType<Collection<T>> {
    public CollectionOfRepresentableGenericType(final Class<T> clazz) {
      super(new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return new Type[] { clazz };
        }

        @Override
        public Type getRawType() {
          return Collection.class;
        }

        @Override
        public Type getOwnerType() {
          return Collection.class;
        }

        @Override
        public String toString() {
          return "CollectionOfRepresentableGenericType<" + clazz.getName() + ">";
        }
      });
    }
  }

}
