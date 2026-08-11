package com.middleware.backend.integrated_systems.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * High-level category of an integrated system.
 * <p>
 * {@link Protocol} remains the wire/transport protocol; {@code SystemType}
 * describes what kind of system is being integrated.
 */
public enum SystemType {

    /** HTTP/HTTPS APIs (existing integrated systems) */
    HTTP_API,

    /** Relational or similar database systems */
    DATABASE,

    /** Messaging / broker systems */
    MESSAGE_BROKER,

    /** Email systems */
    EMAIL,

    /** File-transfer systems */
    FILE_TRANSFER;

    /**
     * Protocols that are valid for this system type.
     *
     * @return an unmodifiable set of compatible protocols
     */
    public Set<Protocol> compatibleProtocols() {
        return switch (this) {
            case HTTP_API -> EnumSet.of(Protocol.HTTP, Protocol.HTTPS);
            case DATABASE -> EnumSet.of(Protocol.POSTGRESQL, Protocol.MYSQL, Protocol.SQLSERVER, Protocol.ORACLE);
            case MESSAGE_BROKER -> EnumSet.of(Protocol.MQTT, Protocol.AMQP);
            case EMAIL -> EnumSet.of(Protocol.SMTP);
            case FILE_TRANSFER -> EnumSet.of(Protocol.FTP, Protocol.SFTP);
        };
    }

    /**
     * @param protocol protocol to test
     * @return true if {@code protocol} is valid for this system type
     */
    public boolean supports(Protocol protocol) {
        return protocol != null && compatibleProtocols().contains(protocol);
    }

    /**
     * Infers a system type from a protocol so existing HTTP/HTTPS records
     * (and clients that omit {@code systemType}) stay valid.
     *
     * @param protocol the protocol, may be null
     * @return inferred type; {@link #HTTP_API} when unknown or null
     */
    public static SystemType fromProtocol(Protocol protocol) {
        if (protocol == null) {
            return HTTP_API;
        }
        for (SystemType type : values()) {
            if (type.supports(protocol)) {
                return type;
            }
        }
        return HTTP_API;
    }
}
