package com.middleware.dhis2;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.dhis2.service.Dhis2Service;

/**
 * DHIS2 Integration Component for CSV uploads and data synchronization
 */
@Component("hakeem")
public class Dhis2Component extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2Component.class);

    @Metadata(description = "DHIS2 service for handling operations", required = false)
    private Dhis2Service dhis2Service;

    public Dhis2Component() {
        LOG.info("Dhis2Component initialized.");
    }

    public Dhis2Component(CamelContext context) {
        super(context);
        LOG.info("Dhis2Component initialized with CamelContext.");
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Dhis2Endpoint endpoint = new Dhis2Endpoint(uri, this, remaining);

        // Set properties from URI query parameters
        setProperties(endpoint, parameters);

        // Ensure dhis2Service is set, either via @Autowired or manual lookup
        if (this.dhis2Service == null) {
            this.dhis2Service = getCamelContext().getRegistry()
                    .lookupByNameAndType(Dhis2Service.BEAN_ID, Dhis2Service.class);

            if (this.dhis2Service == null) {
                LOG.error(
                        "Dhis2Service not found in Camel registry. Please ensure it's defined as a Spring bean with ID: {}",
                        Dhis2Service.BEAN_ID);
                throw new IllegalStateException("Dhis2Service is required but not found in Camel registry.");
            }
            LOG.info("Dhis2Service successfully looked up from Camel registry.");
        }
        endpoint.setDhis2Service(dhis2Service);

        LOG.info("Created Dhis2Endpoint: {}", uri);
        return endpoint;
    }

    public Dhis2Service getDhis2Service() {
        return dhis2Service;
    }

    public void setDhis2Service(Dhis2Service dhis2Service) {
        this.dhis2Service = dhis2Service;
        LOG.info("Dhis2Service set on component: {}", dhis2Service);
    }
}