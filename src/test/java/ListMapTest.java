import Orderbook.ListMap;
import org.junit.jupiter.api.Test;

public class ListMapTest {
    @Test
    public void testListMapObject()
    {
        ListMap<Integer> lm = new ListMap<Integer>();
        lm.addItem(10);
        lm.addItem(111);
        lm.addItem(12);
        lm.addItem(13);
        lm.addItem(14);

        System.out.println(lm);

        lm.removeItem(10);

        System.out.println(lm);

        System.out.println(lm.chooseRandomItem());

    }
}
