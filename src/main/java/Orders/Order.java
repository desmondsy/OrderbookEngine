package Orders;

import lombok.Getter;
import lombok.Setter;

public class Order {
    @Getter private long timestamp;
    @Getter private int orderId;
    @Getter private int securityId;
    @Getter private Side side;
    @Getter @Setter private int quantity; // setter for quantity matching
    @Getter private Double price;
    @Getter private Limit parentLimit;
    @Getter private ORDER_TYPE ordType = ORDER_TYPE.MARKET;
    @Getter private boolean isBuy;

    @Getter @Setter private Order nextOrder;
    @Getter @Setter private Order prevOrder;

    public Order(long timestamp, int orderId, int securityId, Side side, int quantity, Double price){
        this.timestamp = timestamp;
        this.orderId = orderId;
        this.securityId = securityId;
        this.side = side;
        this.quantity = quantity;
        if (price != null)
        {
            this.price = price;
            this.parentLimit = new Limit(price);
            this.ordType = ORDER_TYPE.LIMIT;
        }
        this.isBuy = side == Side.BUY;
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
