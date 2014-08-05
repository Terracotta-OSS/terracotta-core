/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import org.terracotta.license.util.Base64;

import com.google.common.collect.MapMaker;
import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.license.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClientFactory;
import com.tc.util.ProductInfo;
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

  private static TCLogger           logger = TCLogging.getLogger(CreateClient.class);

  private final String              embeddedTcConfig;
  private final boolean             isURLConfig;
  private final String              productIdName;
  private final ClassLoader         loader;
  private final boolean             rejoin;

  private final SecurityInfo        securityInfo;
  private final Map<String, Object> env;

  public CreateClient(String embeddedTcConfig, boolean isURLConfig, ClassLoader loader, boolean rejoin,
                      String productIdName, Map<String, Object> env) {
    this.embeddedTcConfig = embeddedTcConfig;
    this.isURLConfig = isURLConfig;
    this.productIdName = productIdName;
    String username = null;
    if (isURLConfig) {
      username = URLConfigUtil.getUsername(embeddedTcConfig);
    }
    this.securityInfo = new SecurityInfo(username != null, username);
    this.loader = loader;
    this.rejoin = rejoin;
    this.env = env;
  }

  @Override
  public ClientCreatorCallable call() throws Exception {
    TCSecurityManager securityManager = null;

    if (securityInfo.isSecure()) {
      if (!ProductInfo.getInstance().isEnterprise()) { throw new RuntimeException(
                                                                                  "You're trying to setup a secured environment, which requires a EE version of Terracotta"); }
      logger.info("Secured environment! Enabling SSL & will be authenticating as user '" + securityInfo.getUsername()
                  + "'");
      securityManager = DistributedObjectClientFactory.createSecurityManager(env);
    }

    String configSpec = embeddedTcConfig;
    if (!isURLConfig) {
      // convert to base64 string configuration source
      configSpec = "base64://"
                   + Base64.encodeBytes(embeddedTcConfig.getBytes("UTF-8"), Base64.GZIP | Base64.DONT_BREAK_LINES);
    }

    ProductID productId = productIdName == null ? ProductID.USER : ProductID.valueOf(productIdName);
    AbortableOperationManager abortableOperationManager = new AbortableOperationManagerImpl();
    UUID uuid = UUID.getUUID();
    final DistributedObjectClientFactory distributedObjectClientFactory = new DistributedObjectClientFactory(
                                                                                                             configSpec,
                                                                                                             securityManager,
                                                                                                             securityInfo,
                                                                                                             loader,
                                                                                                             rejoin,
                                                                                                             productId,
                                                                                                             abortableOperationManager,
                                                                                                             uuid);
    return new ClientCreatorCallableImpl(distributedObjectClientFactory, abortableOperationManager, uuid);
  }

  public static class ClientCreatorCallableImpl implements ClientCreatorCallable {

    private final DistributedObjectClientFactory distributedObjectClientFactory;
    private final AbortableOperationManager      abortableOperationManager;
    private final UUID                           uuid;

    public ClientCreatorCallableImpl(DistributedObjectClientFactory distributedObjectClientFactory,
                                     AbortableOperationManager abortableOperationManager, UUID uuid) {
      this.distributedObjectClientFactory = distributedObjectClientFactory;
      this.abortableOperationManager = abortableOperationManager;
      this.uuid = uuid;
    }

    @Override
    public Object call() throws Exception {
      return distributedObjectClientFactory.create();
    }

    @Override
    public Object getAbortableOperationManager() {
      return abortableOperationManager;
    }

    @Override
    public String getUuid() {
      return uuid.toString();
    }

  }

}
