package com.function.handlers;

import com.function.models.EventData;
import com.function.models.Order;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.CosmosDBInput;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.EventGridOutput;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderHandler {
    @FunctionName("GetAllOrders")
    public HttpResponseMessage getAllOrders(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            route = "orders",
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
        @CosmosDBInput(
            name = "orders",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString",
            sqlQuery = "SELECT * FROM orders") List<Order> orders,
        final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK).body(orders).build();
    }

    @FunctionName("CreateOrder")
    public HttpResponseMessage createOrder(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "customers/{customerId}/orders",
            authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Order> request,
        @BindingName("customerId") UUID customerId,
        @CosmosDBOutput(
            name = "orders",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString") OutputBinding<Order> orderOutput,
        final ExecutionContext context) {

        context.getLogger().info("New order created for customer " + customerId);
        final Order order = request.getBody();
        order.customerId = customerId;
        order.id = UUID.randomUUID();
        order.createdTimestamp = Instant.now().toString();

        orderOutput.setValue(order);

        return request.createResponseBuilder(HttpStatus.OK).body(order).build();
    }

    @FunctionName("GetCustomerOrders")
    public HttpResponseMessage getCustomerOrders(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            route = "customers/{customerId}/orders",
            authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
        @CosmosDBInput(
            name = "orders",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString",
            sqlQuery = "SELECT * FROM orders where orders.customerId = {customerId}")
            List<Order> orders,
        final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK).body(orders).build();
    }

    @FunctionName("ProcessOrderPayment")
    public void processOrderPayment(
        @CosmosDBTrigger(
            name = "ordersInput",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            leaseCollectionName = "leases",
            createLeaseCollectionIfNotExists = true,
            connectionStringSetting = "CosmosConnectionString") Order[] orders,
        @CosmosDBOutput(
            name = "ordersOutput",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString") OutputBinding<List<Order>> outputOrders,
        @EventGridOutput(
            name = "eventOutput",
            topicEndpointUri = "EventGridTopicEndpoint",
            topicKeySetting = "EventGridTopicKey") OutputBinding<List<EventData>> outputEvents,
        final ExecutionContext context ) {
        List<Order> updatedOrders = new ArrayList<Order>();
        List<EventData> events = new ArrayList<EventData>();

        for (Order order : orders) {
            if (order.paymentTimestamp == null) {
                order.tax = Math.round(order.quantity * order.unitPrice * 0.08 * 100.0) / 100.0;
                order.total = Math.round(order.quantity * order.unitPrice + order.tax * 100.0) / 100.0;
                order.paymentTimestamp = Instant.now().toString();
                context.getLogger().info("Processing payment for order " + order.id);

                updatedOrders.add(order);
                events.add(new EventData(order));
            }
        }

        outputOrders.setValue(updatedOrders);
        outputEvents.setValue(events);
    }

    @FunctionName("ShipOrder")
    public void shipOrder(
        @EventGridTrigger(name = "shipevent") EventData event,
        @CosmosDBOutput(
            name = "ordersOutput",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString") OutputBinding<Order> outputOrder,
        final ExecutionContext context ) {
        final Order order = event.data;
        order.shippedTimestamp = Instant.now().toString();

        context.getLogger().info("Shipping order " + order.id);

        outputOrder.setValue(order);
    }
}
