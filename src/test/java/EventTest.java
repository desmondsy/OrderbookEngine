import Simulation.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventTest {

    @Test
    public void testIsAggressiveEvent()
    {
        Assertions.assertTrue(Event.AGGRESSIVE_BUY.isAggressiveEvent());
        Assertions.assertTrue(Event.AGGRESSIVE_SELL.isAggressiveEvent());
        Assertions.assertFalse(Event.PASSIVE_BUY.isAggressiveEvent());
        Assertions.assertFalse(Event.PASSIVE_SELL.isAggressiveEvent());
        Assertions.assertFalse(Event.MOD_BUY.isAggressiveEvent());
        Assertions.assertFalse(Event.MOD_SELL.isAggressiveEvent());
        Assertions.assertFalse(Event.CANCEL_BUY.isAggressiveEvent());
        Assertions.assertFalse(Event.CANCEL_SELL.isAggressiveEvent());
    }

    @Test
    public void testIsBuyEvent()
    {
        Assertions.assertTrue(Event.AGGRESSIVE_BUY.isBuyEvent());
        Assertions.assertFalse(Event.AGGRESSIVE_SELL.isBuyEvent());
        Assertions.assertTrue(Event.PASSIVE_BUY.isBuyEvent());
        Assertions.assertFalse(Event.PASSIVE_SELL.isBuyEvent());
        Assertions.assertTrue(Event.MOD_BUY.isBuyEvent());
        Assertions.assertFalse(Event.MOD_SELL.isBuyEvent());
        Assertions.assertTrue(Event.CANCEL_BUY.isBuyEvent());
        Assertions.assertFalse(Event.CANCEL_SELL.isBuyEvent());
    }

}
