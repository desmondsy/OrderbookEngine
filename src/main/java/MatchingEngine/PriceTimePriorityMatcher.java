package MatchingEngine;

import Orderbook.Orderbook;
import Orders.Limit;
import Orders.Order;
import Orders.Trade;

import java.util.ArrayList;
import java.util.List;

public class PriceTimePriorityMatcher implements IOrderMatcher {
    @Override
    public List<Trade> matchLimitBuy(Order o, Orderbook ob) {
        Limit bestAskLimit = ob.getBestAskLimit();
        Order ptr = bestAskLimit.getHead();
        List<Trade> trades = new ArrayList<>();

        if (o.getPrice() >= bestAskLimit.getLimitPrice())
        {
            // traverse through the orders best ask limit level
            while (ptr != null)
            {
                if (o.getQuantity() >= ptr.getQuantity())
                {
                    trades.add(new Trade(o.getPrice(), o.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                    o.setQuantity(o.getQuantity() - ptr.getQuantity());
                    ob.removeOrder(ptr.getOrderId());
                    ptr = ptr.getNextOrder();
                }
                else
                {
                    trades.add(new Trade(ptr.getPrice(), ptr.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                    o.setQuantity(o.getQuantity() - ptr.getQuantity());
                    ob.removeOrder(ptr.getOrderId());
                    if (o.getQuantity() == 0)
                    {
                        return trades;
                    }
                }
            }
            // ptr is null (the limit is depleted, hence add the remainder quantity at far touch)
            bestAskLimit.setHead(o);
            bestAskLimit.setTail(o);
        }
        return trades;
    }

    @Override
    public List<Trade> matchLimitSell(Order o, Orderbook ob) {
        return null;
    }

    @Override
    public List<Trade> matchMarketBuy(Order o, Orderbook ob) {
        return null;
    }

    @Override
    public List<Trade> matchMarketSell(Order o, Orderbook ob) {
        return null;
    }
}
