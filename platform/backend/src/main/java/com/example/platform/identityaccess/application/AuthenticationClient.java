package com.example.platform.identityaccess.application;

import com.fasterxml.jackson.annotation.JsonCreator;

public record AuthenticationClient(String clientId, ClientType clientType) {

    private static final String DEFAULT_CLIENT_ID = "web";

    public AuthenticationClient {
        clientId = normalizeClientId(clientId);
        clientType = clientType == null ? ClientType.WEB : clientType;
    }

    public static AuthenticationClient web() {
        return new AuthenticationClient(DEFAULT_CLIENT_ID, ClientType.WEB);
    }

    private static String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return DEFAULT_CLIENT_ID;
        }
        return clientId.trim();
    }

    public enum ClientType {
        WEB,
        MOBILE,
        FHIR,
        SERVICE;

        @JsonCreator
        public static ClientType from(String value) {
            if (value == null || value.isBlank()) {
                return WEB;
            }
            return ClientType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        }
    }
}
