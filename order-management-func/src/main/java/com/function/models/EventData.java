package com.function.models;

import java.time.Instant;
import java.util.UUID;

public class EventData {
    public String id = UUID.randomUUID().toString();
    public String eventType = "OrderProcessed";
    public String subject = "OrderProcessed";
    public String eventTime = Instant.now().toString();
    public String dataVersion = "1.0";
    public Order data;

    public EventData(Order order) {
        data = order;
    }
}
