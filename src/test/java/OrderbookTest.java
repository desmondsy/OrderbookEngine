import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OrderbookTest {
    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("pricetime"));

    public void printOrderbook()
    {
        System.out.println("BID LIMITS");
        for (Limit limit: ob.getBidLimits())
        {
            System.out.println(limit);
            Order ptr = limit.getHead();
            while (ptr != null)
            {
                System.out.println(ptr);
                ptr = ptr.getNextOrder();
            }
        }

        System.out.println("ASK LIMITS");
        for (Limit limit: ob.getAskLimits())
        {
            System.out.println(limit);
            Order ptr = limit.getHead();
            while (ptr != null)
            {
                System.out.println(ptr);
                ptr = ptr.getNextOrder();
            }
        }

        System.out.println("\n####\n");
    }

    @Test
    public void testAddRemoveModifyOrders()
    {
        List<Order> orderList = OrderGeneratorTest.readOrdersFromFile("MultipleLimits.txt");
        System.out.println("--------------------ADDING--------------------");

        for (Order o: orderList)
        {
            ob.addOrder(o);
        }

        printOrderbook();

        // asserts here
        Assertions.assertEquals(ob.getTotalBidSize(), 4200);
        Assertions.assertEquals(ob.getTotalAskSize(), 4200);
        Assertions.assertEquals(ob.getOrderMap().get(0).getParentLimit().getTotalVolumeAtLimit(), 2100);
        Assertions.assertEquals(ob.getOrderMap().get(12).getParentLimit().getTotalVolumeAtLimit(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(9).getParentLimit().getTotalVolumeAtLimit(), 3200);
        Assertions.assertEquals(ob.getOrderMap().get(19).getParentLimit().getTotalVolumeAtLimit(), 500);

        // remove
        System.out.println("--------------------REMOVING--------------------");
        ob.removeOrder(14);
        Assertions.assertFalse(ob.getBidLimits().contains(new Limit(6)));
        Assertions.assertEquals(ob.getTotalBidSize(), 3900);

        ob.removeOrder(3);
        Assertions.assertEquals(ob.getOrderMap().get(2).getNextOrder().getOrderId(), 4);
        Assertions.assertEquals(ob.getOrderMap().get(4).getPrevOrder().getOrderId(), 2);
        Assertions.assertEquals(ob.getOrderMap().get(2).getParentLimit().getTotalVolumeAtLimit(), 1700);
        Assertions.assertEquals(ob.getTotalBidSize(), 3500);

        ob.removeOrder(0);
        Assertions.assertNull(ob.getOrderMap().get(1).getPrevOrder());
        Assertions.assertEquals(ob.getOrderMap().get(1).getParentLimit().getTotalVolumeAtLimit(), 1600);
        Assertions.assertEquals(ob.getTotalBidSize(), 3400);

        ob.removeOrder(11);
        Assertions.assertEquals(ob.getOrderMap().get(10).getNextOrder().getOrderId(), 22);
        Assertions.assertEquals(ob.getOrderMap().get(22).getPrevOrder().getOrderId(), 10);
        Assertions.assertEquals(ob.getOrderMap().get(10).getParentLimit().getTotalVolumeAtLimit(), 2600);
        Assertions.assertEquals(ob.getTotalAskSize(), 3600);

        printOrderbook();

        System.out.println("--------------------MODIFYING--------------------");

        // modify
        ob.modifyOrderPrice(1, 9);
        ob.modifyOrderQty(24, 150);

        // remove
        ob.removeOrder(16);

        printOrderbook();
    }
}
