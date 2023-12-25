package Simulation;

import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;

import java.util.Random;

public class RunSimulation {
    public static void main(String[] args) {
        Random random = new Random(123);
        SimulationConfig simulationConfig = new SimulationConfig("simulationConfig.properties");
        Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher(simulationConfig.getMatchingEngine()), random);
        OrderbookSimulator simulator = new OrderbookSimulator(ob, simulationConfig, random);

        simulator.run();
    }
}
