/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.msg.TickerTokenMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class TickerTokenManager {

  private final int                                    id;
  private final int                                    timerPeriod;

  private final Map<Class, TickerTokenFactory>         factoryMap        = Collections
                                                                             .synchronizedMap(new HashMap<Class, TickerTokenFactory>());
  private final Map<Class, TickerTokenProcessor>       tokenProcessorMap = Collections
                                                                             .synchronizedMap(new HashMap<Class, TickerTokenProcessor>());
  private final Map<TickerTokenKey, TimerTask>         timerTaskMap      = Collections
                                                                             .synchronizedMap(new HashMap<TickerTokenKey, TimerTask>());
  private final Map<TickerTokenKey, TickerTokenHandle> tokenHandleMap    = Collections
                                                                             .synchronizedMap(new HashMap<TickerTokenKey, TickerTokenHandle>());

  private final Timer                                  timer             = new Timer("Ticker Token Timer", false);

  private final int                                    totalTickers;
  private final Counter                                tickerCounter     = new Counter();

  public TickerTokenManager(int id, int totalTickers, int timerPeriod) {
    this.id = id;
    this.totalTickers = totalTickers;
    this.timerPeriod = timerPeriod;
  }

  public int getId() {
    return id;
  }

  public void registerTickerTokenProcessor(Class tokenClass, TickerTokenProcessor processor) {
    tokenProcessorMap.put(tokenClass, processor);
  }

  public void registerTickerTokenFactory(Class tokenClass, TickerTokenFactory factory) {
    factoryMap.put(tokenClass, factory);
  }

  public TickerTokenHandle startTicker(Class tickerTokenType, TickerTokenHandle handle) {
    int startTick = tickerCounter.increment();
    TickerTask task = new TickerTask(startTick, this.totalTickers, this, getTickerTokenFactory(tickerTokenType),
                                     timerTaskMap);
    timer.schedule(task, timerPeriod, timerPeriod);
    TickerTokenKey key = new TickerTokenKey(tickerTokenType, id, startTick);
    tokenHandleMap.put(key, handle);
    timerTaskMap.put(key, task);
    handle.setKey(key);
    return handle;
  }

  public void cancelTicker(TickerTokenHandle handle) {
    TimerTask timerTask = null;

    TickerTokenKey key = handle.getKey();
    if (key != null) {
      if ((timerTask = timerTaskMap.remove(key)) != null) {
        timerTask.cancel();
        tokenHandleMap.remove(key);
      }
    }
    handle.complete();
  }

  public void send(TickerToken token) {
    TickerTokenMessage message = getTickerTokenFactory(token.getClass()).createMessage(token);
    sendMessage(message);
  }

  public abstract void sendMessage(TickerTokenMessage message);

  public void receive(TickerToken token) {
    TickerTokenProcessor processor = getTickerTokenProcessor(token.getClass());
    processor.processToken(token);

    int primaryID = token.getPrimaryID();
    if (id == primaryID) {
      if (validCompleteToken(token)) {
        synchronized (this) {

          if (token.evaluateComplete()) {
            complete(token);
          }
        }
      }
    } else {
      send(token);
    }
  }

  private TickerTokenFactory getTickerTokenFactory(Class tokenClass) {
    TickerTokenFactory factory = factoryMap.get(tokenClass);
    if (factory == null) { throw new AssertionError("factory for token class: " + tokenClass + " is not registered"); }
    return factory;
  }

  private TickerTokenProcessor getTickerTokenProcessor(Class tokenClass) {
    TickerTokenProcessor handler = tokenProcessorMap.get(tokenClass);
    if (handler == null) { throw new AssertionError("token handler for token class: " + tokenClass
                                                    + " is not registered"); }
    return handler;
  }

  private TickerTokenHandle removeCompleteHandler(TickerToken token) {
    return tokenHandleMap.remove(new TickerTokenKey(token.getClass(), token.getPrimaryID(), token.getStartTick()));
  }

  private TimerTask removeTimer(TickerToken token) {
    return timerTaskMap.remove(new TickerTokenKey(token));
  }

  private boolean validCompleteToken(TickerToken token) {
    return tokenHandleMap.get(new TickerTokenKey(token.getClass(), token.getPrimaryID(), token.getStartTick())) != null;
  }

  private void complete(TickerToken token) {
    TimerTask t = removeTimer(token);
    if (t != null) {
      t.cancel();
      removeCompleteHandler(token).complete();
    }
  }

  private static class TickerTask extends TimerTask {

    private final TickerTokenManager             manager;
    private final TickerTokenFactory             factory;
    private final Map<TickerTokenKey, TimerTask> timerTaskMap;
    private final int                            startTick;
    private final int                            totalTickers;

    private TickerTask(int startTick, int totalTickers, TickerTokenManager manager, TickerTokenFactory factory,
                       Map<TickerTokenKey, TimerTask> timerTaskMap) {
      this.startTick = startTick;
      this.totalTickers = totalTickers;
      this.manager = manager;
      this.factory = factory;
      this.timerTaskMap = timerTaskMap;
    }

    @Override
    public void run() {

      TickerToken token = factory.createTriggerToken(manager.getId(), startTick, totalTickers);
      timerTaskMap.put(new TickerTokenKey(token), this);
      manager.send(token);

    }

  }
}
