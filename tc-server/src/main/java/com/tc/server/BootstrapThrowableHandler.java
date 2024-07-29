/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.server;

import org.slf4j.Logger;

import com.tc.exception.ExceptionHelper;
import com.tc.exception.ExceptionHelperImpl;
import com.tc.exception.RuntimeExceptionHelper;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.handler.CallbackStartupExceptionLoggingAdapter;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.TCDataFileLockingException;
import com.tc.util.Throwables;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.net.BindException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.StopAction;

/**
 * Handles Throwable appropriately by printing messages to the logger, etc.
 */
public class BootstrapThrowableHandler implements ThrowableHandler {
  // XXX: The dispatching in this class is retarded, but I wanted to move as much of the exception handling into a
  // single place first, then come up with fancy ways of dealing with them. --Orion 03/20/2006

  // instantiating message here to avoid any allocations on OOME
  private static final String                        OOME_ERROR_MSG                  = "Fatal error: out of available memory. Exiting...";
  protected final Logger logger;
  private final ExceptionHelperImpl                  helper;
  private final List<CallbackOnExitHandler>          callbackOnExitDefaultHandlers   = new CopyOnWriteArrayList<CallbackOnExitHandler>();
  private final Map<Class<?>, CallbackOnExitHandler> callbackOnExitExceptionHandlers = new HashMap<Class<?>, CallbackOnExitHandler>();
  private final Object                               dumpLock                        = new Object();

  private static final long                          TIME_OUT                        = TCPropertiesImpl
                                                                                         .getProperties()
                                                                                       .getLong(TCPropertiesConsts.L2_DUMP_ON_EXCEPTION_TIMEOUT)
                                                                                       * 1000;
  private boolean                                    isExitScheduled;
  private boolean                                    isDumpTaken;

  /**
   * Construct a new ThrowableHandler with a logger
   * 
   * @param logger Logger
   */
  public BootstrapThrowableHandler(Logger logger) {
    this.logger = logger;
    helper = new ExceptionHelperImpl();
    helper.addHelper(new RuntimeExceptionHelper());
    registerStartupExceptionCallbackHandlers();
  }

  @Override
  public void addHelper(ExceptionHelper toAdd) {
    helper.addHelper(toAdd);
  }

  private void registerStartupExceptionCallbackHandlers() {
    String bindExceptionExtraMessage = ".  Please make sure the server isn't already running or choose a different port.";
    addCallbackOnExitExceptionHandler(BindException.class,
                                      new CallbackStartupExceptionLoggingAdapter(bindExceptionExtraMessage));
    addCallbackOnExitExceptionHandler(LocationNotCreatedException.class, new CallbackStartupExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(FileNotCreatedException.class, new CallbackStartupExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(TCDataFileLockingException.class, new CallbackStartupExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(TCNotRunningException.class, new CallbackShutdownExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(InterruptedException.class, new CallbackShutdownExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(IllegalStateException.class, new CallbackShutdownExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(TCRuntimeException.class, new CallbackShutdownExceptionLoggingAdapter());
    addCallbackOnExitExceptionHandler(ConfigurationException.class, new ConfigurationExceptionLoggingAdapter());
  }

  @Override
  public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
    callbackOnExitDefaultHandlers.add(callbackOnExitHandler);
  }

  @Override
  public void addCallbackOnExitExceptionHandler(Class<?> c, CallbackOnExitHandler exitHandler) {
    callbackOnExitExceptionHandlers.put(c, exitHandler);
  }

  /**
   * Handle throwable occurring on thread
   * 
   * @param thread Thread receiving Throwable
   * @param t Throwable
   */
  @Override
  public void handleThrowable(Thread thread, Throwable t) {
    handlePossibleOOME(t);

    final CallbackOnExitState throwableState = new CallbackOnExitState(t);

    final Throwable proximateCause = helper.getProximateCause(t);
    final Throwable ultimateCause = helper.getUltimateCause(t);

    Object registeredExitHandlerObject;
    try {
      if ((registeredExitHandlerObject = callbackOnExitExceptionHandlers.get(proximateCause.getClass())) != null) {
        try {
          ((CallbackOnExitHandler) registeredExitHandlerObject).callbackOnExit(throwableState);
        } catch (Throwable unhandled) {
          handleDefaultException(thread, new CallbackOnExitState(unhandled));
        }
      } else if ((registeredExitHandlerObject = callbackOnExitExceptionHandlers.get(ultimateCause.getClass())) != null) {
        try {
          ((CallbackOnExitHandler) registeredExitHandlerObject).callbackOnExit(throwableState);
        } catch (Throwable unhandled) {
          handleDefaultException(thread, new CallbackOnExitState(unhandled));
        }
      } else {
        handleDefaultException(thread, throwableState);
      }
    } catch (Throwable throwable) {
      logger.error("Error while handling uncaught expection" + t.getCause(), throwable);
    }

    exit(throwableState);
  }

  /**
   * Makes sure we don't allocate any heap objects on OOME.
   * {@code -XX:+HeapDumpOnOutOfMemoryError} should take care of debug information.
   * Considering {@code -XX:OnOutOfMemoryError=<cmd>} option might be also a good idea.
   */
  @Override
  public void handlePossibleOOME(Throwable t) {
    Throwable rootCause = Throwables.getRootCause(t);
    if (rootCause instanceof OutOfMemoryError) {
      try {
        logger.error(OOME_ERROR_MSG);
        String msg = rootCause.getMessage();
        if (msg != null && msg.length() > 0) {
          logger.error(msg);
        }
      } finally {
        exit(false);
      }
    }
  }

  private void handleDefaultException(Thread thread, CallbackOnExitState throwableState) {
    logException(thread, throwableState);

    synchronized (dumpLock) {
      if (!isDumpTaken) {
        isDumpTaken = true;
        for (CallbackOnExitHandler handler : callbackOnExitDefaultHandlers) {
          handler.callbackOnExit(throwableState);
        }
      }
    }
  }

  private void logException(Thread thread, CallbackOnExitState throwableState) {
    try {
      // We need to make SURE that our stacktrace gets printed, when using just the logger sometimes the VM exits
      // before the stacktrace prints
      Throwable cause = throwableState.getThrowable();

      TCLogging.getConsoleLogger().error("Thread:" + thread + " got an uncaught exception. calling CallbackOnExitDefaultHandlers. " + cause.getMessage(),
                   cause);
      int tab = 0;
      while (cause != null) {
        StringBuilder spaces = new StringBuilder(tab * 2);
        for (int x=0;x<tab*2;x++) spaces.setCharAt(x, ' ');
        TCLogging.getConsoleLogger().error("{}{}:{}",spaces,cause.getClass().getName(),cause.getMessage());
        cause = cause.getCause();
      }
    } catch (Exception e) {
      // IGNORE EXCEPTION HERE
    }
  }

  private void exit(CallbackOnExitState throwableState) {
    boolean autoRestart = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_AUTORESTART);
    if (!ServerEnv.getServer().isStopped()) {
      logger.info("ExitState : " + throwableState + "; AutoRestart: " + autoRestart);
      if (autoRestart && throwableState.isRestartNeeded()) {
        exit(true);
      } else {
        exit(false);
      }
    }
  }

  protected synchronized void exit(boolean status) {
    // let all the logging finish
//    ThreadUtil.reallySleep(2000);
    StopAction[] actions = status ? new StopAction[] {StopAction.RESTART} : new StopAction[0];
    ServerEnv.getServer().stop(actions);
  }
}
