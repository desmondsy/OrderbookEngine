package Orders;

import lombok.Getter;
import lombok.Setter;

public class Limit {
    private double price;
    @Getter @Setter private Order head = null;
    @Getter @Setter private Order tail = null;
    @Getter private int limitPrice;
    @Getter private int totalVolumeAtLimit;
    private int size;

    private Limit parentNode;
    private Limit left;
    private Limit right;

    public boolean isEmpty() {
        return head == null && tail == null;
    }

    // check if this limit is a buy or sell limit
    public Side getLimitSide() {
        return head.getSide() == Side.BUY ? Side.BUY : Side.SELL;
    }
}
