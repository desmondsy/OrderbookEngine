package Simulation;

public enum Event {
    AGGRESSIVE_BUY,
    PASSIVE_BUY,
    CANCEL_BUY,
    MOD_BUY,
    AGGRESSIVE_SELL,
    PASSIVE_SELL,
    CANCEL_SELL,
    MOD_SELL;

    public boolean isBuyEvent()
    {
        return this == AGGRESSIVE_BUY || this == PASSIVE_BUY || this == CANCEL_BUY || this == MOD_BUY;
    }

    public boolean isAggressiveEvent()
    {
        return this == AGGRESSIVE_BUY || this == AGGRESSIVE_SELL;
    }
}
