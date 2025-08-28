package com.middleware.backend.kaotocamel.integrationBeans;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("dhis2CsvDryRunThenCommit")
public class Dhis2CsvDryRunThenCommit implements Processor {

    @Autowired
    private ProducerTemplate producer;

    @Autowired
    private Dhis2ImportSummarizer dhis2ImportSummarizer;

    private static final String DHIS2_ENDPOINT_URI =
            "https://{{dhis2.base-url}}/api/dataValueSets"
                    + "?authenticationPreemptive=true"
                    + "&authUsername={{dhis2.username}}"
                    + "&authPassword={{dhis2.password}}"
                    + "&throwExceptionOnFailure=false"
                    + "&bridgeEndpoint=true"
                    + "&headerFilterStrategy=#dhis2HeaderFilter";

    @Override
    public void process(Exchange exchange) throws Exception {
         final String baseQuery = exchange.getProperty("_queryBase", String.class);
        final byte[] csvBytes = exchange.getProperty("_csvBytes", byte[].class);

         Exchange dryRunEx = ExchangeBuilder.anExchange(exchange.getContext()).build();
        prepareHttpRequest(dryRunEx, baseQuery, csvBytes, true); // dryRun=true

        Exchange dryRunOut = producer.send(DHIS2_ENDPOINT_URI, dryRunEx);

         String dryBody = bodyAsString(dryRunOut);
        Message out = exchange.getMessage();
        out.setBody(dryBody);
        copyResponseHeaders(dryRunOut, out);

         dhis2ImportSummarizer.process(exchange);
        if ("true".equals(String.valueOf(out.getHeader("CamelRouteStop")))) {
            exchange.setRouteStop(true);
            return;
        }

         Exchange commitEx = ExchangeBuilder.anExchange(exchange.getContext()).build();
        prepareHttpRequest(commitEx, baseQuery, csvBytes, false); // dryRun=false

        Exchange commitOut = producer.send(DHIS2_ENDPOINT_URI, commitEx);

        String commitBody = bodyAsString(commitOut);
        out.setBody(commitBody);
        copyResponseHeaders(commitOut, out);

         dhis2ImportSummarizer.process(exchange);
    }

     private static void prepareHttpRequest(Exchange ex, String baseQuery, byte[] csv, boolean dryRun) {
        final Message msg = ex.getMessage();
         msg.setHeader(Exchange.HTTP_QUERY, baseQuery + "&format=csv&dryRun=" + dryRun);
         msg.setHeader(Exchange.HTTP_METHOD, "POST");
        msg.setHeader(Exchange.CONTENT_TYPE, "application/csv");
        msg.setHeader("Accept", "application/json");
         msg.setBody(csv);
    }

    private static String bodyAsString(Exchange ex) {
        Object b = ex.getMessage().getBody();
        return b == null ? "" : ex.getMessage().getBody(String.class);
    }

    private static void copyResponseHeaders(Exchange from, Message to) {
        to.getHeaders().putAll(from.getMessage().getHeaders());
    }
}
