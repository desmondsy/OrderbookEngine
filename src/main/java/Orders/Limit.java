package Orders;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class Limit {
    @Getter private double price;
    @Getter @Setter private Order head = null;
    @Getter @Setter private Order tail = null;
    @Getter private int totalVolumeAtLimit;

    public Limit(double price) {
        // Each passive order points to a limit price. We need to create the limit if it doesnt yet exist in the orderbook.
        this.price = price;
    }

    // some helper methods
    public boolean isEmpty() {
        return head == null && tail == null;
    }

    @Override
    public String toString() {
        return "Limit [price=" + price + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Limit limit = (Limit) o;
        return Objects.equals(price, limit.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }
}
