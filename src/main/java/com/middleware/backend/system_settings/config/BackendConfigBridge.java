//package com.middleware.backend.system_settings.config;
//
//import com.middleware.model.ConfigDetail;
//import com.middleware.service.ConfigService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Map;
//
//@Slf4j
//@Configuration
//public class BackendConfigBridge {
//
//    /**
//     * Service from the backend module used to fetch system configuration.
//     */
//    private final com.middleware.backend.system_settings.service.ConfigService backendConfigService;
//
//    /**
//     * Constructor for injecting the backend ConfigService.
//     *
//     * @param backendConfigService the backend system configuration service
//     */
//    public BackendConfigBridge(com.middleware.backend.system_settings.service.ConfigService backendConfigService) {
//        this.backendConfigService = backendConfigService;
//    }
//
//    /**
//     * Exposes a bridge ConfigService bean to be used in the application.
//     * This bean adapts the backend ConfigService to the expected interface.
//     *
//     * @return a ConfigService implementation that fetches configurations from the backend service
//     */
//    @Bean(name = "configServiceBridge")
//    public ConfigService configServiceImplementation() {
//        return new ConfigService() {
//
//            /**
//             * Retrieves configuration details for a given code.
//             * Fetches data from the backend ConfigService and maps it to ConfigDetail.
//             * Logs warnings if no configuration is found.
//             *
//             * @param code the configuration code
//             * @return a ConfigDetail containing the code and configurations, or null if not found
//             */
//            @Override
//            public ConfigDetail getConfig(String code) {
//                try {
//                    Map<String, String> backendConfigs = backendConfigService.getModuleConfig(code);
//                    if (backendConfigs == null || backendConfigs.isEmpty()) {
//                        log.warn("No config entries found for code: {}", code);
//                        return null;
//                    }
//
//                    ConfigDetail detail = new ConfigDetail();
//                    detail.setCode(code);
//                    detail.setConfigs(backendConfigs);
//                    log.info("Returning config for code={}: {}", code, backendConfigs);
//                    return detail;
//                } catch (RuntimeException e) {
//                    log.warn("No config found for code: {}", code);
//                    return null; // so the processor can handle it and return 404
//                }
//            }
//        };
//    }
//
//}
//
