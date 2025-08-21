package com.middleware.component.errormapper;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The consumer for the MyCustom component. (Currently not implemented as
 * endpoint is producerOnly).
 */
public class MyCustomConsumer extends DefaultConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MyCustomConsumer.class);

    public MyCustomConsumer(MyCustomEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        LOG.info("MyCustomConsumer initialized (though this component is producer-only).");
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.warn(
                "MyCustomConsumer started, but this component is configured as producer-only. No messages will be consumed.");
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        LOG.info("MyCustomConsumer stopped.");
    }
}
