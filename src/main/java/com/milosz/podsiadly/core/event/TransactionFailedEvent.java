package com.milosz.podsiadly.core.event;

import org.springframework.context.ApplicationEvent;

public class TransactionFailedEvent extends ApplicationEvent {
    private final Long transactionId;
    private final String transactionRef;
    private final String reason;

    public TransactionFailedEvent(Object source, Long transactionId, String transactionRef, String reason) {
        super(source);
        this.transactionId = transactionId;
        this.transactionRef = transactionRef;
        this.reason = reason;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public String getReason() {
        return reason;
    }
}