package Orders;

import lombok.Getter;

public class Trade {
    @Getter private Side side;
    @Getter private double price;
    @Getter private int volume;
    @Getter private int makerId;
    @Getter private int takerId;

    public Trade(final Side side, final double price, final int volume, final int makerId, final int takerId) {
        this.side = side;
        this.price = price;
        this.volume = volume;
        this.makerId = makerId;
        this.takerId = takerId;
    }
}
