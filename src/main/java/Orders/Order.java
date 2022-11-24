package Orders;

import lombok.Getter;
import lombok.Setter;

public class Order {
    @Getter private long timestamp;
    @Getter private int orderId;
    @Getter private int securityId;
    @Getter private Side side;
    @Getter private int quantity;
    @Getter private double price;
    @Getter private Limit parentLimit;
    @Getter @Setter private Order nextOrder;
    @Getter @Setter private Order prevOrder;

    public Order(long timestamp, int orderId, int securityId, Side side, int quantity, Limit parentLimit, Double price){
        this.timestamp = timestamp;
        this.orderId = orderId;
        this.securityId = securityId;
        this.side = side;
        this.quantity = quantity;
        this.parentLimit = parentLimit;
        if (price != null) {
            this.price = price;
        }
    }
    public Order(Order other, int orderId)
    {
        this.timestamp = other.timestamp;
        this.orderId = orderId;
        this.securityId = other.securityId;
        this.side = other.side;
        this.quantity = other.quantity;
        this.price = other.price;
        this.parentLimit = other.parentLimit;
    }
}
