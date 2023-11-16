package MatchingEngine;

import Orderbook.Orderbook;
import Orders.Order;
import Orders.Trade;

import java.util.List;

public interface IOrderMatcher {
    void matchMarketOrder(Order o, Orderbook ob);

    void matchAggressiveLimitOrder(Order o, Orderbook ob);
}
