package org.terracotta.helper.client;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.helper.common.HelperEntityCodec;
import org.terracotta.helper.common.HelperEntityMessage;
import org.terracotta.helper.common.HelperEntityResponse;

public class HelperEntityClientService implements EntityClientService<HelperEntity, HelperEntityConfig, HelperEntityMessage, HelperEntityResponse, Object>{
  @Override
  public boolean handlesEntityType(final Class<HelperEntity> aClass) {
    return HelperEntity.class.equals(aClass);
  }

  @Override
  public byte[] serializeConfiguration(final HelperEntityConfig helperEntityConfig) {
    return new byte[0];
  }

  @Override
  public HelperEntityConfig deserializeConfiguration(final byte[] bytes) {
    return new HelperEntityConfig();
  }

  @Override
  public HelperEntity create(final EntityClientEndpoint<HelperEntityMessage, HelperEntityResponse> entityClientEndpoint, final Object o) {
    return new HelperEntity(entityClientEndpoint);
  }

  @Override
  public MessageCodec<HelperEntityMessage, HelperEntityResponse> getMessageCodec() {
    return new HelperEntityCodec();
  }
}
