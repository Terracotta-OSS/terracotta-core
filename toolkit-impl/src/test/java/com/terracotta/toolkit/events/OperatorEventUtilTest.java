package com.terracotta.toolkit.events;

import com.tc.platform.PlatformService;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;

import static com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import static com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import static com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import static com.terracotta.toolkit.events.OperatorEventUtil.DELIMITER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OperatorEventUtilTest {

  private static PlatformService platformService;
  @Before
  public void setUp() throws Exception {
    platformService = mock(PlatformService.class);
  }

  @Test
  public void testFireOperatorEvent() throws Exception {
    final String TEST_MESSAGE = "test message";
    final String NORMAL_APPLICATION_NAME = "applicationName";
    final String EXPECTED_TEST_MESSAGE = NORMAL_APPLICATION_NAME + ": " + TEST_MESSAGE;

    OperatorEventUtil.fireOperatorEvent(platformService, OperatorEventLevel.INFO, NORMAL_APPLICATION_NAME, TEST_MESSAGE);
    verify(platformService).fireOperatorEvent(EventLevel.INFO, EventSubsystem.APPLICATION, EventType.APPLICATION_USER_DEFINED, EXPECTED_TEST_MESSAGE);
  }

  @Test
  public void testFireOperatorEventWithSpecialApplicationName() throws Exception {
    final String TEST_MESSAGE = "test message";
    final String SPECIAL_APPLICATION_NAME = EventSubsystem.WAN + DELIMITER + EventType.WAN_REPLICA_CONNECTED;

    OperatorEventUtil.fireOperatorEvent(platformService, OperatorEventLevel.INFO, SPECIAL_APPLICATION_NAME, TEST_MESSAGE);
    verify(platformService).fireOperatorEvent(EventLevel.INFO, EventSubsystem.WAN, EventType.WAN_REPLICA_CONNECTED, TEST_MESSAGE);
  }
}