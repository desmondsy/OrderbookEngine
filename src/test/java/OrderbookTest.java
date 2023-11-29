import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OrderbookTest {
    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("pricetime"));

    @Test
    public void testAddRemoveModifyOrders()
    {
        List<Order> orderList = OrderGeneratorTest.readOrdersFromFile("data/MultipleLimits.txt");
        System.out.println("--------------------ADDING--------------------");

        for (Order o: orderList)
        {
            ob.addOrder(o);
        }

        ob.printOrderbook();

        // asserts here
        Assertions.assertEquals(ob.getTotalBidSize(), 4200);
        Assertions.assertEquals(ob.getTotalAskSize(), 4200);
        Assertions.assertEquals(ob.getOrderMap().get(0).getParentLimit().getTotalVolumeAtLimit(), 2100);
        Assertions.assertEquals(ob.getOrderMap().get(12).getParentLimit().getTotalVolumeAtLimit(), 600);
        Assertions.assertEquals(ob.getOrderMap().get(9).getParentLimit().getTotalVolumeAtLimit(), 3200);
        Assertions.assertEquals(ob.getOrderMap().get(19).getParentLimit().getTotalVolumeAtLimit(), 500);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        // remove
        System.out.println("--------------------REMOVING--------------------");
        ob.removeOrder(14, false);
        Assertions.assertFalse(ob.getBidLimits().contains(new Limit(6)));
        Assertions.assertEquals(ob.getTotalBidSize(), 3900);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.removeOrder(3, false);
        Assertions.assertEquals(ob.getOrderMap().get(2).getNextOrder().getOrderId(), 4);
        Assertions.assertEquals(ob.getOrderMap().get(4).getPrevOrder().getOrderId(), 2);
        Assertions.assertEquals(ob.getOrderMap().get(2).getParentLimit().getTotalVolumeAtLimit(), 1700);
        Assertions.assertEquals(ob.getTotalBidSize(), 3500);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.removeOrder(0, false);
        Assertions.assertNull(ob.getOrderMap().get(1).getPrevOrder());
        Assertions.assertEquals(ob.getOrderMap().get(1).getParentLimit().getTotalVolumeAtLimit(), 1600);
        Assertions.assertEquals(ob.getTotalBidSize(), 3400);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.removeOrder(11, false);
        Assertions.assertEquals(ob.getOrderMap().get(10).getNextOrder().getOrderId(), 22);
        Assertions.assertEquals(ob.getOrderMap().get(22).getPrevOrder().getOrderId(), 10);
        Assertions.assertEquals(ob.getOrderMap().get(10).getParentLimit().getTotalVolumeAtLimit(), 2600);
        Assertions.assertEquals(ob.getTotalAskSize(), 3600);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.printOrderbook();

        System.out.println("--------------------MODIFYING--------------------");

        // modify orderId 1 -> remove orderId 1 and create new order with the next available orderId (24) and price=9
        ob.modifyOrderPrice(1, 9);

        Assertions.assertFalse(ob.getOrderMap().containsKey(1));
        Assertions.assertTrue(ob.getOrderMap().containsKey(24));
        Assertions.assertEquals(ob.getOrderMap().get(24).getInitialQuantity(), 200);
        Assertions.assertEquals(ob.getOrderMap().get(24).getCurrentQuantity(), 200);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        // modify orderId 24 -> remove orderId 24 and create new order with the next available orderId (25) and qty=150
        ob.modifyOrderQty(24, 150);

        Assertions.assertFalse(ob.getOrderMap().containsKey(24));
        Assertions.assertTrue(ob.getOrderMap().containsKey(25));
        Assertions.assertEquals(ob.getOrderMap().get(25).getInitialQuantity(), 150);
        Assertions.assertEquals(ob.getOrderMap().get(25).getCurrentQuantity(), 150);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.printOrderbook();

        // remove
        ob.removeOrder(16, true);
        Assertions.assertFalse(ob.getOrderMap().containsKey(16));
        Assertions.assertEquals(ob.getOrderMap().get(12).getParentLimit().getTotalVolumeAtLimit(), 250);
        Assertions.assertTrue(ob.compareTotalBidAskVolumes());

        ob.printOrderbook();
    }
}
