package com.function.handlers;

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
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderHandler {
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }

    @FunctionName("GetAllOrders")
    public HttpResponseMessage getAllOrders(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            route = "orders",
            authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
        @CosmosDBInput(
            name = "orders",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString",
            sqlQuery = "SELECT * FROM orders")
            List<Order> orders,
        final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK).body(orders).build();
    }

    @FunctionName("CreateOrder")
    public HttpResponseMessage createOrder(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "customers/{customerId}/orders",
            authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Order> request,
        @BindingName("customerId") UUID customerId,
        @CosmosDBOutput(
            name = "orders",
            databaseName = "%DatabaseName%",
            collectionName = "%CollectionName%",
            connectionStringSetting = "CosmosConnectionString")
            OutputBinding<Order> orderOutput,
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
}
