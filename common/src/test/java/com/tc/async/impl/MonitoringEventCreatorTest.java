/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.async.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that MonitoringEventCreator does not eagerly build the debug string for each
 * enqueued stage event: the event's toString() is only invoked when a JFR recording will
 * actually consume the event, while stage statistics are always collected.
 */
public class MonitoringEventCreatorTest {

  private static final String STAGE_NAME = "testStage";
  private static final String SEDA_EVENT_NAME = "org.terracotta.tripwire.MonitoringEvent";

  private static class CountingContext {
    private int toStringCalls = 0;

    @Override
    public String toString() {
      toStringCalls++;
      return "counting-context";
    }
  }

  @Test
  public void testToStringNotCalledWithoutRecording() throws Exception {
    CountingContext context = new CountingContext();
    MonitoringEventCreator<CountingContext> creator = new MonitoringEventCreator<>(STAGE_NAME, event -> () -> {});

    creator.createEvent(context).call();

    assertEquals("toString must not be invoked when no JFR recording is active", 0, context.toStringCalls);
  }

  @Test
  public void testStatsStillCollectedWithoutRecording() throws Exception {
    CountingContext context = new CountingContext();
    MonitoringEventCreator<CountingContext> creator = new MonitoringEventCreator<>(STAGE_NAME, event -> () -> {});

    creator.createEvent(context).call();
    creator.createEvent(context).call();

    Map<String, ?> state = creator.getState();
    assertTrue("state must contain queueTime", state.containsKey("queueTime"));
    assertTrue("state must contain runTime", state.containsKey("runTime"));
    assertEquals("count must reflect 2 completed events", 2L, ((LongAdder)state.get("count")).sum());
  }

  @Test
  public void testDescriptionPopulatedWhenRecordingActive() throws Exception {
    CountingContext context = new CountingContext();
    MonitoringEventCreator<CountingContext> creator = new MonitoringEventCreator<>(STAGE_NAME, event -> () -> {});

    Path recordingFile = Files.createTempFile("seda-event-recording", ".jfr");
    recordingFile.toFile().deleteOnExit();
    Recording recording = new Recording();
    recording.enable(SEDA_EVENT_NAME).withoutThreshold();
    recording.start();
    try {
      creator.createEvent(context).call();
      recording.dump(recordingFile);
    } finally {
      recording.stop();
      recording.close();
    }

    assertEquals("toString must be invoked once when a recording is active", 1, context.toStringCalls);
    List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
    assertTrue("recording must contain the SEDA event", events.size() == 1);
    assertEquals("event stage must match", STAGE_NAME, events.get(0).getString("stage"));
    assertEquals("event description must match the event toString", "counting-context", events.get(0).getString("description"));
  }
}