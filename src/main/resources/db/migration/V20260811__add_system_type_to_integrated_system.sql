-- Add system_type to integrated_system and default existing HTTP/HTTPS rows to HTTP_API.
-- Safe to run on databases that already have the table (Hibernate ddl-auto=update may
-- also add the column; this script still backfills nulls).

ALTER TABLE public.integrated_system
    ADD COLUMN IF NOT EXISTS system_type varchar(50);

UPDATE public.integrated_system
SET system_type = 'HTTP_API'
WHERE system_type IS NULL;

ALTER TABLE public.integrated_system
    ALTER COLUMN system_type SET DEFAULT 'HTTP_API';

-- Existing DBs have CHECK (protocol IN ('HTTP','HTTPS')) named integrated_system_protocol_check.
-- Expand it so DATABASE / EMAIL / FILE_TRANSFER / MESSAGE_BROKER protocols can be stored.
ALTER TABLE public.integrated_system
    DROP CONSTRAINT IF EXISTS integrated_system_protocol_check;

ALTER TABLE public.integrated_system
    ADD CONSTRAINT integrated_system_protocol_check
    CHECK (protocol IS NULL OR protocol IN (
        'HTTP', 'HTTPS',
        'POSTGRESQL', 'MYSQL', 'SQLSERVER', 'ORACLE',
        'SMTP', 'FTP', 'SFTP', 'MQTT', 'AMQP'
    ));
