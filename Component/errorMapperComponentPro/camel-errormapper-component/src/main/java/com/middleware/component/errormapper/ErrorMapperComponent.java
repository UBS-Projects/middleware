package com.middleware.component.errormapper;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.component.errormapper.service.ErrorMappingService;

/**
 * Represents the component that manages {@link ErrorMapperEndpoint}. This
 * component requires an ErrorMappingService to be present in the Camel registry
 * (e.g., Spring context).
 */

@Component("errormapper")
public class ErrorMapperComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMapperComponent.class);

    // This service will be injected by Spring if available, or looked up in the
    // registry.
    // It's crucial for accessing the error mapping configuration from the backend
    // project.
    private ErrorMappingService errorMappingService;

    public ErrorMapperComponent() {
        LOG.info("ErrorMapperComponent initialized.");
    }

    public ErrorMapperComponent(CamelContext context) {
        super(context);
        LOG.info("ErrorMapperComponent initialized with CamelContext.");
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ErrorMapperEndpoint endpoint = new ErrorMapperEndpoint(uri, this, remaining);

        // Set properties from URI query parameters
        setProperties(endpoint, parameters);

        // Ensure errorMappingService is set, either via @Autowired or manual lookup
        if (this.errorMappingService == null) {
            this.errorMappingService = getCamelContext().getRegistry().lookupByNameAndType(ErrorMappingService.BEAN_ID,
                    ErrorMappingService.class);
            if (this.errorMappingService == null) {
                LOG.error(
                        "ErrorMappingService not found in Camel registry. Please ensure it's defined as a Spring bean in your backend project with ID: {}",
                        ErrorMappingService.BEAN_ID);
                throw new IllegalStateException("ErrorMappingService is required but not found in Camel registry.");
            }
            LOG.info("ErrorMappingService successfully looked up from Camel registry.");
        }
        endpoint.setErrorMappingService(errorMappingService);

        LOG.info("Created ErrorMapperEndpoint: {}", uri);
        return endpoint;
    }

    public ErrorMappingService getErrorMappingService() {
        return errorMappingService;
    }

    /**
     * Sets the ErrorMappingService. This method can be used by Spring to autowire
     * the service into the component when it's used within a Spring Boot
     * application.
     * 
     * @param errorMappingService The ErrorMappingService instance.
     */
    public void setErrorMappingService(ErrorMappingService errorMappingService) {
        this.errorMappingService = errorMappingService;
        LOG.info("ErrorMappingService set on component: {}", errorMappingService);
    }
}
