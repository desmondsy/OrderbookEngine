package MatchingEngine;

import Orderbook.Orderbook;
import Orders.Order;
import Orders.Trade;

import java.util.List;

public interface IOrderMatcher {
    List<Trade> matchLimitBuy(Order o, Orderbook ob);
    List<Trade> matchLimitSell(Order o, Orderbook ob);
    List<Trade> matchMarketBuy(Order o, Orderbook ob);
    List<Trade> matchMarketSell(Order o, Orderbook ob);
}
