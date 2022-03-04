package com.function.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {
    private UUID id;
    private UUID customerId;
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double tax;
    private double total;
    private LocalDateTime createdTimestamp;
    private LocalDateTime billedTimestamp;
    private LocalDateTime shippedTimestamp;

    public UUID getId()
}
