package pt.isel.cn2026labels.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static pt.isel.util.Config.*;

public class FirestorePublishService {
    private static final Logger logger = LoggerFactory.getLogger(FirestorePublishService.class);

    private final Firestore db;

    public FirestorePublishService() {
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(PROJECT_ID)
                .setDatabaseId(DATABASE_ID)
                .build();

        this.db = firestoreOptions.getService();

        logger.info("FirestoreService initiated for the project [{}] and db [{}]",
                PROJECT_ID, DATABASE_ID);
    }

    public void saveReport(String requestId, List<String> englishLabels, List<String> portugueseLabels, List<Float> confidenceScores) throws Exception {
        DocumentReference docRef = db.collection(FIRESTORE_COLLECTION).document(requestId);

        // Data is stored in a HashMap
        Map<String, Object> data = new HashMap<>();

        String processingDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
        data.put("processingDate", processingDate);

        // The new list of labels
        List<Map<String, Object>> labelsList = getMaps(englishLabels, portugueseLabels, confidenceScores);

        data.put("labels", labelsList);

        // Async write operation
        ApiFuture<WriteResult> result = docRef.set(data);

        // Blocks the thread until the write operation is completed
        result.get();

        logger.info("New labels added to firestore! (ID: {})", requestId);
    }

    private static @NonNull List<Map<String, Object>> getMaps(List<String> englishLabels, List<String> portugueseLabels, List<Float> confidenceScores) {
        List<Map<String, Object>> labelsList = new ArrayList<>();
        for (int i = 0; i < englishLabels.size(); i++) {
            Map<String, Object> labelMap = new HashMap<>();
            labelMap.put("textEnglish", englishLabels.get(i));

            // Case when a translation fails
            if (i < portugueseLabels.size()) {
                labelMap.put("textPortuguese", portugueseLabels.get(i));
            } else {
                labelMap.put("textPortuguese", "N/A");
            }

            if (i < confidenceScores.size()) {
                labelMap.put("confidenceScore", confidenceScores.get(i));
            } else {
                labelMap.put("confidenceScore", 0.0f);
            }

            labelsList.add(labelMap);
        }
        return labelsList;
    }
}