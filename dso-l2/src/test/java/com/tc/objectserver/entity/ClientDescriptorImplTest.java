package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.hamcrest.Matchers.is;

public class ClientDescriptorImplTest {


  @Test
  public void testSerialization() throws IOException, ClassNotFoundException {

    {
      ClientDescriptorImpl cd1 = new ClientDescriptorImpl(new ClientID(10), new ClientInstanceID(11));
      ClientDescriptorImpl cd2 = (ClientDescriptorImpl) serializeDeserialize(cd1);
      Assert.assertThat(cd1, is(cd2));
      Assert.assertThat(cd1.isValid(), is(true));
    }
    {
      ClientDescriptorImpl cd1 = new ClientDescriptorImpl();
      ClientDescriptorImpl cd2 = (ClientDescriptorImpl) serializeDeserialize(cd1);
      Assert.assertThat(cd1, is(cd2));
      Assert.assertThat(cd1.isValid(), is(false));
    }
    {
      ClientDescriptorImpl cd1 = new ClientDescriptorImpl(new ClientID(-1), new ClientInstanceID(-1));
      ClientDescriptorImpl cd2 = (ClientDescriptorImpl) serializeDeserialize(cd1);
      Assert.assertThat(cd1, is(cd2));
      Assert.assertThat(cd1.isValid(), is(false));
    }
  }

  private static Object serializeDeserialize(ClientDescriptorImpl cd1) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos=new ByteArrayOutputStream();
    ObjectOutputStream oos=new ObjectOutputStream(baos);
    oos.writeUnshared(cd1);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    Object tmp= ois.readObject();
    return tmp;
  }
}