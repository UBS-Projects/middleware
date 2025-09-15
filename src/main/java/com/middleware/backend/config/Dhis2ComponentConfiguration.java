package com.middleware.backend.config;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.dhis2.model.Dhis2Request;
import com.middleware.dhis2.model.Dhis2Response;
import com.middleware.dhis2.service.Dhis2Service;
import com.middleware.backend.kaotocamel.integrationBeans.CsvGuard;
import com.middleware.backend.kaotocamel.integrationBeans.Dhis2CsvDryRunThenCommit;
import com.middleware.backend.kaotocamel.integrationBeans.Dhis2ImportSummarizer;

@Configuration
public class Dhis2ComponentConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2ComponentConfiguration.class);

    @Autowired
    private CsvGuard csvGuard;

    @Autowired
    private Dhis2CsvDryRunThenCommit dhis2Upload;

    @Autowired
    private Dhis2ImportSummarizer importSummarizer;

    @Bean(name = "dhis2ComponentService")
    public Dhis2Service dhis2ComponentServiceImplementation() {
        return new Dhis2Service() {

            @Override
            public Dhis2Response validateCsvGuard(Dhis2Request request) {
                try {
                    LOG.info("Executing CSV Guard validation with actual logic");

                    // إنشاء mock exchange لاستدعاء CsvGuard
                    Exchange exchange = createMockExchange(request);

                    // استدعاء المنطق الفعلي
                    csvGuard.process(exchange);

                    // فحص إذا كان هناك خطأ
                    Boolean routeStop = exchange.getProperty(Exchange.ROUTE_STOP, Boolean.class);
                    if (routeStop != null && routeStop) {
                        String body = exchange.getIn().getBody(String.class);
                        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                        return Dhis2Response.error(statusCode != null ? statusCode : 400, "ERROR",
                                "Guard validation failed: " + body);
                    }

                    // نجح التحقق
                    return Dhis2Response.success("CSV validation completed successfully", null);

                } catch (Exception e) {
                    LOG.error("CSV Guard validation failed: {}", e.getMessage(), e);
                    return Dhis2Response.error(400, "ERROR", "Validation failed: " + e.getMessage());
                }
            }

            @Override
            public Dhis2Response uploadCsv(Dhis2Request request) {
                try {
                    LOG.info("Executing CSV upload with actual DHIS2 logic");

                    // إنشاء mock exchange لاستدعاء Upload
                    Exchange exchange = createMockExchange(request);

                    // استدعاء المنطق الفعلي
                    dhis2Upload.process(exchange);

                    // فحص النتيجة
                    String responseBody = exchange.getIn().getBody(String.class);
                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

                    if (statusCode != null && statusCode >= 400) {
                        return Dhis2Response.error(statusCode, "ERROR", "Upload failed");
                    }

                    return Dhis2Response.success("CSV upload completed successfully", responseBody);

                } catch (Exception e) {
                    LOG.error("CSV upload failed: {}", e.getMessage(), e);
                    return Dhis2Response.error(500, "ERROR", "Upload failed: " + e.getMessage());
                }
            }

            @Override
            public Dhis2Response summarizeImport(Dhis2Request request) {
                try {
                    LOG.info("Executing import summary with actual logic");

                    // إنشاء mock exchange لاستدعاء Summarizer
                    Exchange exchange = createMockExchange(request);

                    // إذا كان هناك response body من الخطوة السابقة، استخدمه
                    if (request.getResponseBody() != null) {
                        exchange.getIn().setBody(request.getResponseBody());
                    }

                    // استدعاء المنطق الفعلي
                    importSummarizer.process(exchange);

                    // الحصول على النتيجة
                    String responseBody = exchange.getIn().getBody(String.class);
                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

                    if (statusCode != null && statusCode >= 400) {
                        return Dhis2Response.error(statusCode, "ERROR", "Summarization failed");
                    }

                    return Dhis2Response.success("Import summarized successfully", responseBody);

                } catch (Exception e) {
                    LOG.error("Import summarization failed: {}", e.getMessage(), e);
                    return Dhis2Response.error(500, "ERROR", "Summarization failed: " + e.getMessage());
                }
            }

            @Override
            public Dhis2Response processComplete(Dhis2Request request) {
                try {
                    LOG.info("Starting complete DHIS2 processing pipeline with actual logic");

                    // Step 1: Validate CSV Guard
                    LOG.info("Step 1: CSV Guard validation");
                    Dhis2Response guardResponse = validateCsvGuard(request);
                    if (!guardResponse.isSuccess()) {
                        LOG.error("CSV Guard validation failed");
                        return guardResponse;
                    }

                    // Step 2: Upload CSV
                    LOG.info("Step 2: CSV upload");
                    Dhis2Response uploadResponse = uploadCsv(request);
                    if (!uploadResponse.isSuccess()) {
                        LOG.error("CSV upload failed");
                        return uploadResponse;
                    }

                    // Step 3: Summarize (استخدم نتيجة Upload)
                    LOG.info("Step 3: Import summarization");
                    if (uploadResponse.getDetails() != null) {
                        request.setResponseBody(uploadResponse.getDetails().toString());
                    }
                    Dhis2Response summaryResponse = summarizeImport(request);

                    LOG.info("Complete DHIS2 processing pipeline finished successfully");

                    // إرجاع النتيجة النهائية
                    return Dhis2Response.success("Complete DHIS2 processing finished successfully",
                            summaryResponse.getDetails());

                } catch (Exception e) {
                    LOG.error("Complete DHIS2 processing failed: {}", e.getMessage(), e);
                    return Dhis2Response.error(500, "ERROR", "Complete processing failed: " + e.getMessage());
                }
            }

            // Helper method لإنشاء mock exchange
            private Exchange createMockExchange(Dhis2Request request) {
                Exchange exchange = new DefaultExchange(new DefaultCamelContext());

                // ضبط البيانات من request
                if (request.getTransactionUUID() != null) {
                    exchange.getIn().setHeader("transactionUUID", request.getTransactionUUID());
                    exchange.getIn().setHeader("X-Transaction-UUID", request.getTransactionUUID());
                }

                if (request.getScheme() != null) {
                    exchange.getIn().setHeader("scheme", request.getScheme());
                }

                if (request.getStrategy() != null) {
                    exchange.getIn().setHeader("strategy", request.getStrategy());
                }

                exchange.getIn().setHeader("dryRun", String.valueOf(request.isDryRun()));

                if (request.getContentType() != null) {
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, request.getContentType());
                }

                if (request.getCsvBytes() != null) {
                    exchange.getIn().setBody(request.getCsvBytes());
                    exchange.setProperty("_csvBytes", request.getCsvBytes());
                }

                if (request.getQueryBase() != null) {
                    exchange.setProperty("_queryBase", request.getQueryBase());
                }

                return exchange;
            }
        };
    }
}