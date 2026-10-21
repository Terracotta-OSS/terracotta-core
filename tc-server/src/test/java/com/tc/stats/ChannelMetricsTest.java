/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
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
package com.tc.stats;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.spi.metric.MetricService;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for ChannelMetrics.
 */
public class ChannelMetricsTest {

  // --- constructor ---

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullMetricService() {
    new ChannelMetrics(null);
  }

  // --- channelCreated ---

  @Test
  public void testChannelCreatedFiresConnectionOpenEvent() {
    MetricService metricService = mock(MetricService.class);
    ChannelMetrics metrics = new ChannelMetrics(metricService);

    metrics.channelCreated(mock(MessageChannel.class));

    verify(metricService, times(1)).fireEvent(MetricService.Event.CONNECTION_OPEN);
    verifyNoMoreInteractions(metricService);
  }

  @Test
  public void testChannelCreatedFiresOncePerCall() {
    MetricService metricService = mock(MetricService.class);
    ChannelMetrics metrics = new ChannelMetrics(metricService);
    MessageChannel channel = mock(MessageChannel.class);

    metrics.channelCreated(channel);
    metrics.channelCreated(channel);
    metrics.channelCreated(channel);

    verify(metricService, times(3)).fireEvent(MetricService.Event.CONNECTION_OPEN);
  }

  // --- channelRemoved ---

  @Test
  public void testChannelRemovedFiresConnectionCloseEvent() {
    MetricService metricService = mock(MetricService.class);
    ChannelMetrics metrics = new ChannelMetrics(metricService);

    metrics.channelRemoved(mock(MessageChannel.class));

    verify(metricService, times(1)).fireEvent(MetricService.Event.CONNECTION_CLOSE);
    verifyNoMoreInteractions(metricService);
  }

  @Test
  public void testChannelRemovedFiresOncePerCall() {
    MetricService metricService = mock(MetricService.class);
    ChannelMetrics metrics = new ChannelMetrics(metricService);
    MessageChannel channel = mock(MessageChannel.class);

    metrics.channelRemoved(channel);
    metrics.channelRemoved(channel);

    verify(metricService, times(2)).fireEvent(MetricService.Event.CONNECTION_CLOSE);
  }

  // --- no cross-contamination between events ---

  @Test
  public void testCreateAndRemoveFirIndependentEvents() {
    MetricService metricService = mock(MetricService.class);
    ChannelMetrics metrics = new ChannelMetrics(metricService);

    metrics.channelCreated(mock(MessageChannel.class));
    metrics.channelRemoved(mock(MessageChannel.class));

    verify(metricService, times(1)).fireEvent(MetricService.Event.CONNECTION_OPEN);
    verify(metricService, times(1)).fireEvent(MetricService.Event.CONNECTION_CLOSE);
    verifyNoMoreInteractions(metricService);
  }

  // --- NOOP service does not throw ---

  @Test
  public void testChannelCreatedWithNoopMetricServiceDoesNotThrow() {
    ChannelMetrics metrics = new ChannelMetrics(MetricService.NOOP);
    metrics.channelCreated(mock(MessageChannel.class));
  }

  @Test
  public void testChannelRemovedWithNoopMetricServiceDoesNotThrow() {
    ChannelMetrics metrics = new ChannelMetrics(MetricService.NOOP);
    metrics.channelRemoved(mock(MessageChannel.class));
  }
}

// Made with Bob
