package Orders;

import lombok.Getter;
import lombok.Setter;

public class Order {
    @Getter @Setter private long timestamp;
    @Getter @Setter private int orderId;
    @Getter private int securityId;
    @Getter private Side side;
    @Getter @Setter private int quantity; // setter for quantity matching
    @Getter private Double price;
    @Getter @Setter private Limit parentLimit;
    @Getter private ORDER_TYPE ordType = ORDER_TYPE.MARKET;
    @Getter private boolean isBuy;

    @Getter @Setter private Order nextOrder;
    @Getter @Setter private Order prevOrder;

    public Order(int securityId, Side side, int quantity, Double price){
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
    public Order(Order other, double price)
    {
        this.securityId = other.securityId;
        this.side = other.side;
        this.isBuy = other.isBuy;
        this.quantity = other.quantity;
        this.price = price;
        this.parentLimit = new Limit(price);
        this.ordType = ORDER_TYPE.LIMIT;
    }

    public Order(Order other, int quantity)
    {
        this.securityId = other.securityId;
        this.side = other.side;
        this.isBuy = other.isBuy;
        this.quantity = quantity;
        this.price = other.price;
        this.parentLimit = other.parentLimit;
        this.ordType = ORDER_TYPE.LIMIT;
    }

    @Override
    public String toString() {
        return "Order(orderId=" + orderId + ", timestamp=" + timestamp + ", price=" + price + ", qty=" + quantity + ", nextOrder=" + nextOrder + ")";
    }
}
