import Orderbook.ListMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class ListMapTest {
    @Test
    public void testListMapObject()
    {
        ListMap<Integer> lm = new ListMap<Integer>(new Random(123));
        lm.addItem(10);
        lm.addItem(111);
        lm.addItem(12);
        lm.addItem(13);
        lm.addItem(14);

        Assertions.assertEquals(lm.size(), 5);
        Assertions.assertEquals(lm.chooseRandomItem(), 12);

        lm.removeItem(10);

        Assertions.assertEquals(lm.size(), 4);
        Assertions.assertEquals(lm.chooseRandomItem(), 14);
    }
}
