package pt.isel.util;

public class Config {
    // Google Cloud Project Details
    public static final String PROJECT_ID = "cn2526-t1-g01";

    // Firestore Details
    public static final String DATABASE_ID = "cn2526labels-db";
    public static final String FIRESTORE_COLLECTION = "image_details_translations";

    // Storage Details
    public static final String BUCKET_NAME = "cn2026labels-bucket";

    // Pub/Sub Details
    public static final String TOPIC_ID = "cn2526labels-topic";

    public static final String HTTP_IP_LOOKUP_CLOUD_FUNCTION = "https://cn-http-function-25832018080.europe-southwest1.run.app/";

    public static final String MIG_GRPC_NAME = "mig-server-labels";
    public static final String MIG_WORKER_NAME = "mig-app-labels";
    public static final String ZONE = "europe-southwest1-b";

    public static final String REGION = "europe-southwest1";
}