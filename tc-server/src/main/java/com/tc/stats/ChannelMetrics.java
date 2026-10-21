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

import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.spi.metric.MetricService;

import static java.util.Objects.requireNonNull;

/**
 * Translates {@link com.tc.net.protocol.tcm.ChannelManagerEventListener} callbacks into
 * {@link MetricService} events.
 *
 * <p>Registered as a listener on each server-side {@link com.tc.net.protocol.tcm.ChannelManager}
 * (L1 client, diagnostic, and inbound peer-server), it fires
 * {@link MetricService.Event#CONNECTION_OPEN} when an inbound physical transport connection is
 * established and {@link MetricService.Event#CONNECTION_CLOSE} when one is torn down.
 *
 * <p>Outbound peer-server connections opened by {@code TCGroupManagerImpl.openChannel()} are not
 * handled here — they fire the same events directly via a {@code ChannelEventListener} on the
 * {@code ClientMessageChannel}.  Together the two mechanisms ensure every physical connection,
 * regardless of direction, is reflected in the {@link MetricService.Metric#CONNECTIONS} gauge and
 * the {@code CONNECTION_OPEN}/{@code CONNECTION_CLOSE} meters.
 */
public class ChannelMetrics implements ChannelManagerEventListener {
  private final MetricService metricService;

  /**
   * Creates a new listener that routes channel events to {@code metricService}.
   *
   * @param metricService the service to fire events on (must not be {@code null})
   */
  public ChannelMetrics(MetricService metricService) {
    this.metricService = requireNonNull(metricService);
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    metricService.fireEvent(MetricService.Event.CONNECTION_OPEN);
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    metricService.fireEvent(MetricService.Event.CONNECTION_CLOSE);
  }
}
