package pt.isel.server.services;

import com.google.api.gax.paging.Page;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;

import static pt.isel.util.Config.BUCKET_NAME;

public class    CloudStorageService {
    private final Storage storage;

    public CloudStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    // Creates and configures a WriteChannel for streaming data to GCP Storage.
    public WriteChannel createUploadChannel(String blobName) {
        BlobId blobId = BlobId.of(BUCKET_NAME, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/octet-stream")
                .build();

        WriteChannel channel = storage.writer(blobInfo);
        // Defined has 0.5MB chunks
        channel.setChunkSize(512 * 1024);

        return channel;
    }

    public Blob getBlobByRequestId(String requestId) {
        Page<Blob> blobs = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix(requestId + "-"));
        for (Blob blob : blobs.iterateAll()) {
            return blob;
        }
        return null;
    }
}
