package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;

/**
 * @author tim
 */
public class ObjectIDSerializer extends AbstractIdentifierSerializer<ObjectID> {
  public static final ObjectIDSerializer INSTANCE = new ObjectIDSerializer();

  public ObjectIDSerializer() {
    super(ObjectID.class);
  }

  @Override
  protected ObjectID createIdentifier(final long id) {
    return new ObjectID(id);
  }
}

