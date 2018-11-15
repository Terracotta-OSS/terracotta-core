package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.terracotta.monitoring.PlatformService;

/**
 * @author vmad
 */
public class PassthroughPlatformService implements PlatformService {

    private final PassthroughServer passthroughServer;

    public PassthroughPlatformService(PassthroughServer passthroughServer) {
        this.passthroughServer = passthroughServer;
    }

    @Override
    public void dumpPlatformState() {
        passthroughServer.dump();
    }

    @Override
    public void stopPlatform() {
        passthroughServer.stop();
    }

    @Override
    public InputStream getPlatformConfiguration() {
      return new ByteArrayInputStream(new byte[0]);
    }

  @Override
  public void fatalError(String string) {
    passthroughServer.stop();
  }

  @Override
  public long uptime() {
    return 0L;
  }
    
    
}
