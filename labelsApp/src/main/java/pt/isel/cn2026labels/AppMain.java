package pt.isel.cn2026labels;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.cn2026labels.services.LabelsWorker;
import pt.isel.server.LabelsGrpcServer;


public class AppMain {
    public static final String PROJECT_ID = "cn2526-t1-g01";
    public static final String SUBSCRIPTION_ID = "cn2526labels-topic-sub";

    private static final Logger logger = LoggerFactory.getLogger(AppMain.class);



    public static void main(String[] args) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);

        LabelsWorker receiver = new LabelsWorker();
        Subscriber subscriber = null;

        try {
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
            subscriber.startAsync().awaitRunning();

            logger.info("Worker started. receiving messages from the subscription with id: " + SUBSCRIPTION_ID);

            subscriber.awaitTerminated();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (subscriber != null) {
                subscriber.stopAsync();
            }
        }
    }
}