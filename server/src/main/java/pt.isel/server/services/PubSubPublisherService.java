package pt.isel.server.services;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.TopicName;
import com.google.gson.JsonObject;
import com.google.api.core.ApiFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import static pt.isel.util.Config.PROJECT_ID;
import static pt.isel.util.Config.TOPIC_ID;

public class PubSubPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(PubSubPublisherService.class);

    public void publishTask(String requestId, String bucketName, String blobName) {
        TopicName topicName = TopicName.of(PROJECT_ID, TOPIC_ID);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();

            JsonObject jsonPayload = new JsonObject();
            jsonPayload.addProperty("requestId", requestId);
            jsonPayload.addProperty("bucketName", bucketName);
            jsonPayload.addProperty("blobName", blobName);

            ByteString data = ByteString.copyFromUtf8(jsonPayload.toString());
            com.google.pubsub.v1.PubsubMessage pubsubMessage =
                    com.google.pubsub.v1.PubsubMessage.newBuilder().setData(data).build();

            ApiFuture<String> future = publisher.publish(pubsubMessage);
            String messageId = future.get();

            logger.info("Message published to Pub/Sub with ID: {} for RequestId: {}", messageId, requestId);

        } catch (Exception e) {
            logger.error("Error publishing to Pub/Sub", e);
        } finally {
            if (publisher != null) {
                try {
                    publisher.shutdown();
                    publisher.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}