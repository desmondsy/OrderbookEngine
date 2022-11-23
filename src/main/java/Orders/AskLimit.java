package Orders;

public class AskLimit extends Limit implements Comparable<Limit>{
    @Override
    public int compareTo(Limit other)
    {
        return -Integer.compare(this.getLimitPrice(), other.getLimitPrice());
    }
}
