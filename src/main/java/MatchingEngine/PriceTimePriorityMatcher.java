package MatchingEngine;

import Orderbook.Orderbook;
import Orders.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.TreeSet;

class PriceTimePriorityMatcher extends AbstractOrderMatcher {
    private static final Logger logger = LogManager.getLogger(PriceTimePriorityMatcher.class);

    @Override
    public void matchMarketOrder(Order o, Orderbook ob)
    {
        int totalSize = o.isBuy() ? ob.getTotalAskSize() : ob.getTotalBidSize();
        if (o.getInitialQuantity() >= totalSize)
        {
            // if e.g. total ask size is 10000, but an aggressive buy order comes in for 20000, we aren't going to match it.
            logger.error("order qty ({}) is greater than the total aggressive size ({}). Not matching.", o.getInitialQuantity(), totalSize);
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        int filledQty = 0;

        for (Limit limit: limitTree)
        {
            logger.info("iterating through resting orders at limit={}...", limit.getPrice());
            Order ptr = limit.getHead();

            // only iterate limit levels that have at least one order in it.
            // e.g. if we are 100/101 and then we deplete 101 with a market order, the 101 limit is technically still here
            // but will be null (ptr = null). So we don't do any matching on that null level

            // whilst there are still orders to be matched in the current limit level AND market order still has excess unmatched qty
            while(ptr!=null && o.getCurrentQuantity() > 0)
            {
                logger.info("matching with resting orderID: {}, price: {}, qty: {}", ptr.getOrderId(), ptr.getPrice(), ptr.getCurrentQuantity());
                if (o.getCurrentQuantity() >= ptr.getCurrentQuantity())
                {
                    // we are able to fill an entire resting order with possible excess, so we keep going
                    Trade trade = new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                    trades.add(trade);
                    logger.info("new trade: {}", trade);

                    filledQty += ptr.getCurrentQuantity();
                    o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                    ob.removeOrder(ptr.getOrderId(), true);
                    ptr = ptr.getNextOrder();
                }
                else
                {
                    Trade trade = new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                    trades.add(trade);
                    logger.info("new trade: {}", trade);

                    filledQty += o.getCurrentQuantity();
                    int remainingQtyToFill = o.getCurrentQuantity();
                    limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - remainingQtyToFill);
                    if (o.isBuy())
                        ob.setTotalAskSize(ob.getTotalAskSize() - remainingQtyToFill);
                    else
                        ob.setTotalBidSize(ob.getTotalBidSize() - remainingQtyToFill);
                    ptr.setCurrentQuantity(ptr.getInitialQuantity() - remainingQtyToFill);
                    o.setCurrentQuantity(0); // market order is fully matched, we can exit the while loop
                }

                logger.info("orderID: {}, filledQty: {}/{}", o.getOrderId(), filledQty, o.getInitialQuantity());
            }

            // if we reach here, we have either depleted the current limit level OR the market order has been completely filled.
            // If the market order still has remaining unmatched quantity, we need to continue to the next best limit.
            if (o.getCurrentQuantity() == 0)
            {
                logger.info("orderID: {} - fully filled.", o.getOrderId());
                break;
            }

            logger.info("limit price {} cleared. Order still has {} remaining qty. Continuing to next best limit...", limit.getPrice(), o.getCurrentQuantity());
        }

        ob.clearEmptyLimitsAfterMatching(o.isBuy());

        if (o.getCurrentQuantity() > 0)
        {
            // ideally there should be no order quantity remaining after going through ALL the limit levels
            // the initial total size condition should prevent us from reaching this block.
            // raise exception if we somehow reach here.
            throw new UnexpectedRemainingVolumeException("There shouldn't be any remaining quantity here.");
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
            logger.error("order qty ({}) is greater than the total aggressive size ({}). Not matching.", o.getInitialQuantity(), totalSize);
            return;
        }

        TreeSet<Limit> limitTree = o.isBuy() ? ob.getAskLimits() : ob.getBidLimits();
        double farTouchPrice = o.isBuy() ? ob.getBestAsk() : ob.getBestBid();
        int filledQty = 0;

        for (Limit limit: limitTree)
        {
            if (limit.getPrice() == farTouchPrice)
            {
                logger.info("iterating through resting orders at limit={} only...", limit.getPrice());
                Order ptr = limit.getHead();

                while(ptr!=null && o.getCurrentQuantity() > 0)
                {
                    logger.info("matching with resting orderID: {}, price: {}, qty: {}", ptr.getOrderId(), ptr.getPrice(), ptr.getCurrentQuantity());
                    if (o.getCurrentQuantity() >= ptr.getCurrentQuantity())
                    {
                        // we are able to fill an entire resting order with possible excess, so we keep going
                        Trade trade = new Trade(o.getSide(), ptr.getParentLimit().getPrice(), ptr.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);

                        filledQty += ptr.getCurrentQuantity();
                        o.setCurrentQuantity(o.getCurrentQuantity() - ptr.getCurrentQuantity());
                        ob.removeOrder(ptr.getOrderId(), true);
                        ptr = ptr.getNextOrder();
                    }
                    else
                    {
                        Trade trade = new Trade(o.getSide(), ptr.getParentLimit().getPrice(), o.getCurrentQuantity(), ptr.getOrderId(), o.getOrderId());
                        trades.add(trade);
                        logger.info("new trade: {}", trade);

                        filledQty += o.getCurrentQuantity();
                        int remainingQtyToFill = o.getCurrentQuantity();
                        limit.setTotalVolumeAtLimit(limit.getTotalVolumeAtLimit() - remainingQtyToFill);
                        if (o.isBuy())
                            ob.setTotalAskSize(ob.getTotalAskSize() - remainingQtyToFill);
                        else
                            ob.setTotalBidSize(ob.getTotalBidSize() - remainingQtyToFill);
                        ptr.setCurrentQuantity(ptr.getInitialQuantity() - remainingQtyToFill);
                        o.setCurrentQuantity(0); // market order is fully matched, we can exit the while loop
                    }

                    logger.info("orderID: {}, filledQty: {}/{}", o.getOrderId(), filledQty, o.getInitialQuantity());
                }
            }
        }

        ob.clearEmptyLimitsAfterMatching(o.isBuy());

        // e.g. if we do an aggressive limit buy 500 qty on a book like 500 x 100/101 x 200, any remaining qty
        // will be made into a passive buy at 101. we don't clear anything beyond the top level.
        if (o.getCurrentQuantity() > 0)
        {
            logger.info("aggressive limit order cleared the entire far touch qty. Creating a new limit for the remaining qty.");
            ob.addOrder(new Order(o.getSecurityId(), o.getSide(), o.getCurrentQuantity(), farTouchPrice));
        }

    }
}
