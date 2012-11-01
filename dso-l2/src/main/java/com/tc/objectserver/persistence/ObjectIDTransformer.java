package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;

/**
 * @author tim
 */
public class ObjectIDTransformer extends AbstractIdentifierTransformer<ObjectID> {
  public static final ObjectIDTransformer INSTANCE = new ObjectIDTransformer();

  public ObjectIDTransformer() {
    super(ObjectID.class);
  }

  @Override
  protected ObjectID createIdentifier(final long id) {
    return new ObjectID(id);
  }
}

