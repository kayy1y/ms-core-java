package com.skillgrow.core.domain.exception;

/** 503 SERVICE_UNAVAILABLE: un servicio del que dependemos (MS2) no respondió. */
public class ServiceUnavailableException extends DomainException {

    public ServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", message);
    }
}
