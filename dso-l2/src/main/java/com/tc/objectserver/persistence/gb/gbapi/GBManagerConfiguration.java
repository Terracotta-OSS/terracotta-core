package com.tc.objectserver.persistence.gb.gbapi;

import java.util.Collection;
import java.util.Map;

/**
 * @author tim
 */
public interface GBManagerConfiguration {

  Collection<Object> sharedConfig();

  Map<String, GBMapConfig<?, ?>> mapConfig();

}
