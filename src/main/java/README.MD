# TypeList Batch Processing Service

This Spring Batch application processes large files containing TypeList, TypeItem, and TypeSet data and updates a TypeList web service. The application doesn't store data in a database but acts as a bridge between file data and the web service.

## Getting Started

### Prerequisites
- Java 11 or newer
- Maven 3.6+ or Gradle 7.0+ (depending on your build system)
- Network access to the TypeList web service endpoint

### Build the Application
```bash
# If using Maven
mvn clean package

# If using Gradle
./gradlew clean build
```

### Run the Application
```bash
# Run with default configuration
java -jar target/typelist-batch-service-1.0.0.jar

# Run with specific profile
java -jar target/typelist-batch-service-1.0.0.jar --spring.profiles.active=dev

# Run with external config file
java -jar target/typelist-batch-service-1.0.0.jar --spring.config.location=file:/path/to/application.properties
```

### Configuration Options
You can override default configuration settings using command-line arguments:
```bash
java -jar target/typelist-batch-service-1.0.0.jar \
  --api.base.url=https://api.custom-domain.com \
  --batch.chunk-size=500 \
  --batch.max-threads=4
```

## Input File Configuration

### File Location
By default, the application looks for input files in the following directories:
```
/data/typelistservice/input/
```

Default file paths are configured in application.properties:
```properties
# Default file paths
typelist.file.path=input/typelists.json
typeitem.file.path=input/typeitems.json
typeset.file.path=input/typesets.json

# Input, archive and error directories
file.input.directory=/data/typelistservice/input/
file.archive.directory=/data/typelistservice/archive/
file.error.directory=/data/typelistservice/error/
```

### File Naming Conventions
The application processes files based on specific naming patterns:

- TypeList files: `typelist_*.csv`, `typelist_*.json`, or `typelist_*.xlsx`
- TypeItem files: `typeitem_*.csv`, `typeitem_*.json`, or `typeitem_*.xlsx`
- TypeSet files: `typeset_*.csv`, `typeset_*.json`, or `typeset_*.xlsx`

Example valid filenames:
- `typelist_20250428.csv`
- `typeitem_batch75.json`
- `typeset_update_v2.xlsx`

## Triggering Batch Processing

The application supports multiple trigger methods:

### 1. Automatic Scheduled Processing
By default, the application runs a scheduled task that checks for new files every hour.

Configure the schedule in application.properties:
```properties
# Cron expression for scheduled processing (default: every hour)
batch.schedule.cron=0 0 * * * *

# Disable scheduled processing
batch.schedule.enabled=true
```

### 2. REST API Trigger
The application exposes REST endpoints to manually trigger processing:

```
POST /api/batch/process/all                  # Process all pending files
POST /api/batch/process/typelist             # Process only TypeList files
POST /api/batch/process/typeitem             # Process only TypeItem files  
POST /api/batch/process/typeset              # Process only TypeSet files
POST /api/batch/process/file?path={filePath} # Process a specific file
```

### 3. Command-line Arguments
You can trigger immediate processing during application startup:

```bash
java -jar target/typelist-batch-service-1.0.0.jar --batch.process-on-startup=true
```

Note: By default, `spring.batch.job.enabled=false` in the application.properties, which prevents jobs from running automatically at startup.

## Monitoring and Logging

### Log Files
Logs are written to the following location by default:
```
/var/log/typelistservice/batch.log
```

Configure logging in application.properties:
```properties
# Log levels
logging.level.root=INFO
logging.level.com.bazarbozorg=DEBUG
logging.level.org.springframework.batch=INFO

# Log file configuration
logging.file.name=/var/log/typelistservice/batch.log
```

### Monitoring Endpoints
The application exposes Spring Boot Actuator endpoints for monitoring:

```
GET /actuator/health          # Application health status
GET /actuator/info            # Application information
GET /actuator/metrics         # Application metrics
GET /actuator/prometheus      # Prometheus metrics
```

These endpoints are enabled in application.properties:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### Performance Monitoring
The application includes detailed performance logging:

```properties
batch.performance.log-interval-chunks=10
batch.performance.detailed-logging=true
```

Real-time processing status is available through:
1. Log output (INFO level)
2. The ProgressTrackingWriter component
3. REST API endpoint: `GET /api/batch/status`

## Error Handling and Recovery

Error handling configuration in application.properties:
```properties
error.skip.limit=1000
error.retry.limit=3
error.retry.interval.seconds=5
```

When files fail processing:
1. They are moved to the error directory (`file.error.directory`)
2. Error details are logged
3. Notification emails can be sent (if configured)

To retry failed files:
```bash
# Move files back to input directory
mv /data/typelistservice/error/failed_file.csv /data/typelistservice/input/

# Then trigger processing via REST API or restart application
```

## Complete File List

Below is a comprehensive list of all files in this application. Files marked with ✅ are already implemented.

### Main Application

1. **com.bazarbozorg.typelistservice.BatchApplication.java**
- Main Spring Boot application entry point with @SpringBootApplication and @EnableBatchProcessing

### Configuration Files

2. **com.bazarbozorg.typelistservice.batch.config.BatchConfiguration.java** ✅
- Core batch job and step configuration
- Defines the job flow (TypeList → TypeItem → TypeSet)
- Sets up multi-threaded execution

3. **com.bazarbozorg.typelistservice.batch.config.WebServiceClientConfig.java** ✅
- Configuration for the TypeList web service client

4. **com.bazarbozorg.ApiClientConfiguration.java** ✅
- Authentication and API client configuration
- JWT token management
- Configuration for timeouts and connection settings

### Service Layer

5. **com.bazarbozorg.typelistservice.batch.service.TypelistWebServiceClient.java** ✅
- Client for interacting with the TypeList web service API
- Methods for create, update, and load operations for all entity types
- Batch processing functionality

### Writers

6. **com.bazarbozorg.typelistservice.batch.writer.TypeItemWebServiceWriter.java** ✅
- Writer implementation for TypeItem data
- Sends processed TypeItems to the web service

7. **com.bazarbozorg.typelistservice.batch.writer.TypeListWebServiceWriter.java** ✅
- Writer implementation for TypeList data
- Sends processed TypeLists to the web service

8. **com.bazarbozorg.typelistservice.batch.writer.TypeSetWebServiceWriter.java** ✅
- Writer implementation for TypeSet data
- Sends processed TypeSets to the web service

9. **com.bazarbozorg.typelistservice.batch.ProgressTrackingWriter.java**
- Wrapper writer that tracks progress during batch processing
- Provides metrics on processing speed and completion percentage

### Readers

10. **com.bazarbozorg.typelistservice.batch.reader.TypeItemReader.java**
- Reader for TypeItem data from files
- Supports different file formats

11. **com.bazarbozorg.typelistservice.batch.reader.TypeListReader.java**
- Reader for TypeList data from files
- Supports different file formats

12. **com.bazarbozorg.typelistservice.batch.reader.TypeSetReader.java**
- Reader for TypeSet data from files
- Supports different file formats

13. **com.bazarbozorg.typelistservice.batch.reader.FileReaderFactory.java**
- Factory for creating appropriate readers based on file type
- Detects file format and creates corresponding reader

### Format-specific Readers

14. **com.bazarbozorg.typelistservice.batch.reader.TypeCsvItemReader.java**
- CSV-specific implementation for reading TypeItems

15. **com.bazarbozorg.typelistservice.batch.reader.TypeJsonItemReader.java**
- JSON-specific implementation for reading TypeItems

16. **com.bazarbozorg.typelistservice.batch.reader.TypeExcelItemReader.java**
- Excel-specific implementation for reading TypeItems

### Processors

17. **com.bazarbozorg.typelistservice.batch.processor.TypeItemProcessor.java**
- Processor for TypeItem data
- Validates and transforms TypeItems

18. **com.bazarbozorg.typelistservice.batch.processor.TypeListProcessor.java**
- Processor for TypeList data
- Validates and transforms TypeLists

19. **com.bazarbozorg.typelistservice.batch.processor.TypeSetProcessor.java**
- Processor for TypeSet data
- Validates and transforms TypeSets

### Exception Handling

20. **com.bazarbozorg.typelistservice.batch.exception.TypeServiceException.java** ✅
- Custom exception for TypeList service errors
- Used to wrap and handle service-specific exceptions

21. **com.bazarbozorg.typelistservice.batch.exception.FileProcessingException.java**
- Exception for file processing errors
- Handles issues with file reading or parsing

### Utilities

22. **com.bazarbozorg.typelistservice.batch.BatchUtils.java**
- Utility methods for batch processing
- Synchronized reader wrappers and skip policies

23. **com.bazarbozorg.typelistservice.batch.FileTypeDetector.java**
- Utility for detecting file types
- Identifies CSV, JSON, Excel formats

### Listeners

24. **com.bazarbozorg.typelistservice.batch.listener.JobCompletionNotificationListener.java**
- Listener for job completion notifications
- Logs job results and sends notifications

25. **com.bazarbozorg.typelistservice.batch.listener.PerformanceMonitoringListener.java**
- Listener for performance monitoring
- Tracks execution time and throughput

### Configuration Properties

26. **src/main/resources/application.properties**
- Configuration properties for the application
- API endpoints, credentials, batch settings

## Model Classes (From Client Library)

These classes are already provided by the client library and don't need to be implemented:

- **com.bazarbozorg.typelistservice.client.model.TypeItemDTO**
- **com.bazarbozorg.typelistservice.client.model.TypeListDTO**
- **com.bazarbozorg.typelistservice.client.model.TypeSetDTO**
- Other request/response DTOs used by the service

## API Clients (From Client Library)

These interfaces are already provided by the client library:

- **com.bazarbozorg.typelistservice.client.api.TypeItemManagementApi**
- **com.bazarbozorg.typelistservice.client.api.TypeListManagementApi**
- **com.bazarbozorg.typelistservice.client.api.TypeSetManagementApi**
- **com.bazarbozorg.typelistservice.client.api.AuthenticationApi**

## Implementation Notes

- Each entity type (TypeList, TypeItem, TypeSet) has its own reader, processor, and writer
- The batch job processes these entities in sequence to maintain data integrity
- Readers detect and handle different file formats
- Processors validate and transform the data
- Writers send the processed data to the TypeList web service
- Error handling is implemented at multiple levels
- Performance monitoring tracks execution metrics

## Advanced Configuration Options

The application.properties file contains several additional options that can be uncommented and configured for advanced use cases:

### CORS Configuration
```properties
# CORS Configuration (if needed)
#spring.webmvc.cors.allowed-origins=*
#spring.webmvc.cors.allowed-methods=GET,POST,PUT,DELETE
#spring.webmvc.cors.allowed-headers=*
```

### Remote Chunking
```properties
# Advanced Batch Configuration - Remote Chunking
#spring.batch.remote-chunking.enabled=false
#spring.batch.remote-chunking.manager.address=localhost:9991
#spring.batch.remote-chunking.worker.address=localhost:9992
```

### Partitioning
```properties
# Partitioning with external resources
#batch.partitioning.grid-size=${batch.max-threads}
#batch.partitioning.input-pattern=file:input/typeitems_*.json
```

### Cloud Storage Integration
```properties
# Cloud storage integration
#cloud.storage.enabled=false
#cloud.storage.bucket=typelist-batch-files
#cloud.storage.region=us-east-1
```