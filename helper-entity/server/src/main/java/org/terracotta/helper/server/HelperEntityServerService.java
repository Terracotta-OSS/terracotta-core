package org.terracotta.helper.server;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.helper.common.HelperEntityCodec;
import org.terracotta.helper.common.HelperEntityConstants;
import org.terracotta.helper.common.HelperEntityMessage;
import org.terracotta.helper.common.HelperEntityResponse;
import org.terracotta.monitoring.PlatformService;

import com.tc.classloader.PermanentEntity;

@PermanentEntity(type = HelperEntityConstants.HELPER_ENTITY_CLASS_NAME, names={ HelperEntityConstants.HELPER_ENTITY_NAME}, version = HelperEntityConstants.HELPER_ENTITY_VERSION)
public class HelperEntityServerService implements EntityServerService<HelperEntityMessage, HelperEntityResponse> {
  @Override
  public long getVersion() {
    return HelperEntityConstants.HELPER_ENTITY_VERSION;
  }

  @Override
  public boolean handlesEntityType(final String s) {
    return HelperEntityConstants.HELPER_ENTITY_CLASS_NAME.equals(s);
  }

  @Override
  public ActiveServerEntity<HelperEntityMessage, HelperEntityResponse> createActiveEntity(final ServiceRegistry serviceRegistry, final byte[] bytes) throws ConfigurationException {
    try {
      ClientCommunicator clientCommunicator = serviceRegistry.getService(new BasicServiceConfiguration<ClientCommunicator>(ClientCommunicator.class));
      PlatformService platformService = serviceRegistry.getService(new BasicServiceConfiguration<>(PlatformService.class));
      return new HelperEntityActive(clientCommunicator, platformService);
    } catch (ServiceException e) {
      throw new RuntimeException("Couldn't fetch service", e);
    }
  }

  @Override
  public PassiveServerEntity<HelperEntityMessage, HelperEntityResponse> createPassiveEntity(final ServiceRegistry serviceRegistry, final byte[] bytes) throws ConfigurationException {
    return new HelperEntityPassive();
  }

  @Override
  public ConcurrencyStrategy<HelperEntityMessage> getConcurrencyStrategy(final byte[] bytes) {
    return new NoConcurrencyStrategy<HelperEntityMessage>();
  }

  @Override
  public MessageCodec<HelperEntityMessage, HelperEntityResponse> getMessageCodec() {
    return new HelperEntityCodec();
  }

  @Override
  public SyncMessageCodec<HelperEntityMessage> getSyncMessageCodec() {
    return null;
  }
}
