package MatchingEngine;

public class OrderMatcherFactory {
    public static AbstractOrderMatcher createOrderMatcher(String matcher)
    {
        if (matcher.equals("pricetime"))
            return new PriceTimePriorityMatcher();
        else if (matcher.equals("prorata"))
            return new ProRataMatcher();
        else
            throw new RuntimeException("matcher invalid.");
    }
}
