package Simulation;

import MatchingEngine.OrderMatcherFactory;
import Orderbook.Orderbook;

import java.io.IOException;

public class RunSimulation {
    public static void main(String[] args) {
        SimulationConfig simulationConfig = new SimulationConfig("simulationConfig.properties");
        Orderbook ob = new Orderbook(OrderMatcherFactory.createOrderMatcher(simulationConfig.getMatchingEngine()));
        OrderbookSimulator simulator = new OrderbookSimulator(ob, simulationConfig);

        simulator.simulate();
    }
}
