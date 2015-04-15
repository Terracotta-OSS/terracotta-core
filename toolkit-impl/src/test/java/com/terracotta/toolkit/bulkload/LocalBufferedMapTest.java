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
package com.terracotta.toolkit.bulkload;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.TaskRunner;
import com.terracotta.toolkit.util.ImmediateTimer;

import java.util.Map;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    backend = spy(new TestBufferedBackend());
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
  public void testPutCreatesBufferedOperation() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.put("foo", "bar", 1, 2, 3, 4);
    verify(backend).createBufferedOperation(BufferedOperation.Type.PUT, "foo", "bar", 1, 2, 3, 4);
  }

  @Test
  public void testRemove() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.remove("foo", 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.REMOVE, null , 1, -1, -1, -1))));
  }

  @Test
  public void testRemoveCreatesBufferedOperation() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.remove("foo", 1);
    verify(backend).createBufferedOperation(BufferedOperation.Type.REMOVE, "foo", null, 1, -1, -1, -1);
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
    verify(backend).drain((Map<String, BufferedOperation<String>>)argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT, "bar", 1, 2, 3, 4))));
  }

  @Test
  public void testPutIfAbsent() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.putIfAbsent("foo", "baz", 4, 3, 2, 1);
    timer.runSaved();
    verify(backend).drain((Map<String,BufferedOperation<String>>) argThat(hasEntry(is("foo"), operationWith(BufferedOperation.Type.PUT_IF_ABSENT, "baz" , 4, 3, 2, 1))));
  }

  @Test
  public void testPutIfAbsentCreatesBufferedOperation() throws Exception {
    bufferedMap.startBuffering();
    bufferedMap.putIfAbsent("foo", "baz", 4, 3, 2, 1);
    verify(backend).createBufferedOperation(BufferedOperation.Type.PUT_IF_ABSENT, "foo", "baz", 4, 3, 2, 1);
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
    verify(backend, times(1)).drain(anyMap());
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

  private class TestBufferedBackend implements BufferBackend {
    @Override
    public void drain(final Map buffer) {
    }

    @Override
    public BufferedOperation createBufferedOperation(final BufferedOperation.Type type, final Object key, final Object value, final long version, final int createTimeInSecs, final int customMaxTTISeconds, final int customMaxTTLSeconds) {
      BufferedOperation bo = mock(BufferedOperation.class);
      when(bo.getType()).thenReturn(type);
      when(bo.getValue()).thenReturn(value);
      when(bo.getVersion()).thenReturn(version);
      when(bo.getCreateTimeInSecs()).thenReturn(createTimeInSecs);
      when(bo.getCustomMaxTTISeconds()).thenReturn(customMaxTTISeconds);
      when(bo.getCustomMaxTTLSeconds()).thenReturn(customMaxTTLSeconds);
      return bo;
    }
  }
}
