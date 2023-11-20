import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
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
        List<Order> orderList = OrderGeneratorTest.readOrdersFromFile("MultipleLimits.txt");
        System.out.println("--------------------ADDING--------------------");

        for (Order o : orderList) {
            ob.addOrder(o);
        }
    }

    @Test
    public void testMatchLargeMarketBuyOrder()
    {
        ob.printOrderbook();

        // buy market order
        ob.addOrder(new Order(0, Side.BUY, 3250, null));

        ob.printOrderbook();

        Assertions.assertEquals(ob.getOrderMap().get(18).getQuantity(), 50);
    }

    @Test
    public void testMatchLargeMarketSellOrder()
    {
        ob.printOrderbook();

        // sell market order
        ob.addOrder(new Order(0, Side.SELL, 2800, null));

        ob.printOrderbook();

        Assertions.assertEquals(ob.getOrderMap().get(18).getQuantity(), 100);
    }

    @Test
    public void testMatchAggressiveLimitBuyOrder()
    {
        ob.printOrderbook();

        // aggressive limit buy
        ob.addOrder(new Order(0, Side.BUY, 350, 11d));

        ob.printOrderbook();
    }

    @Test
    public void testMatchAggressiveMultipleLimitsBuyOrder()
    {
        ob.printOrderbook();

        // aggressive limit buy
        ob.addOrder(new Order(0, Side.BUY, 3300, 11d));

        ob.printOrderbook();
    }
}
