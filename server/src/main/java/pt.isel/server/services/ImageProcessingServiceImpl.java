package pt.isel.server.services;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.isel.cn2026labels.contracts.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static pt.isel.util.Config.BUCKET_NAME;

public class ImageProcessingServiceImpl extends ImageProcessingServiceGrpc.ImageProcessingServiceImplBase {

    private final PubSubPublisherService pubSubPublisher = new PubSubPublisherService();
    private final CloudStorageService storageService = new CloudStorageService();
    private final FirestoreService firestoreService = new FirestoreService();

    @Override
    public StreamObserver<ImageUploadChunk> submitImage(StreamObserver<SubmissionReference> responseObserver) {
        return new StreamObserver<>() {
            private WriteChannel channel = null;
            private String uniqueId = null;
            private String blobName = null;

            @Override
            public void onNext(ImageUploadChunk chunk) {
                try {
                    if (channel == null) {
                        uniqueId = UUID.randomUUID().toString();
                        blobName = uniqueId + "-" + chunk.getFileName();
                        channel = storageService.createUploadChannel(blobName);
                    }
                    channel.write(ByteBuffer.wrap(chunk.getDataPayload().toByteArray()));
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("GCP Error: " + e.getMessage()).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                if (channel != null) try { channel.close(); } catch (Exception ignored) {}
            }

            @Override
            public void onCompleted() {
                try {
                    if (channel != null) channel.close();

                    pubSubPublisher.publishTask(uniqueId, BUCKET_NAME, blobName);

                    responseObserver.onNext(SubmissionReference.newBuilder().setRequestId(uniqueId).build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Finalize error: " + e.getMessage()).asRuntimeException());
                }
            }
        };
    }

    @Override
    public void getImageAnalysisResults(SubmissionReference request, StreamObserver<ImageAnalysisReport> responseObserver) {
        try {
            ImageAnalysisReport report = firestoreService.getReportById(request.getRequestId());
            if (report == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Report not found or still processing").asRuntimeException());
                return;
            }
            responseObserver.onNext(report);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Database error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void findImagesByCriteria(FilteredSearchCriteria request, StreamObserver<MatchingFilesList> responseObserver) {
        try {
            List<String> files = firestoreService.findFilesByCriteria(request.getStartDate(), request.getEndDate(), request.getLabelKeyword());
            responseObserver.onNext(MatchingFilesList.newBuilder().addAllFileNames(files).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Database error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void downloadImage(DownloadRequest request, StreamObserver<ImageDownloadChunk> responseObserver) {
        try {
            Blob blob = storageService.getBlobByRequestId(request.getRequestId());

            if (blob == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Image not found in Cloud Storage.").asRuntimeException());
                return;
            }

            try (ReadChannel reader = blob.reader()) {
                // Chunks of 512KB
                ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);

                while (reader.read(buffer) > 0) {
                    buffer.flip();
                    responseObserver.onNext(ImageDownloadChunk.newBuilder()
                            .setDataPayload(com.google.protobuf.ByteString.copyFrom(buffer))
                            .setOriginalFileName(blob.getName())
                            .build());
                    buffer.clear();
                }
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error downloading image: " + e.getMessage()).asRuntimeException());
        }
    }
}