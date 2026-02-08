package com.pm.billingservice.grpc;

import billing.AddParams;
import billing.AddResponse;
import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//Registers the service with the gRPC server (from Spring Boot gRPC starter)
@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(BillingRequest billingRequest, StreamObserver<BillingResponse> responseObserver) {
        log.info("CreateBillingAccount request received {} ", billingRequest.toString());
        // business logic - save to db, perform calculation etc
        BillingResponse billingResponse = BillingResponse.newBuilder().setAccountId("XAE451").setStatus("SUCCESS").build();
//        Sends a response
        responseObserver.onNext(billingResponse);
//        Closes the RPC session
        responseObserver.onCompleted();
    }

    @Override
    public void add(AddParams addParams, StreamObserver<AddResponse> responseObserver) {
        log.info("Add request received {} ", addParams.toString());
        double result = addParams.getA() + addParams.getB();
        AddResponse returnRes = AddResponse.newBuilder().setResult(result).build();
        responseObserver.onNext(returnRes);
        responseObserver.onCompleted();
    }

}
