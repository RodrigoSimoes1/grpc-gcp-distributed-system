package pt.isel.server.services;


import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import pt.isel.cn2026labels.contracts.ImageAnalysisReport;
import pt.isel.cn2026labels.contracts.LabelDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pt.isel.util.Config.*;

public class FirestoreService {
    private final Firestore db;
    public FirestoreService() {
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(PROJECT_ID)
                .setDatabaseId(DATABASE_ID)
                .build();
        this.db = firestoreOptions.getService();
    }

    public ImageAnalysisReport getReportById(String requestId) throws Exception {
        DocumentReference docRef = db.collection(FIRESTORE_COLLECTION).document(requestId);

        // Fetches the document from Firestore
        com.google.cloud.firestore.DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            // gRPC response object
            ImageAnalysisReport.Builder reportBuilder = ImageAnalysisReport.newBuilder()
                    .setRequestId(requestId)
                    .setProcessingDate(document.getString("processingDate"));

            // Extracts the labels list
            List<Map<String, Object>> labelsList = (List<Map<String, Object>>) document.get("labels");

            if (labelsList != null) {
                for (Map<String, Object> lbl : labelsList) {

                    Double score = (Double) lbl.getOrDefault("confidenceScore", 0.0);

                    reportBuilder.addDetectedLabels(LabelDetail.newBuilder()
                            .setTextEnglish((String) lbl.get("textEnglish"))
                            .setTextPortuguese((String) lbl.get("textPortuguese"))
                            .setConfidenceScore(score.floatValue())
                            .build());
                }
            }
            return reportBuilder.build();
        }

        return null;
    }

    public List<String> findFilesByCriteria(String startDate, String endDate, String labelKeyword) throws Exception {
        CollectionReference reports = db.collection(FIRESTORE_COLLECTION);

        // Inverts the format from dd-MM-yyyy to yyyy-MM-dd.
        String[] startParts = startDate.split("-");
        String[] endParts = endDate.split("-");

        String isoStartDate = startParts[0] + "-" + startParts[1] + "-" + startParts[2];
        String isoEndDate = endParts[0] + "-" + endParts[1] + "-" + endParts[2] + "T23:59:59Z";

        Query query = reports.whereGreaterThanOrEqualTo("processingDate", isoStartDate)
                .whereLessThanOrEqualTo("processingDate", isoEndDate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<String> matchingFiles = new ArrayList<>();

        String keyword = labelKeyword.trim().toLowerCase();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            boolean matchesKeyword = false;
            if (keyword.isEmpty()) {
                matchesKeyword = true;
            } else {
                List<Map<String, Object>> labelsList = (List<Map<String, Object>>) document.get("labels");

                if (labelsList != null) {
                    for (Map<String, Object> lbl : labelsList) {
                        String eng = (String) lbl.get("textEnglish");
                        String pt = (String) lbl.get("textPortuguese");

                        if ((eng != null && eng.toLowerCase().contains(keyword)) ||
                                (pt != null && pt.toLowerCase().contains(keyword))) {
                            matchesKeyword = true;
                            break;
                        }
                    }
                }
            }

            if (matchesKeyword) {
                String documentId = document.getId();
                matchingFiles.add(documentId);
            }
        }
        return matchingFiles;
    }
}