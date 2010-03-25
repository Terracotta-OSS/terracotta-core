/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.util.Counter;
import com.tc.util.tickertoken.msg.TickerTokenMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TickerTokenManagerImpl implements TickerTokenManager {

  private final int                                                  id;
  private final long                                                 timerPeriod;

  private final Map<Class, TickerTokenFactory>                       factoryMap         = Collections
                                                                                            .synchronizedMap(new HashMap<Class, TickerTokenFactory>());
  private final Map<Class, TickerTokenProcessor>                     tokenProcessorMap  = Collections
                                                                                            .synchronizedMap(new HashMap<Class, TickerTokenProcessor>());
  private final Map<TickerTokenKey, TickerTask>                      timerTaskMap       = Collections
                                                                                            .synchronizedMap(new HashMap<TickerTokenKey, TickerTask>());
  private final Map<Class, TickerToken>                              processCompleteMap = Collections
                                                                                            .synchronizedMap(new HashMap<Class, TickerToken>());
  private final ConcurrentHashMap<TickerTokenKey, TickerTokenHandle> tokenHandleMap     = new ConcurrentHashMap<TickerTokenKey, TickerTokenHandle>();

  private final ConcurrentHashMap<String, TickerTokenHandle>         lookupMap          = new ConcurrentHashMap<String, TickerTokenHandle>();

  private final int                                                  totalTickers;
  private final Counter                                              tickerCounter      = new Counter();
  private final Timer                                                timer              = new Timer(
                                                                                                    "Ticker Token Timer",
                                                                                                    true);

  public TickerTokenManagerImpl(int id, int totalTickers, long timerPeriod) {
    this.id = id;
    this.totalTickers = totalTickers;
    this.timerPeriod = timerPeriod;
  }

  public int getId() {
    return this.id;
  }

  public void registerTickerTokenProcessor(Class tokenClass, TickerTokenProcessor processor) {
    this.tokenProcessorMap.put(tokenClass, processor);
  }

  public void registerTickerTokenFactory(Class tokenClass, TickerTokenFactory factory) {
    this.factoryMap.put(tokenClass, factory);
  }

  private TickerTokenHandleImpl createHandle(String identifier, Class tickerTokenType, AtomicBoolean triggerToken) {
    int startTick = this.tickerCounter.increment();
    TickerTokenKey key = new TickerTokenKey(tickerTokenType, this.id, startTick);
    TickerTokenHandleImpl handle = new TickerTokenHandleImpl(identifier, key, triggerToken);
    if (this.lookupMap.putIfAbsent(identifier, handle) != null) {
      // One already exists for the identifier
      throw new TickerTokenException("Cant create Handle with same identifier : " + identifier
                                     + " when a current ticker exists. " + this.lookupMap);
    }
    return handle;
  }

  public TickerTokenHandle startTicker(String identifier, Class tickerTokenType) {
    AtomicBoolean triggerToken = new AtomicBoolean(false);
    TickerTokenHandleImpl handle = createHandle(identifier, tickerTokenType, triggerToken);
    TickerTokenKey key = handle.getKey();
    if ((this.tokenHandleMap.putIfAbsent(key, handle)) == null) {
      TickerTask task = new TickerTask(key.getStartTick(), this.totalTickers, this, getTickerTokenFactory(key
          .getClassType()), this.timerTaskMap, triggerToken);

      this.timer.schedule(task, this.timerPeriod, this.timerPeriod);
    } else {
      throw new TickerTokenException("Duplicate Handle with same identifier : " + identifier);
    }
    return handle;
  }

  public void cancelTicker(String identifier) {
    TickerTokenHandle handle;
    if ((handle = this.lookupMap.remove(identifier)) != null) {
      TickerTokenKey key = handle.getKey();
      TimerTask timerTask;
      if ((timerTask = this.timerTaskMap.remove(key)) != null) {
        timerTask.cancel();
        this.tokenHandleMap.remove(key);
      }
      handle.complete();
      processCompleteMap.remove(handle.getKey().getClassType());
    }
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
    if (this.id == primaryID) {
      if (validCompleteToken(token)) {
        synchronized (this) {
          if (evaluateComplete(token)) {
            complete(token);

          } else {
            enableTriggerToken(token);
          }
        }
      }
    } else {
      send(token);
    }
  }

  /**
   * Algorithm to determine completion: 1. check this see if any of the tokens are dirty, if so it has not complete 2.
   * check to see if the requests totals and processed totals is equal 3. the current token is complete if 1 and 2 is
   * satisfied. 4. if no previous token has been saved, then store this token as a completed token is the process map,
   * to compare to the next consecutive token.00 5. if the next token is not complete, then clear previous saved token,
   * if saved. 6. if a token is evaluated as complete, and a previous token is complete, check to see if all the maps
   * have equal entries, if so then return completed == true.
   */
  private boolean evaluateComplete(TickerToken token) {
    if (!token.evaluateComplete()) {
      processCompleteMap.remove(token.getClass());
      return false;
    }
    boolean complete = false;
    TickerToken previousToken = null;
    if ((previousToken = processCompleteMap.get(token.getClass())) != null) {
      if (previousToken.evaluateEqual(token)) {
        complete = true;
        processCompleteMap.remove(previousToken.getClass());
      } else {
        processCompleteMap.put(token.getClass(), token);
      }
    } else {
      processCompleteMap.put(token.getClass(), token);
    }

    return complete;
  }

  private TickerTokenFactory getTickerTokenFactory(Class tokenClass) {
    TickerTokenFactory factory = this.factoryMap.get(tokenClass);
    if (factory == null) { throw new AssertionError("factory for token class: " + tokenClass + " is not registered"); }
    return factory;
  }

  private TickerTokenProcessor getTickerTokenProcessor(Class tokenClass) {
    TickerTokenProcessor handler = this.tokenProcessorMap.get(tokenClass);
    if (handler == null) { throw new AssertionError("token handler for token class: " + tokenClass
                                                    + " is not registered"); }
    return handler;
  }

  private TickerTokenHandle removeCompleteHandler(TickerToken token) {
    TickerTokenHandle handle = this.tokenHandleMap.remove(new TickerTokenKey(token.getClass(), token.getPrimaryID(),
                                                                             token.getStartTick()));
    this.lookupMap.remove(handle.getIdentifier());
    this.processCompleteMap.remove(handle.getKey().getClassType());
    return handle;
  }

  private void enableTriggerToken(TickerToken token) {
    TickerTokenHandle handle = this.tokenHandleMap.get(new TickerTokenKey(token.getClass(), token.getPrimaryID(), token
        .getStartTick()));
    handle.enableTriggerToken();
  }

  private boolean validCompleteToken(TickerToken token) {
    return this.tokenHandleMap.get(new TickerTokenKey(token.getClass(), token.getPrimaryID(), token.getStartTick())) != null;
  }

  private TimerTask removeTimer(TickerToken token) {
    return this.timerTaskMap.remove(new TickerTokenKey(token));
  }

  private void complete(TickerToken token) {
    TimerTask t = removeTimer(token);
    if (t != null) {
      t.cancel();
      removeCompleteHandler(token).complete();
    }
  }

  static class TickerTask extends TimerTask {

    private final TickerTokenManager              manager;
    private final TickerTokenFactory              factory;
    private final Map<TickerTokenKey, TickerTask> timerTaskMap;
    private final int                             startTick;
    private final int                             totalTickers;
    private final AtomicBoolean                   triggerToken;

    private TickerTask(int startTick, int totalTickers, TickerTokenManager manager, TickerTokenFactory factory,
                       Map<TickerTokenKey, TickerTask> timerTaskMap, AtomicBoolean triggerToken) {
      this.startTick = startTick;
      this.totalTickers = totalTickers;
      this.manager = manager;
      this.factory = factory;
      this.timerTaskMap = timerTaskMap;
      this.triggerToken = triggerToken;
    }

    @Override
    public void run() {
      if (triggerToken.get()) {
        synchronized (triggerToken) {
          TickerToken token = this.factory.createTriggerToken(this.manager.getId(), this.startTick, this.totalTickers);
          this.timerTaskMap.put(new TickerTokenKey(token), this);
          this.manager.send(token);
          triggerToken.set(false);
        }
      }
    }

  }

}
