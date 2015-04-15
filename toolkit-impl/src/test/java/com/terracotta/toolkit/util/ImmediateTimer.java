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
package com.terracotta.toolkit.util;

import com.tc.util.concurrent.Timer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
* @author tim
*/
public class ImmediateTimer implements Timer {
  private final ScheduledFuture<?> FUTURE = mock(ScheduledFuture.class);
  private Runnable savedCommand;

  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
    return runOrSave(command, delay);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
    return runOrSave(command, initialDelay);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
    return runOrSave(command, initialDelay);
  }

  @Override
  public void execute(final Runnable command) {
    command.run();
  }

  private ScheduledFuture<?> runOrSave(Runnable command, long delay) {
    if (delay == 0) {
      command.run();
    } else {
      savedCommand = command;
    }
    return FUTURE;
  }

  public void runSaved() {
    savedCommand.run();
    savedCommand = null;
  }

  @Override
  public void cancel() {

  }
}
