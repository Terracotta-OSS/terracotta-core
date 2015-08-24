package com.tc.services;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;

import org.terracotta.entity.ServiceProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.Service;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;


class ResponseWaiter implements Future<Void> {
  private boolean done;

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public synchronized boolean isDone() {
    return done;
  }

  @Override
  public synchronized Void get() throws InterruptedException, ExecutionException {
    while (!done) {
      wait();
    }
    return null;
  }

  @Override
  public synchronized Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long timeoutTime = unit.toNanos(timeout) + System.nanoTime();
    while (!done) {
      long waitTime = TimeUnit.NANOSECONDS.toMillis(timeoutTime - System.nanoTime());
      if (waitTime <= 0) {
        throw new TimeoutException();
      }
      wait(waitTime);
    }
    return null;
  }

  synchronized void done() {
    done = true;
    notifyAll();
  }
}

public class CommunicatorService implements ServiceProvider, DSOChannelManagerEventListener {
  private final ConcurrentMap<NodeID, ClientAccount> clientAccounts = new ConcurrentHashMap<>();

  public CommunicatorService(DSOChannelManager dsoChannelManager) {
    dsoChannelManager.addEventListener(this);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    clientAccounts.put(channel.getRemoteNodeID(), new ClientAccount(channel));
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    ClientAccount clientAccount = clientAccounts.remove(channel.getRemoteNodeID());
    if (clientAccount != null) {
      clientAccount.close();
    }
  }

  void response(NodeID nodeID, long responseId) {
    ClientAccount clientAccount = clientAccounts.get(nodeID);
    if (clientAccount != null) {
      clientAccount.response(responseId);
    }
  }


  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    ///Nothing here
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Service<T> getService(long consumerID, ServiceConfiguration<T> configuration) {
    return (Service<T>) new EntityClientCommunicatorService(clientAccounts);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(ClientCommunicator.class);
  }

  @Override
  public void close() {
    clientAccounts.values().stream().forEach(a->a.close());
    clientAccounts.clear();
  }
  
  
}
