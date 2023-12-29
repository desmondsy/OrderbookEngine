package Simulation;

import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;

import java.util.Random;

public class RunSimulation {
    public static void main(String[] args) {
        int seed;
        String propertiesFile;

        try {
            propertiesFile = args[0]; // in src/main/resources
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            propertiesFile = "simulationConfig.properties";
        }

        try {
            seed = Integer.parseInt(args[1]);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            seed = 123;
        }

        Random random = new Random(seed);
        SimulationConfig simulationConfig = new SimulationConfig(propertiesFile);
        Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher(simulationConfig.getMatchingEngine()), random);
        OrderbookSimulator simulator = new OrderbookSimulator(ob, simulationConfig, random);

        simulator.run();
    }
}
