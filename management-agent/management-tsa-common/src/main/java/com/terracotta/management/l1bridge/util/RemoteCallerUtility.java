package com.terracotta.management.l1bridge.util;

import com.terracotta.management.resource.services.utils.ClientEntityV1Bridge;
import com.terracotta.management.service.ClientManagementServiceV1Adapter;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by SUND on 20-01-2015.
 */
public class RemoteCallerUtility {

    private final static String CLIENT_UUID_KEY = "ClientUUID";

    public Set<String> fetchClientUUIDs() throws ServiceExecutionException{
        Set<String> clientUUIDs = new HashSet<String>();
        ClientManagementServiceV1Adapter clientManagementServiceV1Adapter = ServiceLocator.locate(ClientManagementServiceV1Adapter.class);
        if(clientManagementServiceV1Adapter != null) {
            Collection<ClientEntityV1Bridge> clientEntities = clientManagementServiceV1Adapter.getClientsV1(null, null);
            if (clientEntities != null) {
                for (ClientEntityV1Bridge clientEntity : clientEntities) {
                    if (clientEntity != null) {
                        Map<String, Object> attributes = clientEntity.getAttributes();
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