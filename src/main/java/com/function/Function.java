package com.function;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.otel.OpenTelemetryConfig;
import com.example.otel.OtelBootstrap;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private static final Logger log = LoggerFactory.getLogger(Function.class);
    private static final Tracer tracer = OpenTelemetryConfig.getTracer();
    private static final LongCounter requests = OpenTelemetryConfig.getMeter()
            .counterBuilder("requests_total")
            .setDescription("Total requests")
            .setUnit("1")
            .build();
//        OtelBootstrap.meter().counterBuilder("http_requests_total").build();
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        // trace span
        Span span = tracer.spanBuilder("Function.HttpExample").startSpan();
        try {
            String name = request.getQueryParameters().getOrDefault("name", "world");
            requests.add(1, Attributes.empty()); // metric
            log.info("Processing request for name={}", name); // log via SLF4J/Logback -> OTEL

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Hello " + name + " from Azure Functions in Java")
                    .build();
        } catch (Exception e) {
            log.error("Error in function", e);
            throw e;
        } finally {
            span.end();
        }
    }
}
