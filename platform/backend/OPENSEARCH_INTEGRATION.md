# OpenSearch Integration for Document Full-Text Search

## Overview

OpenSearch is now integrated with the document search functionality. Documents are automatically indexed in OpenSearch when created or updated, enabling high-performance full-text search with relevance scoring.

## What Was Implemented

### Backend Changes
- **DocumentSearchDocument** (`src/main/java/com/example/platform/documents/domain/DocumentSearchDocument.java`)
  - OpenSearch document model with indexed fields: title, content (text analyzers), tenantId, workspaceId (keywords), and timestamps
  
- **DocumentSearchRepository** (`src/main/java/com/example/platform/documents/infrastructure/DocumentSearchRepository.java`)
  - Spring Data Elasticsearch repository for querying documents
  - Supports full-text search by title and content
  
- **DocumentsFacade Updates**
  - `searchDocuments()` now uses OpenSearch instead of SQL LIKE queries
  - `createDocument()` and `updateDocument()` automatically index documents in OpenSearch
  
- **Configuration** (`src/main/resources/application.yml`)
  - Added OpenSearch connection: `http://localhost:9200`
  
- **Docker Compose** 
  - Added OpenSearch service (`opensearchproject/opensearch:2.8.0`)
  - Added OpenSearch Dashboards (`opensearchproject/opensearch-dashboards:2.8.0`)

## Quick Start (Local Development)

### 1. Start OpenSearch Stack
```powershell
cd D:\Project\arch-hands-on\platform\backend\observability
docker compose up -d opensearch opensearch-dashboards
```

### 2. Verify OpenSearch is Running
```powershell
curl -u admin:Admin@123456 https://localhost:9200 -k
# or in PowerShell
Invoke-WebRequest -Uri https://localhost:9200 -Authentication Basic -Credential (New-Object System.Management.Automation.PSCredential('admin', (ConvertTo-SecureString 'Admin@123456' -AsPlainText -Force))) -SkipCertificateCheck
```

### 3. Access OpenSearch Dashboards
- URL: http://localhost:5601
- Username: `admin`
- Password: `Admin@123456`

### 4. Run Backend
```powershell
cd D:\Project\arch-hands-on\platform\backend
mvn spring-boot:run
```

### 5. Create and Search Documents

#### Create a document
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: acme-corp" \
  -H "X-Workspace-ID: workspace-1" \
  -H "X-User-ID: user-123" \
  -d '{
    "title": "Quarterly Business Plan",
    "content": "This document outlines our strategic initiatives for Q2 2026..."
  }'
```

#### Search documents (uses OpenSearch)
```bash
curl http://localhost:8080/api/v1/documents/search?query=quarterly \
  -H "X-Tenant-ID: acme-corp" \
  -H "X-Workspace-ID: workspace-1" \
  -H "X-User-ID: user-123"
```

#### Search via GraphQL
```graphql
query SearchDocuments {
  documents(workspaceId: "workspace-1") {
    documentId
    title
    content
  }
}
```

## Automatic Indexing

- When a **document is created**: automatically indexed in OpenSearch
- When a **document is updated**: index is updated
- **Failed indexing** is logged but doesn't block document creation/update (graceful degradation)

## OpenSearch Dashboards Usage

### View Indexed Documents
1. Go to http://localhost:5601
2. Navigation → Dev Tools → Console
3. Query indexed documents:
```json
GET documents/_search
{
  "query": {
    "match": {
      "title": "quarterly"
    }
  }
}
```

### Check Index Mapping
```json
GET documents/_mapping
```

## Performance & Relevance

OpenSearch provides:
- **Full-text search**: Find documents by any text in title or content
- **Relevance scoring**: Results sorted by match relevance (BM25 algorithm)
- **Analyzer**: Uses standard tokenization and lowercase normalization
- **Scalability**: Efficient for large document collections (millions of documents)

Example search query (backend):
```java
List<DocumentSearchDocument> results = documentSearchRepository
    .findByWorkspaceIdAndTitleContainsOrContentContains(
        "workspace-1",  // filter by workspace
        "quarterly",    // search in title
        "quarterly"     // search in content
    );
```

## Multi-Tenancy & Security

- **Tenant isolation**: Documents are indexed with `tenantId` keyword field
- **Workspace filtering**: Queries filter by `workspaceId` to ensure user sees only workspace documents
- **OpenSearch auth**: Using basic auth (admin/Admin@123456) for local development
  - For production: configure mTLS, IAM roles, or OpenSearch Security plugin

## Configuration

### Development (`application.yml`)
```yaml
spring:
  data:
    elasticsearch:
      uris: http://localhost:9200
```

### Production (Environment Variables)
```bash
SPRING_DATA_ELASTICSEARCH_URIS=https://opensearch.example.com:9200
SPRING_DATA_ELASTICSEARCH_USERNAME=service-user
SPRING_DATA_ELASTICSEARCH_PASSWORD=secure-password
```

## Monitoring & Observability

### Logs
Search documents in logs for "search_index" operations:
```bash
grep "search_index" logs/platform-backend.json.log
```

### Metrics
Method `searchDocuments()` is instrumented with observability aspect:
- Logged with correlation ID and execution time
- Metrics available at `/actuator/metrics`

### Traces
Elasticsearch client operations are traced via Micrometer Tracing; traces are exported to OTEL Collector → Tempo (if configured).

## Troubleshooting

### Issue: OpenSearch connection refused
**Solution**: 
1. Verify OpenSearch is running: `docker ps | grep opensearch`
2. Check logs: `docker logs observability_opensearch`
3. Ensure port 9200 is not blocked by firewall

### Issue: Index not created
**Solution**:
1. Spring Data Elasticsearch should auto-create the index
2. If missing, manually create via Dashboards:
```json
PUT documents
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "standard" },
      "content": { "type": "text", "analyzer": "standard" },
      "documentId": { "type": "keyword" },
      "tenantId": { "type": "keyword" },
      "workspaceId": { "type": "keyword" },
      "status": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

### Issue: Slow searches
**Solution**:
1. Check if index has enough replicas/shards for the data volume
2. Create additional indexes by date (`documents-2026-05`, `documents-2026-06`) and use index aliases
3. Monitor via Dashboards → Stack Management → Index Management

## Advanced Features (Optional)

### Custom Analyzers
Edit the index to use custom analyzers (e.g., stemming, synonyms):
```json
PUT documents/_settings
{
  "analysis": {
    "analyzer": {
      "custom_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase", "english_stemmer"]
      }
    },
    "filter": {
      "english_stemmer": {
        "type": "stemmer",
        "language": "english"
      }
    }
  }
}
```

### Index Lifecycle Management (ILM)
For high-volume analytics, configure ILM to roll over indexes by size/time:
```json
PUT _ilm/policy/documents_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0d",
        "actions": {
          "rollover": {
            "max_primary_size": "50gb",
            "max_age": "30d"
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

## Next Steps

1. **Test locally** with OpenSearch Dashboards
2. **Performance tuning**: Monitor query latency and optimize analyzers/field mappings
3. **Production deployment**: Configure TLS, authentication, backups
4. **Analytics integration**: Consider indexing analytics events in a separate OpenSearch cluster
5. **Monitoring**: Set up alerts for search latency and indexing failures


