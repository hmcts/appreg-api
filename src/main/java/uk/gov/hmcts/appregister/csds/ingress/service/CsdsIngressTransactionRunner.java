package uk.gov.hmcts.appregister.csds.ingress.service;

import java.util.function.Supplier;

@FunctionalInterface
public interface CsdsIngressTransactionRunner {
    <T> T execute(Supplier<T> supplier);
}
