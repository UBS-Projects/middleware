package com.middleware;

import java.util.Map;

import com.middleware.service.IntegrationMappingService;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel component for Integration Mapping.
 *
 * <p>Scheme: <b>integrationmapping</b></p>
 * <p>Producer-only component that routes requests to a middleware “mapping” by name.
 * The mapping name is provided as a {@code @UriParam("mapping")} on the endpoint,
 * exactly like the Notification component style in this project.</p>
 *
 * <p>Usage from YAML/Kaoto:</p>
 * <pre>
 * - to:
 *     uri: integrationmapping
 *     parameters:
 *       mapping: healthmap
 * </pre>
 */
@Component("integrationmapping")
public class IntegrationMappingComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationMappingComponent.class);

    @Metadata(label = "internal",description = "Integration Mapping Service for handling operations")
    private IntegrationMappingService integrationMappingService;

    public IntegrationMappingComponent() {
        LOG.info("IntegrationMappingComponent initialized.");
    }

    public IntegrationMappingComponent(CamelContext context) {
        super(context);
        LOG.info("IntegrationMappingComponent initialized with CamelContext.");
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        // ✅ إذا المستخدم كتب فقط integrationmapping بدون أي path
        if (remaining == null || remaining.trim().isEmpty()) {

            // نحاول نقرأ parameter اسمه operation من YAML
            Object operationParam = parameters.get("operation");

            if (operationParam == null || operationParam.toString().isBlank()) {
                // ✅ ما كتبها؟ نضيفها لحالنا
                remaining = "default";
                parameters.put("operation", "default");
                LOG.info("⚙️ No 'operation' provided, using default='{}'", remaining);
            } else {
                // ✅ موجودة؟ نستخدمها
                remaining = operationParam.toString();
                LOG.info("🔹 Using provided 'operation' parameter: {}", remaining);
            }
        }

        // ✅ أنشئ الـ endpoint كالمعتاد
        IntegrationMappingEndpoint endpoint = new IntegrationMappingEndpoint(uri, this);
        setProperties(endpoint, parameters);

        // ✅ Inject الـ service
        if (this.integrationMappingService == null) {
            this.integrationMappingService = getCamelContext().getRegistry()
                    .lookupByNameAndType(IntegrationMappingService.BEAN_ID, IntegrationMappingService.class);
            if (this.integrationMappingService == null) {
                throw new IllegalStateException("IntegrationMappingService is required but not found in Camel registry.");
            }
        }
        endpoint.setIntegrationMappingService(integrationMappingService);

        endpoint.setOperation(remaining);

        return endpoint;
    }


    public IntegrationMappingService getIntegrationMappingService() { return integrationMappingService; }
    public void setIntegrationMappingService(IntegrationMappingService s) { this.integrationMappingService = s; }
}
