package Orders;

import lombok.Getter;

@Getter
public class Trade {
    private Side side;
    private double price;
    private int volume;
    private int makerId;
    private int takerId;

    public Trade(final Side side, final double price, final int volume, final int makerId, final int takerId) {
        this.side = side;
        this.price = price;
        this.volume = volume;
        this.makerId = makerId;
        this.takerId = takerId;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "side=" + side +
                ", price=" + price +
                ", volume=" + volume +
                ", makerId=" + makerId +
                ", takerId=" + takerId +
                '}';
    }
}
