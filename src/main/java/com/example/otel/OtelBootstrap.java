package com.example.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.time.Duration;

public final class OtelBootstrap {
  private static volatile boolean initialized = false;

  public static synchronized OpenTelemetry init() {
    if (initialized) return GlobalOpenTelemetry.get();
    String endpoint = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");

    Resource resource = Resource.getDefault()
      .merge(Resource.create(Attributes.builder()
        .put(ResourceAttributes.SERVICE_NAME, "azure-func-java")
        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
        .build()));

    // Exporters (gRPC)
    var spanExporter   = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
    var metricExporter = OtlpGrpcMetricExporter.builder().setEndpoint(endpoint).build();
    var logExporter    = OtlpGrpcLogRecordExporter.builder().setEndpoint(endpoint).build();

    var tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
      .setResource(resource)
      .build();

    var meterProvider = SdkMeterProvider.builder()
      .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
        .setInterval(Duration.ofSeconds(10)).build())
      .setResource(resource)
      .build();

    var loggerProvider = SdkLoggerProvider.builder()
      .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
      .setResource(resource)
      .build();

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .setMeterProvider(meterProvider)
      .setLoggerProvider(loggerProvider)
      .buildAndRegisterGlobal();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      sdk.getSdkLoggerProvider().close();
      sdk.getSdkTracerProvider().close();
      sdk.getSdkMeterProvider().close();
    }));

    initialized = true;
    return GlobalOpenTelemetry.get();
  }

  public static Tracer tracer() { return GlobalOpenTelemetry.get().getTracer("app"); }
  public static Meter meter() { return GlobalOpenTelemetry.get().getMeter("app"); }
}
