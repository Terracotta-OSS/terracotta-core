package com.tc.services;

import com.tc.management.beans.TCDumper;
import org.terracotta.monitoring.PlatformService;

/**
 * @author vmad
 */
public class PlatformServiceImpl implements PlatformService {

    private final TCDumper tcDumper;

    public PlatformServiceImpl(TCDumper tcDumper) {
        this.tcDumper = tcDumper;
    }

    @Override
    public void dumpPlatformState() {
        this.tcDumper.dump();
    }
}
