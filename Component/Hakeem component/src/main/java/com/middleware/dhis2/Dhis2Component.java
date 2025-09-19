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
 * Camel component for DHIS2 integration under the scheme "hakeem".
 * <p>
 * Creates {@link Dhis2Endpoint} instances and ensures a {@link com.middleware.dhis2.service.Dhis2Service}
 * is available from the Camel registry, unless explicitly injected.
 */
@Component("hakeem")
public class Dhis2Component extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2Component.class);

    /**
     * DHIS2 service used by endpoints to perform operations. Looked up lazily
     * from the Camel registry when not provided programmatically.
     */
    @Metadata(description = "DHIS2 service for handling operations", required = false)
    private Dhis2Service dhis2Service;

    /**
     * Default constructor.
     */
    public Dhis2Component() {
        LOG.info("Dhis2Component initialized.");
    }

    /**
     * Constructs the component with a Camel context.
     *
     * @param context Camel context
     */
    public Dhis2Component(CamelContext context) {
        super(context);
        LOG.info("Dhis2Component initialized with CamelContext.");
    }

    @Override
    /**
     * Creates a new {@link Dhis2Endpoint} for the given URI and operation and
     * binds component/endpoint options. If a {@link Dhis2Service} is not set on
     * the component, attempts a registry lookup by {@code Dhis2Service.BEAN_ID}.
     *
     * @param uri        full endpoint URI
     * @param remaining  the operation path (syntax: hakeem:operation)
     * @param parameters endpoint parameters to bind
     * @return initialized endpoint
     * @throws IllegalStateException when the {@code Dhis2Service} is not found
     */
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Dhis2Endpoint endpoint = new Dhis2Endpoint(uri, this, remaining);

        setProperties(endpoint, parameters);

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

    /**
     * Returns the shared {@link Dhis2Service} for this component.
     */
    public Dhis2Service getDhis2Service() {
        return dhis2Service;
    }

    /**
     * Sets the shared {@link Dhis2Service} for this component.
     *
     * @param dhis2Service service instance
     */
    public void setDhis2Service(Dhis2Service dhis2Service) {
        this.dhis2Service = dhis2Service;
        LOG.info("Dhis2Service set on component: {}", dhis2Service);
    }
}