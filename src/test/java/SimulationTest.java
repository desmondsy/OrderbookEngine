import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;
import Simulation.OrderbookSimulator;
import Simulation.SimulationConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class SimulationTest {

    @Test
    public void testRunSimulation()
    {
        Random random = new Random(345);
        SimulationConfig simulationConfig = new SimulationConfig("simulationConfig-test.properties"); // files in src/resources/test take precedence
        Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher(simulationConfig.getMatchingEngine()), random);
        OrderbookSimulator simulator = new OrderbookSimulator(ob, simulationConfig, random);

        simulator.run();

        Assertions.assertEquals(ob.getTotalBidSize(), 23892);
        Assertions.assertEquals(ob.getTotalAskSize(), 41517);
        Assertions.assertEquals(ob.getBestAsk(), 99);
        Assertions.assertEquals(ob.getBestBid(), 98);
        Assertions.assertEquals(ob.getBestAskSize(), 533);
        Assertions.assertEquals(ob.getBestBidSize(), 111);
    }
}
