package pt.isel.cn2026labels.services;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.server.services.FirestoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LabelsWorker implements MessageReceiver {
    private static final Logger logger = LoggerFactory.getLogger(LabelsWorker.class);

    private final LabelsApp labelsApp = new LabelsApp();
    private final Gson gson = new Gson();
    private final FirestorePublishService firestore = new FirestorePublishService();

    @Override
    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
        try {
            String payload = message.getData().toStringUtf8();
            logger.info("Message received from Pub/Sub: {}", payload);

            JsonObject jsonMessage = gson.fromJson(payload, JsonObject.class);

            if (!jsonMessage.has("requestId") || !jsonMessage.has("bucketName") || !jsonMessage.has("blobName")) {
                System.err.println("Invalid JSON format: " + payload);
                consumer.ack();
                return;
            }

            String requestId = jsonMessage.get("requestId").getAsString();
            String bucketName = jsonMessage.get("bucketName").getAsString();
            String blobName = jsonMessage.get("blobName").getAsString();

            logger.info("Processing image: {} (Request ID: {})", blobName, requestId);

            String gsURI = "gs://" + bucketName + "/" + blobName;

            // Executes the Vision API
            Map<String, Float> detectedData = labelsApp.detectLabels(gsURI);

            List<String> englishLabels = new ArrayList<>(detectedData.keySet());
            List<Float> confidenceScores = new ArrayList<>(detectedData.values());

            List<String> portugueseLabels = labelsApp.TranslateLabels(englishLabels);

            for (int i = 0; i < englishLabels.size(); i++) {
                logger.info("{} -> {} (Score: {})", englishLabels.get(i), portugueseLabels.get(i), confidenceScores.get(i));
            }
            firestore.saveReport(requestId, englishLabels, portugueseLabels, confidenceScores);

            // Ack that the messages was consumed
            logger.info("Ack sent");
            consumer.ack();

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            consumer.nack();
        }
    }
}