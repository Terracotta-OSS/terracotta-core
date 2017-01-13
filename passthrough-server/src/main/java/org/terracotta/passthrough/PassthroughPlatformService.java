package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.terracotta.monitoring.PlatformService;

/**
 * @author vmad
 */
public class PassthroughPlatformService implements PlatformService {

    private final PassthroughDumper passthroughDumper;

    public PassthroughPlatformService(PassthroughDumper passthroughDumper) {
        this.passthroughDumper = passthroughDumper;
    }

    @Override
    public void dumpPlatformState() {
        passthroughDumper.dump();
    }

    @Override
    public InputStream getPlatformConfiguration() {
      return new ByteArrayInputStream(new byte[0]);
    }
    
    
}
