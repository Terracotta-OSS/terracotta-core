package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;

import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author tim
 */
public class TCCollectionsSerializerImplTest extends TestCase {

  private TCCollectionsSerializer tcCollectionsSerializer;

  @Override
  public void setUp() throws Exception {
    tcCollectionsSerializer = new TCCollectionsSerializerImpl();
  }

  public void testObjectID() throws Exception {
    byte[] serialized = tcCollectionsSerializer.serialize(new ObjectID(1));
    assertThat((ObjectID) tcCollectionsSerializer.deserialize(serialized), is(new ObjectID(1)));
  }

  public void testLong() throws Exception {
    byte[] serialized = tcCollectionsSerializer.serialize(11L);
    assertThat((Long) tcCollectionsSerializer.deserialize(serialized), is(11L));
  }
}
