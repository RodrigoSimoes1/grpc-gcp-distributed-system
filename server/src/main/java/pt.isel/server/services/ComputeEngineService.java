package pt.isel.server.services;

import com.google.cloud.compute.v1.RegionInstanceGroupManagersClient;
import com.google.cloud.compute.v1.InstanceGroupManager;
import com.google.cloud.compute.v1.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static pt.isel.util.Config.*;

public class ComputeEngineService {
    private static final Logger logger = LoggerFactory.getLogger(ComputeEngineService.class);

    public int resizeMig(boolean isGrpcServer, int desiredCount) throws Exception {
        String migName = isGrpcServer ? MIG_GRPC_NAME : MIG_WORKER_NAME;

        logger.info("Starting regional resizing of MIG '{}' for {} instances in region {}...", migName, desiredCount, REGION);

        try (RegionInstanceGroupManagersClient client = RegionInstanceGroupManagersClient.create()) {

            Operation operation = client.resizeAsync(PROJECT_ID, REGION, migName, desiredCount).get();

            if (operation.hasError()) {
                throw new RuntimeException("Error: " + operation.getError().getErrors(0).getMessage());
            }

            InstanceGroupManager igm = client.get(PROJECT_ID, REGION, migName);
            return igm.getTargetSize();
        }
    }
}