package com.middleware.backend.kaotocamel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SequenceFixService {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void fixSequencesOnStartup() {
         fixIntegrationMappingSequence();
    }

    private void fixIntegrationMappingSequence() {
        try {
            log.info("Fixing sequence for integration_mapping.id ...");

            Long maxId = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM integration_mapping",
                    Long.class
            );
            if (maxId == null) maxId = 0L;

            String seqName = jdbc.queryForObject(
                    "SELECT pg_get_serial_sequence('integration_mapping', 'id')",
                    String.class
            );

            if (seqName == null || seqName.isBlank()) {
                seqName = "public.integration_mapping_id_seq";
                log.warn("pg_get_serial_sequence returned NULL. Falling back to {}", seqName);
            } else if (!seqName.contains(".")) {
                 seqName = "public." + seqName;
            }

             Long effective;
            if (maxId == 0L) {
                // جدول فاضي → أول nextval() = 1
                effective = jdbc.queryForObject(
                        "SELECT setval(?::regclass, 1, false)",
                        Long.class,
                        seqName
                );
                log.info("Sequence {} initialized. setval returned {}", seqName, effective);
            } else {
                 effective = jdbc.queryForObject(
                        "SELECT setval(?::regclass, ?, true)",
                        Long.class,
                        seqName, maxId
                );
                log.info("Sequence {} synced to {}. setval returned {}", seqName, maxId, effective);
            }

            log.info("Sequence fix done.");

        } catch (Exception e) {
            log.error("Sequence fix failed in primary path: {}", e.getMessage(), e);

             try {
                Long maxId = jdbc.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM integration_mapping",
                        Long.class
                );
                if (maxId == null) maxId = 0L;

                jdbc.execute(
                        "DO $$ BEGIN " +
                                "IF NOT EXISTS ( " +
                                "  SELECT 1 FROM pg_class c " +
                                "  JOIN pg_namespace n ON n.oid = c.relnamespace " +
                                "  WHERE c.relkind = 'S' AND n.nspname = 'public' AND c.relname = 'integration_mapping_id_seq' " +
                                ") THEN " +
                                "  CREATE SEQUENCE public.integration_mapping_id_seq " +
                                "    START WITH " + Math.max(1, maxId + 1) + " INCREMENT BY 1; " +
                                "  ALTER TABLE public.integration_mapping " +
                                "    ALTER COLUMN id SET DEFAULT nextval('public.integration_mapping_id_seq'); " +
                                "  ALTER SEQUENCE public.integration_mapping_id_seq " +
                                "    OWNED BY public.integration_mapping.id; " +
                                "END IF; " +
                                "END $$;"
                );

                 if (maxId == 0L) {
                    Long v = jdbc.queryForObject(
                            "SELECT setval('public.integration_mapping_id_seq'::regclass, 1, false)",
                            Long.class
                    );
                    log.info("Fallback: sequence initialized to 1. setval returned {}", v);
                } else {
                    Long v = jdbc.queryForObject(
                            "SELECT setval('public.integration_mapping_id_seq'::regclass, ?, true)",
                            Long.class,
                            maxId
                    );
                    log.info("Fallback: sequence synced to {}. setval returned {}", maxId, v);
                }

            } catch (Exception ex) {
                log.error("Alternative fallback failed: {}", ex.getMessage(), ex);
            }
        }
    }
}
