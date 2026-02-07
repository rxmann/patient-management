package com.pm.patientservice.grpc;

import billing.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    public BillingServiceGrpcClient(@Value("${billing.service.address:localhost}") String serverAddress, @Value("${billing.service.grpc.port:7001}") int serverPort) {

        try {
            log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();
            blockingStub = BillingServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            log.error("Failed to initialize gRPC client", e);
            throw e;
        }
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId).setName(name).setEmail(email).build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via GRPC: {}", response);
        return response;
    }

    public double addTwoNumbers(Double a, Double b) {
        AddParams addParams = AddParams.newBuilder().setA(a).setB(b).build();
        AddResponse result = blockingStub.add(addParams);
        log.debug("Received response from billing service via GRPC: {}", result);
        return result.getResult();
    }

}
