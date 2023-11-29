import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled
public class ProRataWithTopMatchingTest {

    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("proratawithtop"));

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
    public void testPRWTMarketOrderMatching()
    {
        ob.printOrderbook();

        ob.addOrder(new Order(0, Side.BUY, 6, null));

        ob.printOrderbook();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(6).getCurrentQuantity(), 94);
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(23).getCurrentQuantity(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(6).getParentLimit().getTotalVolumeAtLimit(), 3194);
    }

    @Test
    public void testPRWTMarketOrderMatching2()
    {
        ob.printOrderbook();

        ob.addOrder(new Order(0, Side.BUY, 120, null));

        ob.printOrderbook();

        ob.getMatchingEngine().printTrades();

        Assertions.assertTrue(ob.compareTotalBidAskVolumes());
        Assertions.assertEquals(ob.getOrderMap().get(11).getCurrentQuantity(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(23).getCurrentQuantity(), 600);
    }
}
