package MatchingEngine;

import Orderbook.Orderbook;
import Orders.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

class PriceTimePriorityMatcher extends AbstractOrderMatcher {
    @Override
    public void matchMarketOrder(Order o, Orderbook ob)
    {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getInitialQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        List<Integer> ordersToRemove = new ArrayList<>();

        for (Limit limit: limitTree)
        {
            Order ptr = limit.getHead();

            // only iterate limit levels that have at least one order in it.
            // e.g. if we are 100/101 and then we deplete 101 with a market order, the 101 limit is technically still here
            // but will be null (ptr = null). So we don't do any matching on that null level

            // whilst there are still orders to be matched in the current limit level AND market order still has excess unmatched qty
            while(ptr!=null && o.getCurrentQuantity() > 0)
            {
                if (o.getCurrentQuantity() >= ptr.getCurrentQuantity())
                {
                    // we are able to fill an entire resting order with possible excess, so we keep going
                    trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                    o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                    ordersToRemove.add(ptr.getOrderId()); // resting orders are fully matched
                    ptr = ptr.getNextOrder();
                }
                else
                {
                    trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                    int remainingQtyToFill = o.getCurrentQuantity();
                    limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - remainingQtyToFill);
                    if (o.isBuy())
                        ob.setTotalAskSize(ob.getTotalAskSize() - remainingQtyToFill);
                    else
                        ob.setTotalBidSize(ob.getTotalBidSize() - remainingQtyToFill);
                    ptr.setCurrentQuantity(ptr.getInitialQuantity() - remainingQtyToFill);
                    o.setCurrentQuantity(0); // market order is fully matched, we can exit the while loop
                }
            }

            // if we reach here, we have either depleted the current limit level OR the market order has been completely filled.
            // If the market order still has remaining unmatched quantity, we need to continue to the next best limit.
            if (o.getCurrentQuantity() == 0)
                break;

        }

        if (o.getCurrentQuantity() > 0)
        {
            // ideally there should be no order quantity remaining after going through ALL the limit levels
            // the initial total size condition should prevent us from reaching this block.
            // raise exception if we somehow reach here.
            throw new UnexpectedRemainingVolumeException("There shouldn't be any remaining quantity here.");
        }

        /*
        TODO: we currently have to store the orders we are going to remove during matching in a list instead of removing it
          directly. The reason is ob.removeOrder can remove a Limit from TreeSet<Limit>. When we iterate through the
          treeset and attempt to remove elements from it during iteration, we will run into a ConcurrentModificationException. Therefore, we
          instead keep track of what orders we need to remove, and loop through the list separately to avoid the issue.
          Not the most ideal... but does the job
         */
        for (Integer orderId: ordersToRemove)
        {
            ob.removeOrder(orderId);
        }
    }

    @Override
    public void matchAggressiveLimitOrder(Order o, Orderbook ob)
    {
        // logic is similar to matching a market order
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getInitialQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        List<Integer> ordersToRemove = new ArrayList<>();
        double farTouchPrice = o.isBuy() ? ob.getBestAsk() : ob.getBestBid();

        for (Limit limit: limitTree)
        {
            if (limit.getPrice() == farTouchPrice)
            {
                Order ptr = limit.getHead();

                while(ptr!=null && o.getCurrentQuantity() > 0)
                {
                    if (o.getCurrentQuantity() >= ptr.getCurrentQuantity())
                    {
                        trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                        o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                        ordersToRemove.add(ptr.getOrderId()); // resting orders are fully matched
                        ptr = ptr.getNextOrder();
                    }
                    else
                    {
                        trades.add(new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId()));
                        int remainingQtyToFill = o.getCurrentQuantity();
                        limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - remainingQtyToFill);
                        if (o.isBuy())
                            ob.setTotalAskSize(ob.getTotalAskSize() - remainingQtyToFill);
                        else
                            ob.setTotalBidSize(ob.getTotalBidSize() - remainingQtyToFill);
                        ptr.setCurrentQuantity(ptr.getCurrentQuantity() - remainingQtyToFill);
                        o.setCurrentQuantity(0); // order is fully matched, we can exit the while loop
                    }
                }
            }
        }

        // remove orders before potentially adding the new order as we need might need to update the orderbook best bid/ask
        // state during removeOrder
        for (Integer orderId: ordersToRemove)
        {
            ob.removeOrder(orderId);
        }

        // e.g. if we do an aggressive limit buy 500 qty on a book like 500 x 100/101 x 200, any remaining qty
        // will be made into a passive buy at 101. we don't clear anything beyond the top level.
        if (o.getCurrentQuantity() > 0)
            ob.addOrder(new Order(o.getSecurityId(), o.getSide(), o.getCurrentQuantity(), farTouchPrice));

    }
}
