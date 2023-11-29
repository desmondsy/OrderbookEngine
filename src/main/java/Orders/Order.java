package Orders;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Order {
    @Setter private long timestamp;
    @Setter private int orderId;
    private int securityId;
    private Side side;
    @Setter private int initialQuantity;
    @Setter private int currentQuantity;
    private Double price;
    @Setter private Limit parentLimit;
    private ORDER_TYPE ordType = ORDER_TYPE.MARKET;
    private boolean isBuy;

    @Setter private Order nextOrder;
    @Setter private Order prevOrder;

    public Order(int securityId, Side side, int quantity, Double price){
        this.securityId = securityId;
        this.side = side;
        this.initialQuantity = quantity;
        this.currentQuantity = quantity;
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
        this.initialQuantity = other.currentQuantity;
        this.currentQuantity = other.currentQuantity;
        this.price = price;
        this.parentLimit = new Limit(price);
        this.ordType = ORDER_TYPE.LIMIT;
    }

    public Order(Order other, int quantity)
    {
        this.securityId = other.securityId;
        this.side = other.side;
        this.isBuy = other.isBuy;
        this.initialQuantity = quantity;
        this.currentQuantity = quantity;
        this.price = other.price;
        this.parentLimit = other.parentLimit;
        this.ordType = ORDER_TYPE.LIMIT;
    }

    @Override
    public String toString() {
        return "Order(orderId=" + orderId + ", timestamp=" + timestamp + ", side=" + side + ", price=" + price + ", initialQty=" + initialQuantity + ", currentQty=" + currentQuantity + ", nextOrder=" + nextOrder + ")";
    }
}
