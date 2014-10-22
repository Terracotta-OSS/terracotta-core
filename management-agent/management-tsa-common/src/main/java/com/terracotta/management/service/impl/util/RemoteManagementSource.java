/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.ErrorEntity;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.SubGenericType;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Ludovic Orban
 */
public class RemoteManagementSource {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteManagementSource.class);

  private static final String CONNECTION_TIMEOUT_HEADER_NAME = "X-Terracotta-Connection-Timeout";
  private static final String READ_TIMEOUT_HEADER_NAME = "X-Terracotta-Read-Timeout";
  private static final String CLEAN_ME_MARKER = "___CLEAN_ME___";

  private final LocalManagementSource localManagementSource;
  private final TimeoutService timeoutService;
  private final SecurityContextService securityContextService;
  private final Client client;
  private final Map<RemoteTSAEventListener, Collection<Future<EventInput>>> eventListenerFutures = Collections.synchronizedMap(new IdentityHashMap<RemoteTSAEventListener, Collection<Future<EventInput>>>());

  public RemoteManagementSource(LocalManagementSource localManagementSource, TimeoutService timeoutService, SecurityContextService securityContextService, SSLContextFactory sslContextFactory) {
    this.localManagementSource = localManagementSource;
    this.timeoutService = timeoutService;
    this.securityContextService = securityContextService;

    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    if (sslContextFactory != null) {
      try {
        clientBuilder.sslContext(sslContextFactory.create());
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(
            "Failure instantiating Jersey client due to invalid KeyManagerFactory algorithm.", e);
      } catch (IOException e) {
        throw new RuntimeException("Failure instantiating Jersey client due to inability to load keyStore.", e);
      } catch (KeyStoreException e) {
        throw new RuntimeException("Failure instantiating Jersey client due to invalid KeyStore type.", e);
      } catch (CertificateException e) {
        throw new RuntimeException("Failure instantiating Jersey client due to invalid certificates in a KeyStore.",
            e);
      } catch (UnrecoverableKeyException e) {
        throw new RuntimeException("Failure instantiating Jersey client due to bad key in a KeyStore.", e);
      } catch (KeyManagementException e) {
        throw new RuntimeException(
            "Failure instantiating Jersey client due to one or more invalid keys in a KeyStore.", e);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Failure instantiating Jersey client due to bad store location.", e);
      }

      HostnameVerifier hostnameVerifier;
      if (Boolean.getBoolean("tc.ssl.disableHostnameVerifier")) {
        hostnameVerifier = new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        };
      } else {
        //DEV-8842 : returning false every time means :
        // we don't allow the hostname to be different from what is configured in the cert.
        hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
      }
      clientBuilder.hostnameVerifier(hostnameVerifier);
    }

    // do not register the EncodingFilter, GZipEncoder and DeflateEncoder here
    // as a Jersey bug breaks SSE flow when they are enabled.
    // Only the non-SSE resources will register them for now.
    this.client = clientBuilder.build();
    client.register(SseFeature.class);
  }

  // test ctor
  RemoteManagementSource(LocalManagementSource localManagementSource, TimeoutService timeoutService, SecurityContextService securityContextService, Client client) {
    this.localManagementSource = localManagementSource;
    this.timeoutService = timeoutService;
    this.securityContextService = securityContextService;
    this.client = client;
  }

  public void shutdown() {
    client.close();
  }

  /**
   * Perform a GET on the specified URI of the specified server and return an object of type 'type' generified to 'subType'.
   * @throws ManagementSourceException
   */
  public <T, S, R extends T> R getFromRemoteL2(String serverName, URI uri, Class<T> type, Class<S> subType) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    URI fullUri = UriBuilder.fromUri(serverUrl).uri(uri).build();
    Builder resource = resource(fullUri);
    AtomicBoolean flag = new AtomicBoolean(false);
    client.property(CLEAN_ME_MARKER, flag);
    try {
      return (R) resource.get(new SubGenericType<T, S>(type, subType));
    } catch (WebApplicationException wae) {
      ErrorEntity errorEntity = createErrorEntity(wae);
      throw new ManagementSourceException("GET " + fullUri + " failed", errorEntity);
    } finally {
      flag.set(true);
      cleanup(client, CLEAN_ME_MARKER);
    }
  }

  /**
   * Perform an empty POST on the specified URI of the specified server and return nothing.
   * @throws ManagementSourceException
   */
  public void postToRemoteL2(String serverName, URI uri) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    URI fullUri = UriBuilder.fromUri(serverUrl).uri(uri).build();
    Builder resource = resource(fullUri);
    AtomicBoolean flag = new AtomicBoolean(false);
    client.property(CLEAN_ME_MARKER, flag);
    try {
      resource.post(null);
    } catch (WebApplicationException wae) {
      ErrorEntity errorEntity = createErrorEntity(wae);
      throw new ManagementSourceException("POST(1) " + fullUri + " failed", errorEntity);
    } finally {
      flag.set(true);
      cleanup(client, CLEAN_ME_MARKER);
    }
  }

  /**
   * Perform a POST of 'entities' on the specified URI of the specified server and return an object of type 'returnType'.
   * @throws ManagementSourceException
   */
  public <T extends Representable, R> R postToRemoteL2(String serverName, URI uri, Collection<T> entities, Class<R> returnType) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    URI fullUri = UriBuilder.fromUri(serverUrl).uri(uri).build();
    Builder resource = resource(fullUri);
    AtomicBoolean flag = new AtomicBoolean(false);
    client.property(CLEAN_ME_MARKER, flag);
    try {
      return resource.post(Entity.entity(entities, MediaType.APPLICATION_JSON_TYPE), returnType);
    } catch (WebApplicationException wae) {
      ErrorEntity errorEntity = createErrorEntity(wae);
      throw new ManagementSourceException("POST(2) " + fullUri + " failed", errorEntity);
    } finally {
      flag.set(true);
      cleanup(client, CLEAN_ME_MARKER);
    }
  }

  /**
   * Perform an empty POST on the specified URI of the specified server and return an object of type 'type' generified to 'subType'.
   * @throws ManagementSourceException
   */
  public <T, S, R extends T> R postToRemoteL2(String serverName, URI uri, Class<T> returnType, Class<S> returnSubType) throws ManagementSourceException {
    String serverUrl = localManagementSource.getRemoteServerUrls().get(serverName);
    URI fullUri = UriBuilder.fromUri(serverUrl).uri(uri).build();
    Builder resource = resource(fullUri);
    AtomicBoolean flag = new AtomicBoolean(false);
    client.property(CLEAN_ME_MARKER, flag);
    try {
      return (R) resource.post(null, new SubGenericType<T, S>(returnType, returnSubType));
    } catch (WebApplicationException wae) {
      ErrorEntity errorEntity = createErrorEntity(wae);
      throw new ManagementSourceException("POST(3) " + fullUri + " failed", errorEntity);
    } finally {
      flag.set(true);
      cleanup(client, CLEAN_ME_MARKER);
    }
  }

  static final AtomicBoolean NOTIFICATION_LOGGED = new AtomicBoolean(false);
  static void cleanup(Client client, String markerProperty) {
    try {
      Field listenersField = client.getClass().getDeclaredField("listeners");
      listenersField.setAccessible(true);
      LinkedBlockingDeque<?> lbdq = (LinkedBlockingDeque<?>)listenersField.get(client);
      Iterator<?> it = lbdq.iterator();
      while (it.hasNext()) {
        Object listener = it.next();
        Field confRuntimeField = listener.getClass().getDeclaredField("val$crt");
        confRuntimeField.setAccessible(true);
        Object clientRuntime = confRuntimeField.get(listener);
        Method getConfigMethod = clientRuntime.getClass().getMethod("getConfig");
        getConfigMethod.setAccessible(true);
        ClientConfig clientConfig = (ClientConfig)getConfigMethod.invoke(clientRuntime);

        AtomicBoolean cleanme = (AtomicBoolean)clientConfig.getProperty(markerProperty);
        if (cleanme.get()) {
          it.remove();
        }
      }
    } catch (Exception e) {
      if (NOTIFICATION_LOGGED.compareAndSet(false, true)) {
        LOG.error("Unable to cleanup Jersey 2.6 Client listeners, you may run into a memory leak!", e);
      }
    }
  }

  private ErrorEntity createErrorEntity(WebApplicationException wae) {
    try {
      return wae.getResponse().readEntity(ErrorEntity.class);
    } catch (Exception e) {
      return ExceptionUtils.toErrorEntity(wae);
    }
  }

  private Invocation.Builder sseResource(URI uri) {
    WebTarget resource = client.target(uri);
    Builder builder = resource.request();
    if (TSAConfig.isSslEnabled()) {
      SecurityContextService.SecurityContext securityContext = securityContextService.getSecurityContext();
      builder = builder.header(IACredentials.REQ_TICKET, securityContext.getRequestTicket())
          .header(IACredentials.SIGNATURE, securityContext.getSignature())
          .header(IACredentials.ALIAS, securityContext.getAlias())
          .header(IACredentials.TC_ID_TOKEN, securityContext.getToken());
    }
    return builder;
  }

  public Invocation.Builder resource(URI uri) {
    return resource(uri, true);
  }

  public Invocation.Builder resource(URI uri, boolean enableCompression) {
    WebTarget resource = client.target(uri);
    if (enableCompression) {
      resource.register(EncodingFilter.class);
    }
    resource.register(GZipEncoder.class);
    resource.register(DeflateEncoder.class);
    resource.property(ClientProperties.CONNECT_TIMEOUT, (int)timeoutService.getCallTimeout());
    resource.property(ClientProperties.READ_TIMEOUT, (int)timeoutService.getCallTimeout());

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

  public static interface RemoteTSAEventListener {
    void onEvent(InboundEvent inboundEvent);
    void onError(Throwable throwable);
  }

  public void addTsaEventListener(final RemoteTSAEventListener listener) {
    Map<String, String> remoteServerUrls = localManagementSource.getRemoteServerUrls();
    for (String serverUrl : remoteServerUrls.values()) {
      final AsyncInvoker async = sseResource(UriBuilder.fromUri(serverUrl)
          .uri("/tc-management-api/v2/events")
          .queryParam("localOnly", "true")
          .build()).async();

      Future<EventInput> f = async.get(new InvocationCallback<EventInput>() {
        @Override
        public void completed(EventInput eventInput) {
          while (true) {
            InboundEvent inboundEvent = eventInput.read();
            if (inboundEvent == null) {
              break;
            }
            listener.onEvent(inboundEvent);
          }

          failed(new EOFException("Remote event listener closed"));
        }

        @Override
        public void failed(Throwable throwable) {
          if (throwable instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException)throwable;
            if (wae.getResponse().getStatus() == 401) {
              // IA error -> disconnect
              listener.onError(throwable);
              clearAndCancelFutures(listener);
              return;
            }
          }

          if (throwable instanceof InterruptedException) {
            listener.onError(throwable);
            clearAndCancelFutures(listener);
            return;
          }

          try {
            Thread.sleep(eventReadFailureRetryDelayInMs());
          } catch (InterruptedException ie) {
            listener.onError(throwable);
            clearAndCancelFutures(listener);
            return;
          }

          // restart the request
          Future<EventInput> newFuture = async.get(this);
          addFuture(listener, newFuture);
          clearDoneFutures(listener);
        }
      });

      addFuture(listener, f);
    }
  }

  // for testing
  protected long eventReadFailureRetryDelayInMs() {
    return 1000L;
  }

  private void addFuture(RemoteTSAEventListener listener, Future<EventInput> f) {
    Collection<Future<EventInput>> futureList = eventListenerFutures.get(listener);
    if (futureList == null) {
      futureList = new ArrayList<Future<EventInput>>();
      eventListenerFutures.put(listener, futureList);
    }
    synchronized (futureList) {
      futureList.add(f);
    }
  }

  private void clearDoneFutures(RemoteTSAEventListener listener) {
    Collection<Future<EventInput>> futureList = eventListenerFutures.get(listener);
    if (futureList != null) {
      synchronized (futureList) {
        Iterator<Future<EventInput>> it = futureList.iterator();
        while (it.hasNext()) {
          Future<EventInput> future = it.next();
          if (future.isDone() || future.isCancelled()) {
            it.remove();
          }
        }
      }
    }
  }

  private void clearAndCancelFutures(RemoteTSAEventListener listener) {
    Collection<Future<EventInput>> futureList = eventListenerFutures.remove(listener);
    if (futureList != null) {
      synchronized (futureList) {
        Iterator<Future<EventInput>> it = futureList.iterator();
        while (it.hasNext()) {
          Future<EventInput> future = it.next();
          future.cancel(true);
          it.remove();
        }
      }
    }
  }

  public void removeTsaEventListener(RemoteTSAEventListener listener) {
    Collection<Future<EventInput>> futureList = eventListenerFutures.remove(listener);
    if (futureList != null) {
      synchronized (futureList) {
        for (Future<EventInput> future : futureList) {
          future.cancel(true);
        }
      }
    }
  }

  public static String toCsv(Collection<String> strings) {
    if (strings == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();

    for (String string : strings) {
      sb.append(string);
      sb.append(",");
    }
    if (!strings.isEmpty()) {
      sb.deleteCharAt(sb.length() - 1);
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
      return t == null ? null : Collections.singleton(t);
    }

    @Override
    public Collection<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      T t = delegate.get(timeout, unit);
      return t == null ? null : Collections.singleton(t);
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


}
