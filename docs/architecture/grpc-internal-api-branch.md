# gRPC Internal API Branch

**Branch:** `experiment/grpc-internal-api`  
**Status:** Experimental  
**Purpose:** Compare the current REST-first backend with internal gRPC service interfaces.

## Scope

This branch keeps the REST API as the product baseline and adds an optional gRPC server for service-to-service style reads.

The gRPC server is disabled by default:

```text
PLATFORM_GRPC_ENABLED=false
```

Enable it with:

```text
PLATFORM_GRPC_ENABLED=true
PLATFORM_GRPC_PORT=9090
```

## Current Service Interface

```text
platform.v1.PlatformQueryService/ListTasks
platform.v1.PlatformQueryService/ListDocuments
```

Source contract:

```text
platform/backend/src/main/proto/platform_query.proto
```

## Current Implementation Notes

- gRPC uses Netty via `grpc-netty-shaded`.
- Requests use `google.protobuf.StringValue` for the workspace ID.
- Responses use `google.protobuf.Struct` to keep this first branch compile-safe without adding protoc generation.
- The implementation delegates to the existing `TasksFacade` and `DocumentsFacade`.
- REST, GraphQL, and benchmark APIs remain available.

## Next Step

If this branch proves useful, add a protobuf generation step and replace `Struct` responses with typed message classes for stronger client contracts and better payload-size comparison.
