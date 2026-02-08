# gRPC in Java - Quick Reference

## What is gRPC?

gRPC is a **remote procedure call framework** where services communicate via **Protobuf contracts over HTTP/2** with
generated clients and servers.

- Makes service-to-service calls **fast, typed, streaming, and reliable**.
- You write a `.proto` → generate Java code → implement server → create stub client → call methods like local proxy
  methods.
- Removes JSON overhead, manual clients, weak typing, and HTTP/1 inefficiencies.
- Best for **internal microservice communication**, not public browser APIs.

---

## When to Use gRPC

### Use gRPC for:

- Microservices internal APIs
- High throughput systems
- Streaming pipelines
- Polyglot services
- Low latency calls

### Avoid gRPC for:

- Browser public APIs
- Simple CRUD apps
- Human-debuggable APIs

---

## Why gRPC is Faster & Reliable

- **Binary Serialization (Protobuf):** smaller payloads, faster encode/decode than JSON/XML
- **HTTP/2 Transport:** multiplexing, persistent connections, header compression
- **Single Contract (.proto):** one schema for all languages, no drift
- **Code Generation:** no manual clients, fewer runtime bugs
- **Strong Typing:** compile-time safety vs runtime JSON errors
- **Streaming Built-in:** unary, server, client, and bidirectional streams
- **Flow Control:** backpressure and cancellation handled by framework
- **Connection Reuse:** fewer TCP handshakes, lower latency

> JSON/XML are "typed" by convention, but Protobuf is schema-enforced at compile time.

---

## gRPC Communication Models

- **Unary:** request → response
- **Server Streaming:** request → many responses
- **Client Streaming:** many requests → one response
- **Bidirectional Streaming:** many ↔ many

> All run over a single HTTP/2 connection.

---

## Implementation Guidelines (Java)

### Sample Proto

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.company.billing.grpc";

service BillingService {
  rpc add(AddRequest) returns (AddResponse);
}

message AddRequest {
  string patientId = 1;
  string status = 2;
}

message AddResponse {
  string result = 1;
}
```

The `.proto` file is the **single source of truth**.

### Generate Java Code

Use `protoc` via Maven/Gradle plugins.

Generates:

- DTOs
- Client stubs
- Server interfaces

No manual serialization needed.

### Implement Server

```java
public class BillingServiceImpl extends BillingServiceGrpc.BillingServiceImplBase {
    @Override
    public void add(AddRequest req, StreamObserver<AddResponse> res) {
        res.onNext(AddResponse.newBuilder().setResult("OK").build());
        res.onCompleted();
    }
}
```

### Create Client

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build();

BillingServiceGrpc.BillingServiceBlockingStub stub =
        BillingServiceGrpc.newBlockingStub(channel);

stub.

add(request);
```

### gRPC Architecture Flow

```
.proto → protoc → Java stubs → Server impl → Client stub → HTTP/2 → Protobuf
```

---

## Proto Sharing at Scale

**Industry Practice:** Proto files are APIs, not copied between services.

### Shared Proto Repository Example

```
company-protos/
  billing.proto
  patient.proto
  common.proto
```

### Pipeline

```
Proto Repo
    ↓
  Build
    ↓
Publish Artifact
    ↓
Services Import Dependency
    ↓
protoc Generates Stubs
```

### Why It Matters

- Single source of truth
- Versioning & backward compatibility
- No copy-paste
- Safe upgrades

### Maven Example

Publish proto as a jar:

```xml

<groupId>com.company</groupId>
<artifactId>company-protos</artifactId>
<version>1.2.3</version>
```

Service imports:

```xml

<dependency>
    <groupId>com.company</groupId>
    <artifactId>company-protos</artifactId>
    <version>1.2.3</version>
</dependency>
```

The gRPC plugin picks up `.proto` files and runs `protoc` during build.

Each service:

- Pulls proto jar
- Recompiles stubs
- Gets same interfaces

**Treat `.proto` like public APIs:** version them, publish them, never copy them.

---

## Final Summary

gRPC in Java enables fast, typed, streaming microservice communication using Protobuf over HTTP/2, where services share
versioned proto contracts from a central repo and generate clients automatically instead of writing REST APIs by hand.
