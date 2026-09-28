package pt.isel.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.isel.server.services.ImageProcessingServiceImpl;

import java.io.IOException;

public class LabelsGrpcServer {
    private static final Logger logger = LoggerFactory.getLogger(LabelsGrpcServer.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(PORT)
                .addService(new ImageProcessingServiceImpl())
                .addService(new pt.isel.server.services.InfrastructureManagerImpl())
                .build()
                .start();

        logger.info("Server started, listening on " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            if (server != null) {
                server.shutdown();
            }
            logger.info("Server shut down");
        }));

        server.awaitTermination();
    }
}