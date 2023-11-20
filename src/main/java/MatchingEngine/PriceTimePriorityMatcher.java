package MatchingEngine;

import Orderbook.Orderbook;
import Orders.*;

import java.util.ArrayList;
import java.util.TreeSet;

class PriceTimePriorityMatcher implements IOrderMatcher {
    ArrayList<Trade> trades = new ArrayList<>();

    @Override
    public void matchMarketOrder(Order o, Orderbook ob)
    {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();

        for (Limit limit: limitTree)
        {
            Order ptr = limit.getHead();

            // only iterate limit levels that have at least one order in it.
            // e.g. if we are 100/101 and then we deplete 101 with a market order, the 101 limit is technically still here
            // but will be null (ptr = null). So we don't do any matching on that null level
            while(ptr!=null && o.getQuantity() > 0)
            {
                if (o.getQuantity() >= ptr.getQuantity())
                {
                    trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                    o.setQuantity(o.getQuantity() - ptr.getQuantity());
                    ob.removeOrder(ptr.getOrderId());
                    ptr = ptr.getNextOrder();
                }
                else
                {
                    trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                    ptr.setQuantity(ptr.getQuantity() - o.getQuantity());
                    o.setQuantity(0);
                    if (ptr.getQuantity() == 0)
                        ob.removeOrder(ptr.getOrderId());
                }
            }

            if (o.getQuantity() <= 0)
            {
                ob.removeOrder(o.getOrderId());
                break;
            }

            // if we reach here, we have depleted the current limit level. E.g order for 1000 but best ask is < 1000.
            // We continue as is to the next level, should be no issues.

        }

        if (o.getQuantity() > 0)
        {
            // ideally there should be no order quantity remaining after going through ALL the limit levels
            // the initial total size condition should prevent us from reaching this block.
            // raise exception if we somehow reach here.
        }
    }

    @Override
    public void matchAggressiveLimitOrder(Order o, Orderbook ob)
    {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        double farTouchPrice = o.isBuy() ? ob.getBestAsk() : ob.getBestBid();

        for (Limit limit: limitTree)
        {
            if (limit.getPrice() == farTouchPrice)
            {
                Order ptr = limit.getHead();

                while(ptr!=null)
                {
                    if (o.getQuantity() >= ptr.getQuantity())
                    {
                        trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                        o.setQuantity(o.getQuantity() - ptr.getQuantity());
                        ob.removeOrder(ptr.getOrderId());
                        ptr = ptr.getNextOrder();
                    }
                    else
                    {
                        trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getQuantity(), ptr.getOrderId(), o.getOrderId()));
                        o.setQuantity(o.getQuantity() - ptr.getQuantity());
                        ob.removeOrder(ptr.getOrderId());
                        if (o.getQuantity() <= 0)
                        {
                            return;
                        }
                    }
                }
                break;
            }

            // e.g. if we do an aggressive limit buy 500 qty on a book like 500 x 100/101 x 200, any remaining qty
            ob.addOrder(new Order(o.getSecurityId(), o.getSide(), o.getQuantity(), farTouchPrice));

        }
    }
}
