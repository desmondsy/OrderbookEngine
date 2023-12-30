# OrderbookEngine

A fully functioning LOB + matching engine implementation written in Java. 3 basic matching algorithms are implemented (price-time, pro-rata, pro-rata with top) in the MatchingEngine package.

Orderbook implementation:

- In each limit, maintain a doubly linked list to store individual orders. Provides constant time addition and deletion of orders
- Each individual limit level is stored in a balanced binary tree (TreeSet). The orderbook maintains two of these trees, one for buy limits and one for sell limits.
- OrderId: Order HashMap
- O(1) access for bestBid/Ask, bestBidSize/AskSize, totalBidSize/AskSize

Matching implementation:

- IOrderMatcher interface - each matcher must implement matchMarketOrder and matchAggressiveLimitOrder. 
- matchMarketOrder can deplete multiple limit levels
- matchAggressiveLimitOrder can only deplete the far touch limit before creating a new passive order at the far touch for the remaining quantity.
- Price-Time: Standard price-time priority matching.
- Pro-Rata: https://atas.net/volume-analysis/basics-of-volume-analysis/cme-order-matching-algorithms-part-1/
- Threshold Pro-Rata: https://atas.net/volume-analysis/basics-of-volume-analysis/cme-order-matching-algorithms-part-3/

Simulator implementation:

- At any iteration, there are 8 events (`Event.java`) that the simulator can pick from with defined weights (in `eventProbabilitiesDefault.properties`)
- These 8 events are: `PASSIVE_BUY/SELL`, `AGGRESSIVE_BUY/SELL`, `MOD_BUY/SELL`, `CANCEL_BUY/SELL`.
- If an `AGGRESSIVE_BUY/SELL` event is selected, there is a 50% chance it will be a market order or an aggressive limit order.
- If any of the other 6 events are selected, we need to determine the limit price level for that event. E.g. If `PASSIVE_BUY` is chosen, we need to choose a bid price for the new passive order. To do this, an array of decaying probabilities is generated, where the ith element represents the probability we select a price level that is (i+1) ticks away from the opposite best quote (see `OrderbookSimulator.generateDecayingProbabilities`).
- The simulation is initialized with INIT_ITERATIONS (`simulationConfig.properties`) interations, where only `PASSIVE_BUY/SELL` events can be chosen to build up the orderbook. No matching/trades can occur during this phase. Afterwards, the simulation is run for ITERATIONS iterations.


To run:

1. Install maven
2. Run `mvn clean install -DskipTests assembly:single` to create an all in one jar.
3. Run `java -jar target/OrderbookEngine-1.0-SNAPSHOT-jar-with-dependencies.jar simulationConfig.properties 123`. The first argument is the config file which can be found in `src/main/resources`, and the second argument is a random seed value for the Random class. The defaults are simulationConfig.properties and 123 if no arguments are specified.
4. A log file will be generated for each run, located in `logs/`.

TODO:

Implement a light orderbook + BBO visualization program (python) based on the simulation log output

Improve the simulation - Poisson distribution to model time between events instead of using iterations (Watts/Delft paper in `pdfs/`)

Implement concurrent features
