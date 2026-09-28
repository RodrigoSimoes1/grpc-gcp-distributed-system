package pt.isel.client;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.cn2026labels.contracts.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LabelsGrpcClient {
    private final ImageProcessingServiceGrpc.ImageProcessingServiceStub asyncStub;
    private final ImageProcessingServiceGrpc.ImageProcessingServiceBlockingStub blockingStub;
    private final InfrastructureManagerGrpc.InfrastructureManagerBlockingStub infraBlockingStub;

    private static final Logger logger = LoggerFactory.getLogger(LabelsGrpcClient.class);


    public LabelsGrpcClient(ManagedChannel channel) {
        this.asyncStub = ImageProcessingServiceGrpc.newStub(channel);
        this.blockingStub = ImageProcessingServiceGrpc.newBlockingStub(channel);
        this.infraBlockingStub = InfrastructureManagerGrpc.newBlockingStub(channel);
    }

    public void submitImage(String imagePath) {
        Path path = Paths.get(imagePath);
        if (!Files.exists(path)) {
            System.out.println("Error: File does not exist at " + imagePath);
            return;
        }

        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<SubmissionReference> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(SubmissionReference ref) {
                logger.info("\nImage submitted! \nRequest ID: {}", ref.getRequestId());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("\nServer rejected submission: {}", t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        StreamObserver<ImageUploadChunk> requestObserver = asyncStub.submitImage(responseObserver);

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[512 * 1024];
            int bytesRead;
            logger.info("Uploading ");

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                requestObserver.onNext(ImageUploadChunk.newBuilder()
                        .setFileName(path.getFileName().toString())
                        .setDataPayload(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build());
                logger.info(".");
            }
            requestObserver.onCompleted();

            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                System.err.println("Upload timed out after 1 minute");
            }
        } catch (Exception e) {
            requestObserver.onError(e);
            finishLatch.countDown();
        }
    }

    public void getImageCharacteristicsById(String requestId) {
        try {
            SubmissionReference request = SubmissionReference.newBuilder().setRequestId(requestId).build();
            ImageAnalysisReport report = blockingStub.getImageAnalysisResults(request);

            logger.info("\nImage Report:");
            logger.info("Processing Date: {}", report.getProcessingDate());
            logger.info("Labels:");
            for (LabelDetail label : report.getDetectedLabelsList()) {
                String formattedLabel = String.format(" - %s -> %s (Confidence: %.2f%%)",
                        label.getTextEnglish(),
                        label.getTextPortuguese(),
                        label.getConfidenceScore() * 100);

                logger.info(formattedLabel);
            }
        } catch (StatusRuntimeException e) {
            logger.info("Could not retrieve report: {}", e.getStatus().getDescription());
        }
    }

    public void getStoredFilesGivenTwoDates(String startDate, String endDate, String labelKeyword) {
        try {
            FilteredSearchCriteria request = FilteredSearchCriteria.newBuilder()
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .setLabelKeyword(labelKeyword)
                    .build();

            MatchingFilesList response = blockingStub.findImagesByCriteria(request);

            logger.info("\nMatching Files");
            if (response.getFileNamesList().isEmpty()) {
                logger.info("No files found matching the criteria.");
            } else {
                for (String fileName : response.getFileNamesList()) {
                    logger.info(" - {}", fileName);
                }
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Error querying files: " + e.getStatus().getDescription());
        }
    }

    public void downloadImage(String requestId, String destinationFolder) {
        try {
            DownloadRequest request = DownloadRequest.newBuilder().setRequestId(requestId).build();

            logger.info("Requesting download for ID: {}...", requestId);
            Iterator<ImageDownloadChunk> chunks = blockingStub.downloadImage(request);

            Path destPath = null;
            OutputStream os = null;

            while (chunks.hasNext()) {
                ImageDownloadChunk chunk = chunks.next();

                if (os == null) {
                    String fileName = chunk.getOriginalFileName();
                    destPath = Paths.get(destinationFolder, fileName);
                    os = Files.newOutputStream(destPath);
                }

                os.write(chunk.getDataPayload().toByteArray());
                System.out.print(".");
            }

            if (os != null) {
                os.close();
                logger.info("\nImage successfully downloaded to: {}", destPath.toAbsolutePath());
            } else {
                logger.warn("\nNo data received from the server.");
            }

        } catch (StatusRuntimeException e) {
            logger.error("\nError downloading image: {}", e.getStatus().getDescription());
        } catch (Exception e) {
            logger.error("\nFile system error: {}", e.getMessage());
        }
    }

    public void scaleGrpcServers(int desiredCount) {
        try {
            logger.info("Sending request to scale gRPC servers to: {}...", desiredCount);
            ScalingAction action = ScalingAction.newBuilder().setDesiredInstanceCount(desiredCount).build();
            ScalingStatus status = infraBlockingStub.scaleGrpcServers(action);

            logger.info("Status: {}", status.getStatusMessage());
            logger.info("Active instance target: {}", status.getActiveInstances());
        } catch (StatusRuntimeException e) {
            logger.error("Error: {}", e.getStatus().getDescription());
        }
    }

    public void scaleWorkerNodes(int desiredCount) {
        try {
            logger.info("Sending request to scale workers to: {}...", desiredCount);
            ScalingAction action = ScalingAction.newBuilder().setDesiredInstanceCount(desiredCount).build();
            ScalingStatus status = infraBlockingStub.scaleWorkerNodes(action);

            logger.info("Status: {}", status.getStatusMessage());
            logger.info("Active instance target: {}", status.getActiveInstances());
        } catch (StatusRuntimeException e) {
            logger.error("Error: {}", e.getStatus().getDescription());
        }
    }
}