# Kafka Quick Reference - Learning & Implementation Guide

## What is Kafka?

Kafka is a **distributed event streaming platform** that acts as a persistent, append-only log where **producers write
events into topics, stored across partitions on brokers, and consumer groups read those partitions in parallel** while a
controller manages metadata, replication, and failover.

- Makes event-driven architectures **scalable, reliable, and replayed**.
- You write events → Kafka partitions them → multiple consumers read in parallel → no message loss.
- Removes message queue limitations: single consumer, message deletion, weak replay.
- Best for **event-driven microservices, streaming pipelines, and audit logs**.

---

## Core Kafka Components (Foundation View)

### 1️⃣ Producer

**Sends events (messages) into Kafka.**

- Chooses topic
- Chooses partition (by key or round-robin)
- Serializes data
- Sends to broker

**Mental Model:** Producer = writer of events.

### 2️⃣ Consumer

**Reads events from Kafka.**

- Pulls records from partitions
- Tracks offsets (position in log)
- Processes messages
- Can replay from any offset

**Mental Model:** Consumer = reader of events.

### 3️⃣ Topic

**Logical stream of events.**

- Named channel
- Example: `orders`, `payments`, `users`, `logs`
- Immutable event log

**Mental Model:** Topic = event category / event stream.

### 4️⃣ Partition

**Physical shard of a topic for scaling and ordering.**

- Each partition is an append-only log
- Ordered within itself
- Distributed across brokers
- **Correction:** ❌ NOT "logical group of topics" → ✅ It IS a slice of a topic

**Mental Model:** Partition = shard of a topic.

### 5️⃣ Broker

**Kafka server node.**

- Stores partitions on disk
- Serves reads/writes
- Replicates data
- Elects leaders per partition

**Mental Model:** Broker = Kafka machine / process.

### 6️⃣ Cluster

**Group of brokers working together.**

- Distributes partitions
- Handles replication
- Handles failover

**Replication:**

- Each partition has:
    - **Leader:** handles all reads/writes
    - **Followers:** replicate data for redundancy
- If leader fails, a follower is elected leader

**Mental Model:** Cluster = multiple Kafka servers cooperating.

### 7️⃣ Controller / Manager (ZooKeeper / KRaft)

**Manages metadata and cluster coordination.**

**Old Model:**

- ZooKeeper handled:
    - broker registration
    - leader election
    - metadata management

**New Model:**

- KRaft (Kafka Raft) replaces ZooKeeper
- Kafka-native consensus

**Responsibilities:**

- Track active brokers
- Track topics/partitions
- Leader election per partition
- Metadata consistency
- Broker health monitoring

**Mental Model:** Controller = Kafka's brain.

### 8️⃣ Consumer Group

**Group of consumers sharing work on a topic.**

- Each partition goes to **one consumer** in the group
- Kafka balances automatically
- Ensures parallel, non-duplicating processing

**Example:**

```
Topic: orders (3 partitions)
Consumer Group: payment-processors (3 consumers)

P1 → C1
P2 → C2
P3 → C3
```

If you add a 4th consumer, rebalancing occurs (C4 remains idle unless more partitions exist).

**Mental Model:** Consumer Group = load balancer for consumers.

### 9️⃣ Offset

**Position (cursor) in a partition log.**

- Consumers track their offset
- Allows replay from any point
- Stored in `__consumer_offsets` topic

**Mental Model:** Offset = bookmark in a log.

---

## How Events Are Distributed

### Producer → Partition Selection

```
If key is present:
  partition = hash(key) % num_partitions
  (all events with same key → same partition → ordered)

Else (no key):
  partition = round_robin % num_partitions
  (events distributed evenly, no ordering guarantee)
```

### Partition → Consumer

```
Kafka assigns partitions to consumers in the same group.
One partition → one consumer at a time.

Guarantee: No two consumers in same group read same partition.
```

**Mental Model:** Kafka distributes work via partition assignment + consumer group rebalancing.

---

## Event Chaining & Streaming

Kafka enables **event-driven architectures:**

```
Service A → Kafka → Service B → Kafka → Service C
(OrderCreated) (InventoryChecked) (PaymentProcessed)
```

**Flow:**

1. Service A produces `OrderCreated` → Topic: `orders`
2. Service B consumes `orders`, processes, produces `InventoryChecked` → Topic: `inventory`
3. Service C consumes `inventory`, processes, produces `PaymentProcessed` → Topic: `payments`

Each service:

- Consumes an event
- Processes independently
- Produces next event

**Kafka is the backbone** that decouples services and enables replay.

---

## Component Summary Table

| Component                 | Purpose                         |
|---------------------------|---------------------------------|
| **Producer**              | Writes events into Kafka        |
| **Consumer**              | Reads events from Kafka         |
| **Topic**                 | Logical event stream            |
| **Partition**             | Physical shard of a topic       |
| **Broker**                | Kafka server node               |
| **Cluster**               | Group of brokers                |
| **Controller (KRaft/ZK)** | Metadata + coordination         |
| **Consumer Group**        | Parallel consumers sharing work |
| **Offset**                | Read position in partition      |

---

## Core Mental Picture

```
Producer → Topic → Partition → Broker → Cluster
                    ↓
                Consumer Group
                    ↓
                Offset tracking
```

**Ultra Short Foundation:**

Kafka is a distributed log system where producers write events into partitioned topics stored on brokers in a cluster,
and consumer groups read those partitions in parallel while the controller manages metadata, replication, and failover.

---

## Kafka with Java - Implementation Guide

### 1. Add Maven Dependencies

```xml
<!-- Kafka Producer & Consumer -->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.0</version>
</dependency>

        <!-- JSON Serialization (optional) -->
<dependency>
<groupId>com.fasterxml.jackson.core</groupId>
<artifactId>jackson-databind</artifactId>
<version>2.15.2</version>
</dependency>
```

### 2. Producer Implementation

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class OrderProducer {

    static void main(String[] args) {
        // Configuration
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Reliability options
        props.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);  // Batch messages

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

            // Send synchronously (blocking)
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    "orders",  // topic
                    "order-123",  // key (determines partition)
                    "{\"orderId\": \"123\", \"amount\": 99.99}"  // value
            );

            RecordMetadata metadata = producer.send(record).get();
            System.out.println("Sent to partition: " + metadata.partition()
                    + " offset: " + metadata.offset());

            // Or send asynchronously (non-blocking)
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        exception.printStackTrace();
                    } else {
                        System.out.println("Success! Partition: " + metadata.partition());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. Consumer Implementation

```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class OrderConsumer {

    static void main(String[] args) {
        // Configuration
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-processors");  // Consumer group
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Offset behavior
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // Start from beginning if no offset
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");  // Auto-commit offset
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {

            // Subscribe to topics
            consumer.subscribe(List.of("orders"));
            System.out.println("Subscribed to: orders");

            // Poll for messages
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(
                            "Topic: " + record.topic() +
                                    " | Partition: " + record.partition() +
                                    " | Offset: " + record.offset() +
                                    " | Key: " + record.key() +
                                    " | Value: " + record.value()
                    );

                    // Process business logic
                    processOrder(record.value());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processOrder(String orderJson) {
        System.out.println("Processing: " + orderJson);
        // Business logic here
    }
}
```

### 4. Consumer with Manual Offset Management

```java
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");  // Disable auto-commit

while(true){
ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for(
ConsumerRecord<String, String> record :records){
        try{

processOrder(record.value());  // Business logic

        // Commit only after successful processing
        consumer.

commitSync();
        }catch(
Exception e){
        System.err.

println("Failed to process: "+e.getMessage());
        // Don't commit, message will be retried
        }
        }
        }
```

### 5. Kafka Configuration Cheat Sheet

**Producer Config:**

```java
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.

put(ProducerConfig.ACKS_CONFIG, "all");  // 0, 1, or all
props.

put(ProducerConfig.RETRIES_CONFIG, 3);
props.

put(ProducerConfig.LINGER_MS_CONFIG, 10);  // Batch wait time
props.

put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
props.

put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
```

**Consumer Config:**

```java
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.

put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
props.

put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
props.

put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
props.

put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
props.

put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
props.

put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
```

---

## Kafka vs RabbitMQ - Design & Philosophy

| Aspect               | Kafka                               | RabbitMQ                             |
|----------------------|-------------------------------------|--------------------------------------|
| **Core Model**       | Distributed log / event stream      | Message broker / queue               |
| **Durability**       | Persistent by default (disk-backed) | Durable queues (optional)            |
| **Replay**           | ✅ Built-in (offset-based)           | ❌ Requires custom logic              |
| **Ordering**         | ✅ Per partition                     | ❌ No ordering guarantee              |
| **Consumer Model**   | Pull (consumers ask for messages)   | Push (broker sends to consumers)     |
| **Scaling**          | ✅ Partitions scale horizontally     | ⚠️ Queue on single broker bottleneck |
| **Throughput**       | ✅ High (millions msgs/sec)          | ⚠️ Moderate                          |
| **Latency**          | ⚠️ Higher (batch-oriented)          | ✅ Lower (single message)             |
| **Message Deletion** | Time/size-based retention           | Deleted after ack                    |
| **Use Case**         | Event-driven, streaming, audit logs | Task queues, RPC, decoupling         |
| **Complexity**       | ⚠️ Complex setup, many concepts     | ✅ Simple, straightforward            |
| **Topic Model**      | ✅ Multiple subscribers per topic    | ⚠️ Queues are single-consumer        |

### Design Philosophy

**Kafka = Event Store**

- Treats messages as **immutable events**
- Focuses on **streaming & replay**
- Designed for **scale-out** (partitions)
- Multiple consumers can read same event
- Data lives in Kafka (retention policy)

**RabbitMQ = Message Broker**

- Treats messages as **work to be done**
- Focuses on **reliable delivery**
- Designed for **decoupling services**
- Message deleted after processing
- Data passes through RabbitMQ

### When to Use What

**Use Kafka for:**

- Event-driven architectures
- Real-time streaming pipelines
- Audit logs & event sourcing
- Multi-consumer scenarios
- High-throughput, low-latency requirements
- Replay capabilities needed

**Use RabbitMQ for:**

- Simple pub/sub messaging
- Task/job queues
- Delayed/scheduled messages
- Complex routing logic
- Low message volume
- Simple operational setup

### Example: Same Problem, Different Approaches

**Scenario:** Order placed → Update inventory → Send email → Update analytics

**Kafka Approach:**

```
OrderService
  └─ produce: OrderPlaced → topic: orders

InventoryService
  ├─ consume: orders
  ├─ process
  └─ produce: InventoryUpdated → topic: inventory-updates

EmailService
  ├─ consume: orders
  ├─ send email
  └─ no produce

AnalyticsService
  ├─ consume: orders
  ├─ consume: inventory-updates
  └─ aggregate & store

Result: 
✅ All services can replay
✅ All can process independently in parallel
✅ New services can subscribe anytime
✅ Orders kept forever (retention policy)
```

**RabbitMQ Approach:**

```
OrderService
  └─ send: order → exchange: orders

Exchange routes to:
  ├─ Queue: inventory → InventoryService
  ├─ Queue: email → EmailService
  └─ Queue: analytics → AnalyticsService

Result:
✅ Simple routing
✅ Decoupled services
❌ No replay (message deleted after ack)
❌ Analytics service added later = misses past orders
❌ Hard to add new subscribers after the fact
```

---

## Before You Spin Containers, Remember

You only need to start with:

- ✅ 1 broker
- ✅ 1 topic
- ✅ 1 producer
- ✅ 1 consumer

Everything else (replication, multiple brokers, partitions) is for **scale and reliability**, not core functionality.

**Minimal Docker Compose:**

```yaml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

## Key Takeaways for Revision

1. **Kafka = Distributed Log**, not a message queue
2. **Partitions = Parallelism**, not topics
3. **Consumer Groups = Load Balancing**, not individual consumers
4. **Offsets = Replay**, not auto-delete
5. **Brokers = Commodity Servers**, stateless (state in partitions/disk)
6. **Controller = Metadata Manager**, not a performance bottleneck
7. **Producer chooses partition by key**, not randomly
8. **One partition = one consumer per group**, not many
9. **Kafka persists forever** (by retention policy), not briefly
10. **Event-driven > Request-Response** for streaming systems

---

## Quick Reference Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    KAFKA CLUSTER                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │  BROKER 1              BROKER 2    BROKER 3     │   │
│  │  ┌────────────┐        ┌────────┐  ┌────────┐  │   │
│  │  │ Partition  │        │ Part.  │  │ Part.  │  │   │
│  │  │ P1 (Leader)│        │ P1(Rep)│  │ P1(Rep)│  │   │
│  │  │ Log: 0-999 │        └────────┘  └────────┘  │   │
│  │  └────────────┘                                 │   │
│  │  ┌────────────┐        ┌────────┐              │   │
│  │  │ Partition  │        │ Part.  │              │   │
│  │  │ P2 (Rep)   │        │ P2(L)  │              │   │
│  │  │ Log: 0-999 │        └────────┘              │   │
│  │  └────────────┘                                 │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CONTROLLER (KRaft / ZooKeeper)                 │   │
│  │  • Broker metadata                              │   │
│  │  • Partition leadership                         │   │
│  │  • Rebalancing                                  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  __consumer_offsets (Internal Topic)            │   │
│  │  • Consumer group offsets per partition         │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘

    ↑ WRITE                           ↓ READ
    │                                 │
    │                                 │
┌─────────────┐                 ┌──────────────────┐
│  PRODUCER   │                 │ CONSUMER GROUP   │
│  (P1, key)  │                 │ [C1, C2, C3]     │
│  → Topic    │                 │ Offset tracking  │
│  → Partition│                 │ Auto-commit      │
└─────────────┘                 └──────────────────┘
```

---

## Final Summary

**Kafka** is a distributed event streaming platform built on append-only partitioned logs, enabling multiple independent
consumers to read the same events in parallel, replay from any offset, and process at scale with strong ordering
guarantees per partition. Use it for event-driven architectures, streaming pipelines, and systems requiring replay
capabilities.

**RabbitMQ** is a traditional message broker focused on reliable, simple message delivery with routing and queuing, best
for task processing and service decoupling where messages are consumed once and deleted.

Choose based on your architecture: **events (Kafka) or tasks (RabbitMQ)**.
