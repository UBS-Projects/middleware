package com.middleware;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class trueProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(trueProducer.class);
    private trueEndpoint endpoint;

    public trueProducer(trueEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        System.out.println(exchange.getIn().getBody());
    }

}
