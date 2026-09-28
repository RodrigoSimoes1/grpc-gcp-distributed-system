package pt.isel.server.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.isel.cn2026labels.contracts.InfrastructureManagerGrpc;
import pt.isel.cn2026labels.contracts.ScalingAction;
import pt.isel.cn2026labels.contracts.ScalingStatus;

public class InfrastructureManagerImpl extends InfrastructureManagerGrpc.InfrastructureManagerImplBase {

    private final ComputeEngineService computeService = new ComputeEngineService();

    @Override
    public void scaleGrpcServers(ScalingAction request, StreamObserver<ScalingStatus> responseObserver) {
        try {
            int desiredCount = request.getDesiredInstanceCount();

            if (desiredCount < 0) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("The number can not be negative.")
                        .asRuntimeException());
                return;
            }

            int activeInstances = computeService.resizeMig(true, desiredCount);

            ScalingStatus status = ScalingStatus.newBuilder()
                    .setStatusMessage("Scaling for servers executed with success.")
                    .setActiveInstances(activeInstances)
                    .build();

            responseObserver.onNext(status);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error:" + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void scaleWorkerNodes(ScalingAction request, StreamObserver<ScalingStatus> responseObserver) {
        try {
            int desiredCount = request.getDesiredInstanceCount();

            if (desiredCount < 0) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("The number can not be negative.")
                        .asRuntimeException());
                return;
            }

            int activeInstances = computeService.resizeMig(false, desiredCount);

            ScalingStatus status = ScalingStatus.newBuilder()
                    .setStatusMessage("Scaling for workers executed with success.")
                    .setActiveInstances(activeInstances)
                    .build();

            responseObserver.onNext(status);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error:" + e.getMessage())
                    .asRuntimeException());
        }
    }
}