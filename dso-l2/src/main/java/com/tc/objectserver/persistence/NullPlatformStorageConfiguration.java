package com.tc.objectserver.persistence;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.persistence.IPersistentStorage;

/**
 * @author vmad
 */
public class NullPlatformStorageConfiguration implements ServiceConfiguration<IPersistentStorage> {
    @Override
    public Class<IPersistentStorage> getServiceType() {
        return IPersistentStorage.class;
    }
}
