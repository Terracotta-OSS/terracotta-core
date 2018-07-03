/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.tc.exception.TCRuntimeException;
import com.tc.util.Banner;
import com.tc.util.runtime.ThreadDump;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static com.tc.test.TimeoutTimerConfig.DEFAULT_DUMP_INTERVAL_IN_MILLIS;
import static com.tc.test.TimeoutTimerConfig.DEFAULT_DUMP_THREADS_ON_TIMEOUT;
import static com.tc.test.TimeoutTimerConfig.DEFAULT_NUM_THREAD_DUMPS;
import static com.tc.test.TimeoutTimerConfig.DEFAULT_TIMEOUT_THRESHOLD_IN_MILLIS;

/**
 * TCExtension
 */
public class TCExtension implements TestInstancePostProcessor, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ExecutionCondition, TestExecutionExceptionHandler {

  private static final ScriptEngine engine      = new ScriptEngineManager().getEngineByName("nashorn");

  private final Timer timeoutTimer              = new Timer("Timeout Thread", true);
  private TimerTask   timerTask;

  private long                      timeoutThresholdInMillis  = DEFAULT_TIMEOUT_THRESHOLD_IN_MILLIS;

  // controls for thread dumping.
  private boolean                   dumpThreadsOnTimeout      = DEFAULT_DUMP_THREADS_ON_TIMEOUT;
  private int                       numThreadDumps            = DEFAULT_NUM_THREAD_DUMPS;
  private long                      dumpIntervalInMillis      = DEFAULT_DUMP_INTERVAL_IN_MILLIS;

  private final AtomicReference<Throwable> beforeTimeoutException    = new AtomicReference<Throwable>(null);


  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {

    Arrays.stream(testInstance.getClass()
        .getDeclaredFields())
        .filter(field -> (field.getType().isAssignableFrom(TempDirectoryHelper.class)))
        .findAny()
        .ifPresent(field -> {
          CleanDirectory cleanDirectory = field.getAnnotation(CleanDirectory.class);
          boolean isCleanDirectory = true;
          if(cleanDirectory != null) {
            isCleanDirectory = cleanDirectory.value();
          }

          try {
            Banner.infoBanner("Injecting " + testInstance.getClass().getSimpleName() + "#" + field.getName() + " with an instance of TempDirectoryHelper");
            if(!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            field.set(testInstance, new TempDirectoryHelper(testInstance.getClass(), isCleanDirectory));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            Banner.errorBanner("Could not inject " + testInstance.getClass().getSimpleName() + "#" + field.getName() +" with an instance of TempDirectoryHelper");
          }
        });

    Arrays.stream(testInstance.getClass()
        .getDeclaredFields())
        .filter(field -> (field.getType().isAssignableFrom(DataDirectoryHelper.class)))
        .findAny()
        .ifPresent(field -> {
          try {
            Banner.infoBanner("Injecting " + testInstance.getClass().getSimpleName() + "#" + field.getName() + " with an instance of DataDirectoryHelper");
            if(!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            field.set(testInstance, new DataDirectoryHelper(testInstance.getClass()));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            Banner.errorBanner("Could not inject " + testInstance.getClass().getSimpleName() + "#" + field.getName() +" with an instance of DataDirectoryHelper");
          }
        });
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    //TCLogging.disableLocking();

    extensionContext.getElement()
        .map(annotatedElement -> annotatedElement.getAnnotation(TimeoutTimerConfig.class))
        .ifPresent(timeoutTimerConfig -> {
          timeoutThresholdInMillis = timeoutTimerConfig.timeoutThresholdInMillis();
          dumpThreadsOnTimeout = timeoutTimerConfig.dumpThreadsOnTimeout();
          numThreadDumps = timeoutTimerConfig.numThreadDumps();
          dumpIntervalInMillis = timeoutTimerConfig.dumpIntervalInMillis();
        });
    
    scheduleTimeoutTask(extensionContext);
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    cancelTimeoutTask();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    // NOP
  }

  @Override
  public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
    // favor the "real" exception to make test fail. If there was a exception in the timeout callback,
    // make that able to fail the test too
    Throwable exceptionInTimeoutCallback = beforeTimeoutException.get();
    if (throwable != null) {
      if (exceptionInTimeoutCallback != null) {
        exceptionInTimeoutCallback.printStackTrace();
      }

      throw throwable;
    }
    
    if (exceptionInTimeoutCallback != null) {
      throw exceptionInTimeoutCallback;
    }
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    // NOP
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {

    Date allDisabledUntil = null;
    String disabledUntilString = extensionContext.getElement()
        .map(annotatedElement -> annotatedElement.getAnnotation(DisabledUntil.class))
        .map(disabledUntil -> disabledUntil.value())
        .orElse(null);
    if("INDEFINITE".equalsIgnoreCase(disabledUntilString)) {
       allDisabledUntil = new Date(Long.MAX_VALUE);
    }
    else if(disabledUntilString != null) {
      try {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        allDisabledUntil = format.parse(disabledUntilString);
      } catch (ParseException e) {
        // throwing runtime exception should cause each test case to fail
        // (provided you're disabling from the constructor
        // as directed)
        throw new TCRuntimeException(e);
      }
    }

    String reason = null;
    String testClassName = extensionContext.getTestClass().get().getSimpleName();
    Optional<Method> testMethod = extensionContext.getTestMethod();
    boolean isTestMethodPresent = testMethod.isPresent();
    
    if (allDisabledUntil != null) {
      if (new Date().before(allDisabledUntil)) {
        if (!isTestMethodPresent) {
          reason = "ALL tests in "
                   + testClassName
                   + " are DISABLED until " + allDisabledUntil;
        } else {
          reason = "Test "
                   + testClassName
                   + "#"
                   + testMethod.get().getName()
                   + " is DISABLED until " + allDisabledUntil;
        }

        Banner.warnBanner(reason);
        return ConditionEvaluationResult.disabled(reason);
      } else {
        // don't let timebomb go off on weekend
        // see INT-1173
        Calendar rightNow = Calendar.getInstance();
        int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
          reason =  "Timebomb is scheduled to expire on weekend (" + allDisabledUntil + "). Preventing it from going off. ";
          if (!isTestMethodPresent) {
            reason += "All tests in "
                      + testClassName
                      + " are SKIPPED.";
          } else {
            reason += "Test "
                      + testClassName
                      + "#"
                      + testMethod.get().getName()
                      + " is SKIPPED.";
          }
          
          Banner.warnBanner(reason);
          return ConditionEvaluationResult.disabled(reason);
        }

        if (!isTestMethodPresent) {
          reason = "Timebomb set on "
                   + testClassName
                   + " expired on "
                   + allDisabledUntil;
        } else {
          reason = "Timebomb set on "
                   + testClassName
                   + "#"
                   + testMethod.get().getName()
                   + " expired on "
                   + allDisabledUntil;
        }

        Banner.errorBanner(reason);
        throw new IllegalStateException("Timebomb has expired on " + allDisabledUntil);
      }
    }

    boolean isContainerTest = extensionContext.getElement().map(annotatedElement -> annotatedElement.getAnnotation(ContainerTest.class)).isPresent();

    // Tests marked as container test is executed only when the current test run is configured to run with App Server
    if (isContainerTest ^ isConfiguredToRunWithAppServer()) {
      if (!isTestMethodPresent) {
        reason = "All tests in "
                 + testClassName
                 + " are SKIPPED  because system test trying to run with appserver or container test running without an appserver.";
      } else {
        reason = "Test "
                 + testClassName
                 + "#"
                 + testMethod.get().getName()
                 + " is SKIPPED because system test trying to run with appserver or container test running without an appserver.";
      }
      Banner.warnBanner(reason);
      return ConditionEvaluationResult.disabled(reason);
    }

    return ConditionEvaluationResult.enabled(null);
  }

  private void scheduleTimeoutTask(ExtensionContext extensionContext) {
    // enforce some sanity
    final int MINIMUM = 30;
    long junitTimeout = this.getTimeoutValueInSeconds();

    if (junitTimeout < MINIMUM) { throw new IllegalArgumentException("Junit timeout cannot be less than " + MINIMUM
                                                                     + " seconds"); }

    final int MIN_THRESH = 15000;
    junitTimeout *= 1000;
    if ((junitTimeout - timeoutThresholdInMillis) < MIN_THRESH) {
      Banner.errorBanner("Cannot apply timeout threshold of " + timeoutThresholdInMillis + ", using " + MIN_THRESH
                         + " instead");
      timeoutThresholdInMillis = MIN_THRESH;
    }

    final long delay = junitTimeout - timeoutThresholdInMillis;

    Banner.infoBanner("Timeout task is scheduled to run in " + TimeUnit.MILLISECONDS.toMinutes(delay) + " minutes");

    // cancel the old task
    if (timerTask != null) {
      timerTask.cancel();
    }
    
    final Thread testVMThread = Thread.currentThread();
    timerTask = new TimerTask() {

      @Override
      public void run() {
        timeoutCallback(delay, extensionContext);

        // DEV-8901 interrupt the test VM thread so that if its waiting somewhere, It comes out and the test vm exits.
        testVMThread.interrupt();
      }
    };
    timeoutTimer.schedule(timerTask, delay);
  }

  private void cancelTimeoutTask() {
      if (timerTask != null) {
        timerTask.cancel();
      }
  }

  // called by timer thread (ie. NOT the main thread of test case)
  private void timeoutCallback(long elapsedTime, ExtensionContext extensionContext) {
    Banner.errorBanner("TCTestCase timeout alarm going off after "
                 + TimeUnit.MILLISECONDS.toMinutes(elapsedTime) + " minutes at " + new Date());

    // Invoke the method annotated by "DoDumpServerDetails" if it exists
    try {
      invokeStaticMethodWithAnnotation(extensionContext, DoDumpServerDetails.class);
    } catch (Throwable t) {
      // don't fail the test b/c of this
      t.printStackTrace();
    }

    // doDumpServerDetails();
    if (dumpThreadsOnTimeout) {
      try {
        doThreadDump();
      } catch (Throwable t) {
        // don't fail the test b/c of this
        t.printStackTrace();
      }
    }

    // Invoke the method annotated by "BeforeTimeout" if it exists
    try {
      invokeStaticMethodWithAnnotation(extensionContext, BeforeTimeout.class);
    } catch (Throwable t) {
      this.beforeTimeoutException.set(t);
    }
  }

  private void invokeStaticMethodWithAnnotation(ExtensionContext extensionContext, Class<? extends Annotation> annotationClass) throws Throwable {

    Optional<Throwable> exceptionThrownByInvokedMethod = Arrays.stream(extensionContext.getRequiredTestClass()
        .getDeclaredMethods())
        .filter(method -> (method.isAnnotationPresent(annotationClass) && Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0))
        .findAny()
        .map(method -> {
          try {
            method.invoke(null);
          } catch (Throwable t) {
            return t;
          }

          return null;
        });

    if(exceptionThrownByInvokedMethod.isPresent()) {
      throw exceptionThrownByInvokedMethod.get();
    }
  }

  private void doThreadDump() {
    ThreadDump.dumpAllJavaProcesses(numThreadDumps, dumpIntervalInMillis);
  }

  /**
   * Returns the timeout value
   */
  private int getTimeoutValueInSeconds() {
    return TestConfigObject.getInstance().getJunitTimeoutInSeconds();
  }

  private boolean isConfiguredToRunWithAppServer() {
    return !"unknown".equals(TestConfigObject.getInstance().appServerInfo().getName());
  }


  public static void dumpHeap(File destDir) {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      String hotSpotDiagName = "com.sun.management:type=HotSpotDiagnostic";
      ObjectName name = new ObjectName(hotSpotDiagName);
      String operationName = "dumpHeap";

      File tempFile = new File(destDir, "heapDump_" + (System.currentTimeMillis()) + ".hprof");
      String dumpFilename = tempFile.getAbsolutePath();

      Object[] params = new Object[] { dumpFilename, Boolean.TRUE };
      String[] signature = new String[] { String.class.getName(), boolean.class.getName() };
      mbs.invoke(name, operationName, params, signature);

      Banner.infoBanner("Dumped heap in file " + dumpFilename);
    } catch (Exception e) {
      Banner.errorBanner("Could not dump heap: " + e.getMessage());
    }
  }
}
