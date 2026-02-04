# Kafka Setup Guide

## Local Development with Docker

### Prerequisites
- Docker and Docker Compose installed

### Starting Kafka

1. **Start Kafka cluster:**
   ```bash
   docker-compose up -d
   ```

2. **Verify services are running:**
   ```bash
   docker-compose ps
   ```

3. **Check Kafka logs:**
   ```bash
   docker-compose logs -f kafka
   ```

### Accessing Kafka UI

Kafka UI is available at: http://localhost:8080

You can:
- View topics
- Browse messages
- Monitor consumer groups
- Check topic configurations

### Stopping Kafka

```bash
docker-compose down
```

To remove volumes (delete all data):
```bash
docker-compose down -v
```

## Configuration

### Application Configuration

Update your `application.yml` or environment variables:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

Or set environment variable:
```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Notification Engine Configuration

```yaml
fractal:
  notify:
    async:
      mode: kafka  # Use Kafka for async processing
      kafka:
        enabled: true
        topic: notifications
        consumer-group: notification-processors
        partitions: 3
        replication-factor: 1
        concurrency: 3
```

## Testing Kafka

### Using Kafka Console Producer

```bash
# Enter Kafka container
docker exec -it kafka bash

# Create a test message
kafka-console-producer --bootstrap-server localhost:9092 --topic notifications
```

### Using Kafka Console Consumer

```bash
# Enter Kafka container
docker exec -it kafka bash

# Consume messages
kafka-console-consumer --bootstrap-server localhost:9092 --topic notifications --from-beginning
```

## Topic Management

The notification topic will be automatically created when the application starts. However, you can also create it manually:

```bash
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic notifications \
  --partitions 3 \
  --replication-factor 1
```

### List Topics

```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Describe Topic

```bash
docker exec -it kafka kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic notifications
```

## Monitoring

### Check Consumer Group Status

```bash
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-processors --describe
```

### Check Topic Messages

```bash
docker exec -it kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic notifications
```

## Troubleshooting

### Kafka not starting

1. Check if ports 9092, 2181 are already in use:
   ```bash
   netstat -an | grep 9092
   netstat -an | grep 2181
   ```

2. Check Docker logs:
   ```bash
   docker-compose logs kafka
   ```

### Connection refused

- Ensure Kafka is running: `docker-compose ps`
- Check bootstrap servers configuration
- Verify network connectivity

### Messages not being consumed

1. Check consumer group:
   ```bash
   docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
   ```

2. Check application logs for consumer errors

3. Verify topic exists:
   ```bash
   docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

## Production Considerations

For production, consider:

1. **Multiple Brokers**: Use replication factor > 1
2. **Persistent Storage**: Mount volumes for Kafka data
3. **Security**: Enable SASL/SSL
4. **Monitoring**: Use Kafka metrics and monitoring tools
5. **Backup**: Regular backup of Kafka data
6. **Resource Limits**: Set appropriate memory and CPU limits

Example production docker-compose snippet:
```yaml
kafka:
  volumes:
    - kafka-data:/var/lib/kafka/data
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '1.0'
```
