package com.function.models;

import java.util.UUID;

public class Order {
    public UUID id;
    public UUID customerId;
    public String itemName;
    public int quantity;
    public double unitPrice;
    public Double tax;
    public Double total;
    public String createdTimestamp;
    public String billedTimestamp;
    public String shippedTimestamp;
}
