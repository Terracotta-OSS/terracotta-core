package com.tc.server.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ServerStatTest {

  @Test
  public void decodeJsonAndSetFieldsTest() {
    ServerStat stat = new ServerStat("localhost", null);
    stat.decodeJsonAndSetFields("{ \"health\" : \"OK\", \"role\" : \"ACTIVE\", \"state\": \"ACTIVE-COORDINATOR\", \"managementPort\" : \"9540\", \"serverGroupName\" : \"defaultGroup\"}");
    assertThat(stat.getHealth(), is("OK"));
    assertThat(stat.getRole(), is("ACTIVE"));
    assertThat(stat.getState(), is("ACTIVE-COORDINATOR"));
    assertThat(stat.getGroupName(), is("defaultGroup"));
  }

}