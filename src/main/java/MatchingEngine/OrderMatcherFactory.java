package MatchingEngine;

public class OrderMatcherFactory {
    public static IOrderMatcher createOrderMatcher(String matcher)
    {
        if (matcher.equals("pricetime"))
            return new PriceTimePriorityMatcher();
        else if (matcher.equals("prorata"))
            return new ProRataMatcher();
        else
            throw new RuntimeException("matcher invalid.");
    }
}
