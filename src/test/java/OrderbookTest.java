import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Orders.Order;
import Orders.Side;
import org.junit.jupiter.api.Test;

public class OrderbookTest {
    Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher("pricetime"));

    @Test
    public void testAddOrder()
    {
        Order o1 = new Order(0, 0, 0, Side.BUY, 100, 10d);
        Order o2 = new Order(1, 1, 0, Side.BUY, 200, 10d);
        Order o3 = new Order(2, 2, 0, Side.BUY, 300, 10d);
        Order o4 = new Order(3, 3, 0, Side.BUY, 400, 10d);
        Order o5 = new Order(4, 4, 0, Side.BUY, 500, 10d);
        Order o6 = new Order(5, 5, 0, Side.BUY, 600, 10d);

        ob.addOrder(o1);
        ob.addOrder(o2);
        ob.addOrder(o3);
        ob.addOrder(o4);
        ob.addOrder(o5);
        ob.addOrder(o6);
    }
}
