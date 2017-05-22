package com.tc.exception;

import org.terracotta.entity.EntityUserException;
import org.terracotta.exception.EntityException;

public class VoltronEntityUserExceptionWrapper extends EntityException {

  public VoltronEntityUserExceptionWrapper(EntityUserException cause) {
    super(null, null, cause.getMessage(), cause);
  }
}
