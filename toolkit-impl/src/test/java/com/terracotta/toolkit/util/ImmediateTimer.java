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
