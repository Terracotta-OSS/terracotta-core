package com.terracotta.management.l1bridge.util;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.service.impl.ClientManagementServiceV2;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by SUND on 20-01-2015.
 */
public class RemoteCallerV2Util extends RemoteCallerUtility{

    private final static String CLIENT_UUID_KEY = "ClientUUID";

    public Set<String> fetchClientUUIDs() throws ServiceExecutionException {
        Set<String> clientUUIDs = new HashSet<String>();
        ClientManagementServiceV2 clientManagementServiceV2 = ServiceLocator.locate(ClientManagementServiceV2.class);
        if (clientManagementServiceV2 != null) {
            ResponseEntityV2<ClientEntityV2> responseEntityV2 = clientManagementServiceV2.getClients(null, null);
            if (responseEntityV2 != null) {
                Collection<ClientEntityV2> clientEntityV2Collection = responseEntityV2.getEntities();
                for (ClientEntityV2 clientEntityV2 : clientEntityV2Collection) {
                    if (clientEntityV2 != null) {
                        Map<String, Object> attributes = clientEntityV2.getAttributes();
                        if (attributes != null && attributes.containsKey(CLIENT_UUID_KEY)) {
                            clientUUIDs.add((String) attributes.get(CLIENT_UUID_KEY));
                        }
                    }
                }
            }
        }
        return clientUUIDs;
    }

}
