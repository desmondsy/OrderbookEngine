import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

public class ProRataWithTopMatchingTest {

    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("proratawithtop"), new Random(123));

    @BeforeEach
    public void setup()
    {
        // setup book
        List<Order> orderList = OrderGeneratorTest.readOrdersFromFile("data/MultipleLimits2.txt");
        System.out.println("--------------------ADDING--------------------");

        for (Order o : orderList) {
            ob.addOrder(o);
        }
    }

    @Test
    public void testPRWTMarketOrderMatching()
    {
        ob.printOrderbookWithOrders();

        ob.addOrder(new Order(0, Side.BUY, 6, null));

        ob.printOrderbookWithOrders();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(6).getCurrentQuantity(), 94);
        Assertions.assertEquals(ob.getOrderMap().get(7).getCurrentQuantity(), 200);
        Assertions.assertEquals(ob.getOrderMap().get(8).getCurrentQuantity(), 300);
        Assertions.assertEquals(ob.getOrderMap().get(9).getCurrentQuantity(), 400);
        Assertions.assertEquals(ob.getOrderMap().get(10).getCurrentQuantity(), 500);
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(29).getCurrentQuantity(), 500);
        Assertions.assertEquals(ob.getOrderMap().get(30).getCurrentQuantity(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(6).getParentLimit().getTotalVolumeAtLimit(), 3194);
    }

    @Test
    public void testPRWTMarketOrderMatching2()
    {
        ob.printOrderbookWithOrders();

        ob.addOrder(new Order(0, Side.BUY, 120, null));

        ob.printOrderbookWithOrders();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertFalse(ob.getOrderMap().containsKey(6));
        Assertions.assertEquals(ob.getOrderMap().get(7).getCurrentQuantity(), 195);
        Assertions.assertEquals(ob.getOrderMap().get(8).getCurrentQuantity(), 299);
        Assertions.assertEquals(ob.getOrderMap().get(9).getCurrentQuantity(), 398);
        Assertions.assertEquals(ob.getOrderMap().get(10).getCurrentQuantity(), 497);
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 597);
        Assertions.assertEquals(ob.getOrderMap().get(29).getCurrentQuantity(), 497);
        Assertions.assertEquals(ob.getOrderMap().get(30).getCurrentQuantity(), 597);

        Assertions.assertEquals(ob.getBestAsk(), 11);
        Assertions.assertEquals(ob.getBestBid(), 10);
    }

    @Test
    public void testPRWTMarketBuyOrderMatchingMultipleLimits()
    {
        ob.printOrderbookWithOrders();

        ob.addOrder(new Order(0, Side.BUY, 3100, null));

        ob.printOrderbookWithOrders();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertFalse(ob.getOrderMap().containsKey(6));
        Assertions.assertEquals(ob.getOrderMap().get(7).getCurrentQuantity(), 3);
        Assertions.assertEquals(ob.getOrderMap().get(8).getCurrentQuantity(), 10);
        Assertions.assertEquals(ob.getOrderMap().get(9).getCurrentQuantity(), 13);
        Assertions.assertEquals(ob.getOrderMap().get(10).getCurrentQuantity(), 17);
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 20);
        Assertions.assertEquals(ob.getOrderMap().get(29).getCurrentQuantity(), 17);
        Assertions.assertEquals(ob.getOrderMap().get(30).getCurrentQuantity(), 20);

//        Assertions.assertEquals(ob.getBestAsk(), 11);
//        Assertions.assertEquals(ob.getBestBid(), 10);
    }

    @Test
    public void testPRWTMarketSellOrderMatching()
    {
        ob.printOrderbookWithOrders();

        ob.addOrder(new Order(0, Side.SELL, 500, null));

        ob.printOrderbookWithOrders();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(1).getCurrentQuantity(), 160);
        Assertions.assertEquals(ob.getOrderMap().get(2).getCurrentQuantity(), 240);
        Assertions.assertEquals(ob.getOrderMap().get(3).getCurrentQuantity(), 320);
        Assertions.assertEquals(ob.getOrderMap().get(4).getCurrentQuantity(), 400);
        Assertions.assertEquals(ob.getOrderMap().get(5).getCurrentQuantity(), 480);

    }

    @Test
    public void testPRWTLimitBuyOrderMatchingMultipleLimits()
    {
        ob.printOrderbookWithOrders();

        ob.addOrder(new Order(0, Side.BUY, 3322, 11d));

        ob.printOrderbookWithOrders();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(31).getCurrentQuantity(), 122);
//        Assertions.assertEquals(ob.getOrderMap().get(8).getCurrentQuantity(), 289);
//        Assertions.assertEquals(ob.getOrderMap().get(9).getCurrentQuantity(), 385);
//        Assertions.assertEquals(ob.getOrderMap().get(10).getCurrentQuantity(), 481);
//        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 577);
//        Assertions.assertEquals(ob.getOrderMap().get(22).getCurrentQuantity(), 481);
//        Assertions.assertEquals(ob.getOrderMap().get(23).getCurrentQuantity(), 577);

//        Assertions.assertEquals(ob.getBestAsk(), 11);
//        Assertions.assertEquals(ob.getBestBid(), 10);
    }
}
