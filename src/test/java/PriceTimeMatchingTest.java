import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PriceTimeMatchingTest {
    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("pricetime"));

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
    public void testMatchLargeMarketBuyOrder()
    {
        ob.printOrderbookWithOrders();

        // buy market order
        ob.addOrder(new Order(0, Side.BUY, 3250, null));

        ob.printOrderbookWithOrders();

        Assertions.assertFalse(ob.getAskLimits().contains(new Limit(11)));
        Assertions.assertEquals(ob.getOrderMap().get(18).getCurrentQuantity(), 50);
        Assertions.assertEquals(ob.getOrderMap().get(18).getParentLimit().getTotalVolumeAtLimit(), 450);
        Assertions.assertEquals(ob.getTotalAskSize(), 950);
        Assertions.assertEquals(ob.getBestAsk(), 12);
        Assertions.assertEquals(ob.getBestBid(), 10);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
    }

    @Test
    public void testMatchLargeMarketSellOrder()
    {
        ob.printOrderbookWithOrders();

        // sell market order
        ob.addOrder(new Order(0, Side.SELL, 2800, null));

        ob.printOrderbookWithOrders();

        Assertions.assertFalse(ob.getAskLimits().contains(new Limit(10)));
        Assertions.assertFalse(ob.getAskLimits().contains(new Limit(9)));
        Assertions.assertEquals(ob.getOrderMap().get(13).getParentLimit().getTotalVolumeAtLimit(), 1100);
        Assertions.assertEquals(ob.getOrderMap().get(13).getCurrentQuantity(), 100);
        Assertions.assertEquals(ob.getBestAsk(), 11);
        Assertions.assertEquals(ob.getBestBid(), 7);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
    }

    @Test
    public void testMatchAggressiveLimitBuyOrder()
    {
        ob.printOrderbookWithOrders();

        // aggressive limit buy
        ob.addOrder(new Order(0, Side.BUY, 350, 11d));

        ob.printOrderbookWithOrders();
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
    }

    @Test
    public void testMatchAggressiveMultipleLimitsBuyOrder()
    {
        ob.printOrderbookWithOrders();

        // aggressive limit buy
        ob.addOrder(new Order(0, Side.BUY, 3300, 11d));

        ob.printOrderbookWithOrders();

        Assertions.assertEquals(ob.getBestAsk(), 12);
        Assertions.assertEquals(ob.getBestBid(), 11);
        Assertions.assertFalse(ob.getAskLimits().contains(new Limit(11)));
        Assertions.assertTrue(ob.getAskLimits().contains(new Limit(12)));
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
    }
}
