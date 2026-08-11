package com.middleware.backend.integrated_systems.model;

/**
 * Enum representing supported communication protocols
 * for an integrated system.
 * <p>
 * Kept separate from {@link SystemType}: a system has one type
 * (API, database, broker, …) and one protocol (HTTP, PostgreSQL, …).
 */
public enum Protocol {

    /** HTTP protocol */
    HTTP,

    /** HTTPS protocol (secure HTTP) */
    HTTPS,

    /** PostgreSQL database protocol */
    POSTGRESQL,

    /** MySQL database protocol */
    MYSQL,

    /** Microsoft SQL Server protocol */
    SQLSERVER,

    /** Oracle Database protocol */
    ORACLE,

    /** SMTP email protocol */
    SMTP,

    /** FTP file transfer protocol */
    FTP,

    /** SFTP file transfer protocol */
    SFTP,

    /** MQTT messaging protocol */
    MQTT,

    /** AMQP messaging protocol */
    AMQP;

    /**
     * Default TCP port when the integrated system does not specify one.
     *
     * @return conventional default port for this protocol
     */
    public int defaultPort() {
        return switch (this) {
            case HTTP -> 80;
            case HTTPS -> 443;
            case POSTGRESQL -> 5432;
            case MYSQL -> 3306;
            case SQLSERVER -> 1433;
            case ORACLE -> 1521;
            case SMTP -> 587;
            case FTP -> 21;
            case SFTP -> 22;
            case MQTT -> 1883;
            case AMQP -> 5672;
        };
    }

    /**
     * URL scheme used when building connection URLs (e.g. {@code https}).
     * HTTP/HTTPS keep producing schemes that existing API clients understand.
     *
     * @return lowercase URL scheme
     */
    public String toUrlScheme() {
        return switch (this) {
            case HTTP -> "http";
            case HTTPS -> "https";
            default -> name().toLowerCase();
        };
    }
}
