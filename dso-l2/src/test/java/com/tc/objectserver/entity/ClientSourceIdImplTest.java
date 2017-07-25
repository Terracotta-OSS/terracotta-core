package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.entity.ClientSourceId;

import java.io.IOException;

import static org.hamcrest.Matchers.is;

public class ClientSourceIdImplTest {

  @Test
  public void testDescriptorMatching() throws Exception {
    ClientSourceIdImpl cs = new ClientSourceIdImpl(10);
    ClientDescriptorImpl cd1 = new ClientDescriptorImpl(new ClientID(10), new ClientInstanceID(100));
    ClientDescriptorImpl cd2 = new ClientDescriptorImpl(new ClientID(11), new ClientInstanceID(100));
    Assert.assertThat(cs.matches(cd1), is(true));
    Assert.assertThat(cs.matches(cd2), is(false));
  }

  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {

    {
      ClientSourceIdImpl cs1 = new ClientSourceIdImpl(10);
      ClientSourceId cs2 = serializeDeserialize(cs1);
      Assert.assertThat(cs1, is(cs2));
    }
    {
      ClientSourceIdImpl cs1 = new ClientSourceIdImpl();
      ClientSourceId cs2 = serializeDeserialize(cs1);
      Assert.assertThat(cs1, is(cs2));
    }
    {
      ClientSourceIdImpl cs1 = new ClientSourceIdImpl(-10);
      ClientSourceId cs2 = serializeDeserialize(cs1);
      Assert.assertThat(cs1, is(cs2));
    }
  }

  private static ClientSourceId serializeDeserialize(ClientSourceId cs1) throws IOException, ClassNotFoundException {
    return new ClientSourceIdImpl(cs1.toLong());
  }
}