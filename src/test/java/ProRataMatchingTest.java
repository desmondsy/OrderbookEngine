import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ProRataMatchingTest {

    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("prorata"));

    @BeforeEach
    public void setup()
    {
        // setup book
        List<Order> orderList = OrderGeneratorTest.readOrdersFromFile("data/MultipleLimits.txt");
        System.out.println("--------------------ADDING--------------------");

        for (Order o : orderList) {
            ob.addOrder(o);
        }
    }

    @Test
    public void testProRataMarketOrderMatching()
    {
        ob.printOrderbook();

        ob.addOrder(new Order(0, Side.BUY, 6, null));

        ob.printOrderbook();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(6).getCurrentQuantity(), 96);
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 599);
        Assertions.assertEquals(ob.getOrderMap().get(23).getCurrentQuantity(), 599);
        Assertions.assertEquals(ob.getOrderMap().get(6).getParentLimit().getTotalVolumeAtLimit(), 3194);
    }

    @Test
    public void testProRataMarketOrderMatching2()
    {
        ob.printOrderbook();

        ob.addOrder(new Order(0, Side.BUY, 3200, null));

        ob.printOrderbook();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
//        Assertions.assertEquals(ob.getMatchingEngine().getTrades().size(), 8);
//        Assertions.assertEquals(ob.getBestAsk(), 12);
//        Assertions.assertEquals(ob.getBestBid(), 10);
    }

    @Test
    public void testProRataAggressiveLimitOrderMatching()
    {
        ob.printOrderbook();

        ob.addOrder(new Order(0, Side.BUY, 3201, 11d));

        ob.printOrderbook();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getMatchingEngine().getTrades().size(), 8);
        Assertions.assertEquals(ob.getBestAsk(), 12);
        Assertions.assertEquals(ob.getBestBid(), 11);
    }
}
