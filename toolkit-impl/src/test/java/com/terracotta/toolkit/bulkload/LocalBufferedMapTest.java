package com.terracotta.toolkit.bulkload;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class LocalBufferedMapTest {
  private LocalBufferedMap<String, String> bufferedMap;
  private BufferBackend<String, String> backend;
  private BulkLoadConstants bulkLoadConstants;
  private TaskRunner taskRunner;
  private ImmediateTimer timer;

  @Before
  public void setUp() throws Exception {
    timer = spy(new ImmediateTimer());
    taskRunner = when(mock(TaskRunner.class).newTimer(anyString())).thenReturn(timer).getMock();
    bulkLoadConstants = new BulkLoadConstants(TCPropertiesImpl.getProperties());
    backend = mock(BufferBackend.class);
    bufferedMap = new LocalBufferedMap<String, String>("foo", backend, bulkLoadConstants, taskRunner);
  }

  @Test
  public void testStartBufferingSchedulesTask() throws Exception {
    bufferedMap.startBuffering();
    verify(timer).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void testNoImmediateDrainOnPut() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("a", "b", -1, 0, 0, 0);
    verify(backend, never()).drain(anyMap());
  }

  @Test
  public void testNoImmediateDrainOnRemove() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.remove("foo", -1);
    verify(backend, never()).drain(anyMap());
  }

  @Test(expected = IllegalStateException.class)
  public void testPutFailsWhenBufferingOff() throws Exception {
    bufferedMap.put("foo", "bar", 0, 0, 0, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void testRemoveFailsWhenBufferingOff() throws Exception {
    bufferedMap.remove("foo", 0);
  }

  @Test
  public void testPut() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "bar", 1, 2, 3, 4))));
  }

  @Test
  public void testRemove() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.remove("foo", 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.REMOVE, null , 1, -1, -1, -1))));
  }

  @Test
  public void testClear() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.remove("foo", 2);
    bufferedMap.clear();
    timer.runSaved();
    verify(backend, never()).drain(anyMap());
  }

  @Test
  public void testConflatedPuts() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.put("foo", "baz", 4, 3, 2, 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "baz" , 4, 3, 2, 1))));
  }

  @Test
  public void testConflatedRemove() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.remove("foo", 5);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.REMOVE, null , 5, -1, -1, -1))));
  }

  @Test
  public void testPutIfAbsentDoesNotReplace() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.putIfAbsent("foo", "baz", 4, 3, 2, 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "bar" , 1, 2, 3, 4))));
  }

  @Test
  public void testPutIfAbsent() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.putIfAbsent("foo", "baz", 4, 3, 2, 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT_IF_ABSENT, "baz" , 4, 3, 2, 1))));
  }

  @Test
  public void testFlush() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.flush();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "bar", 1, 2, 3, 4))));
  }

  @Test
  public void testMultipleFlush() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.flush();
    bufferedMap.flush();
    verify(backend, only()).drain(anyMap());
  }

  @Test
  public void testStopBufferingFlushes() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    bufferedMap.flushAndStopBuffering();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "bar", 1, 2, 3, 4))));
  }

  @Test(expected = IllegalStateException.class)
  public void testStopBuffering() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.flushAndStopBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
  }

  @Test(expected = IllegalStateException.class)
  public void testMultipleStartBuffering() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.startBuffering();
  }

  @Test(expected = IllegalStateException.class)
  public void testMultipleStopBuffering() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.flushAndStopBuffering();
    bufferedMap.flushAndStopBuffering();
  }

  @Test(expected = IllegalStateException.class)
  public void testStopBeforeStart() throws Exception {
    bufferedMap.flushAndStopBuffering();
  }

  private Matcher<BufferedOperation<String>> operationWith(final BufferedOperation.Type type, final String value, final long version, final int creationTime, final int tti, final int ttl) {
    return new BaseMatcher<BufferedOperation<String>>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof BufferedOperation) {
          BufferedOperation<String> bufferedOperation = (BufferedOperation<String>) o;
          if (type != bufferedOperation.getType()) {
            return false;
          }
          if (value == null) {
            if (bufferedOperation.getValue() != null) {
              return false;
            }
          } else if (!value.equals(bufferedOperation.getValue())) {
            return false;
          }
          if (version != bufferedOperation.getVersion()) {
            return false;
          }
          if (creationTime != bufferedOperation.getCreateTimeInSecs()) {
            return false;
          }
          if (tti != bufferedOperation.getCustomMaxTTISeconds()) {
            return false;
          }
          if (ttl != bufferedOperation.getCustomMaxTTLSeconds()) {
            return false;
          }
          return true;
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
      }
    };
  }

  private class ImmediateTimer implements Timer {
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

    void runSaved() {
      savedCommand.run();
      savedCommand = null;
    }

    @Override
    public void cancel() {

    }
  }
}
