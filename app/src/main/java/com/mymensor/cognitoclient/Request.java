package com.mymensor.cognitoclient;

/**
 * An abstract class for all the request classes used to communicate with sample
 * Cognito developer authentication
 */
public abstract class Request {

    /**
     * Builds and returns the request url
     */
    public abstract String buildRequestUrl();

}
