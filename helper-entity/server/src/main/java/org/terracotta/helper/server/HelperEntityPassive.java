package org.terracotta.helper.server;

import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.helper.common.HelperEntityMessage;
import org.terracotta.helper.common.HelperEntityResponse;

public class HelperEntityPassive implements PassiveServerEntity<HelperEntityMessage, HelperEntityResponse> {
  @Override
  public void invoke(final HelperEntityMessage helperEntityMessage) throws EntityUserException {
    
  }

  @Override
  public void startSyncEntity() {

  }

  @Override
  public void endSyncEntity() {

  }

  @Override
  public void startSyncConcurrencyKey(final int i) {

  }

  @Override
  public void endSyncConcurrencyKey(final int i) {

  }

  @Override
  public void createNew() throws ConfigurationException {

  }

  @Override
  public void destroy() {

  }
}
