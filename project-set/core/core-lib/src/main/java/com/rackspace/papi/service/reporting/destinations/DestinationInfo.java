package com.rackspace.papi.service.reporting.destinations;

public interface DestinationInfo {

    void incrementRequestCount();
    void incrementResponseCount();
    void incrementStatusCodeCount(int statusCode);
    void accumulateResponseTime(long responseTime);    
    String getDestinationId();
    long getTotalRequests();
    long getTotalStatusCode(int statusCode);
    double getAverageResponseTime();
    double getThroughput();

    DestinationInfo copy();
}
