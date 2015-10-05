/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.google.common.collect.MapMaker;
import com.tc.license.ProductID;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.util.UUID;

import java.util.Map;
import java.util.concurrent.Callable;


public class CreateClient implements Callable<ClientCreatorCallable> {

  static {
    /*
     * Make sure the google-collections finalizer thread is not in the TC thread group otherwise we will pin the L1
     * loader in memory since the TCThreadGroup will have been loaded by the L1Loader and hence will provide a strong
     * ref from the finalizer thread to the loader.
     */
    Map<String, Object> dummy = new MapMaker().weakValues().makeMap();
    dummy.put("dummy", new Object());
  }

  private final String              embeddedTcConfig;
  private final String              productIdName;

  private final SecurityInfo        securityInfo;

  public CreateClient(String embeddedTcConfig, String productIdName) {
    this.embeddedTcConfig = embeddedTcConfig;
    this.productIdName = productIdName;
    String username = URLConfigUtil.getUsername(embeddedTcConfig);
    this.securityInfo = new SecurityInfo(username != null, username);
  }

  @Override
  public ClientCreatorCallable call() throws Exception {
    TCSecurityManager securityManager = null;

    ProductID productId = productIdName == null ? ProductID.USER : ProductID.valueOf(productIdName);
    UUID uuid = UUID.getUUID();
    final DistributedObjectClientFactory distributedObjectClientFactory = new DistributedObjectClientFactory(embeddedTcConfig,
                                                                                                             securityManager,
                                                                                                             securityInfo,
                                                                                                             productId,
        uuid);
    return new ClientCreatorCallableImpl(distributedObjectClientFactory, uuid);
  }

  public static class ClientCreatorCallableImpl implements ClientCreatorCallable {

    private final DistributedObjectClientFactory distributedObjectClientFactory;
    private final UUID                           uuid;

    public ClientCreatorCallableImpl(DistributedObjectClientFactory distributedObjectClientFactory,
                                     UUID uuid) {
      this.distributedObjectClientFactory = distributedObjectClientFactory;
      this.uuid = uuid;
    }

    @Override
    public Object call() throws Exception {
      return distributedObjectClientFactory.create();
    }

    @Override
    public String getUuid() {
      return uuid.toString();
    }

  }

}
