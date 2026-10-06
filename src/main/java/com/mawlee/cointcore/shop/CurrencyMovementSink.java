package com.mawlee.cointcore.shop;

/**
 * Optional forwarder toward the site currency-movement section.
 * Default implementation is a no-op: the durable outbox still records locally.
 * <p>
 * Do not call any API that sets an absolute site wallet balance (old AzLink did this).
 * Queue/append-only posting is the only safe direction when an adapter is wired later.
 */
public interface CurrencyMovementSink {
    CurrencyMovementSink NOOP = movement -> {
    };

    void enqueue(CurrencyMovement movement);
}
