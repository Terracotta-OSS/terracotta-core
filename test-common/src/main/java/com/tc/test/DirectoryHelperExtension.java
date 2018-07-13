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
 * DirectoryHelperExtension
 */
public class DirectoryHelperExtension implements TestInstancePostProcessor {

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

}
