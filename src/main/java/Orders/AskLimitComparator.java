package Orders;

import java.util.Comparator;

public class AskLimitComparator implements Comparator<Limit> {
    @Override
    public int compare(Limit x, Limit y)
    {
        return Double.compare(x.getPrice(), y.getPrice());
    }
}
