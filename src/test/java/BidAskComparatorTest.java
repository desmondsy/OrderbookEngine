import Orders.AskLimitComparator;
import Orders.BidLimitComparator;
import Orders.Limit;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BidAskComparatorTest {
    private TreeSet<Limit> askLimits;
    private TreeSet<Limit> bidLimits;

    @BeforeAll
    public void setup() {
        askLimits = new TreeSet<>(new AskLimitComparator());
        askLimits.add(new Limit(10));
        askLimits.add(new Limit(18));
        askLimits.add(new Limit(11));
        askLimits.add(new Limit(14));
        askLimits.add(new Limit(25));
        askLimits.add(new Limit(20));
        askLimits.add(new Limit(17));

        bidLimits = new TreeSet<>(new BidLimitComparator());
        bidLimits.add(new Limit(1));
        bidLimits.add(new Limit(3));
        bidLimits.add(new Limit(4));
        bidLimits.add(new Limit(7));
        bidLimits.add(new Limit(8));
        bidLimits.add(new Limit(9));
    }

    @Test
    public void testAskLimitsOrdering() {
        List<Double> expected = Arrays.asList(10d, 11d, 14d, 17d, 18d, 20d, 25d);
        List<Double> actual = new ArrayList<>();
        for (Limit l: askLimits)
        {
            System.out.println(l);
            actual.add(l.getPrice());
        }
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testBidLimitsOrdering() {
        List<Double> expected = Arrays.asList(9d, 8d, 7d, 4d, 3d, 1d);
        List<Double> actual = new ArrayList<>();
        for (Limit l: bidLimits)
        {
            System.out.println(l);
            actual.add(l.getPrice());
        }
        Assertions.assertEquals(expected, actual);
    }
}
