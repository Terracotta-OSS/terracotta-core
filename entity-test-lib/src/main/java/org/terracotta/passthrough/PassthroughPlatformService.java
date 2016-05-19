package org.terracotta.passthrough;

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
}
