package com.mawlee.cointcore.shop;

public enum CurrencyMovementType {
    PAY("pay"),
    TRADER_BUY("trader_buy"),
    TRADER_SELL("trader_sell"),
    ADMIN_SET("admin_set"),
    ADMIN_ADD("admin_add");

    private final String id;

    CurrencyMovementType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CurrencyMovementType fromId(String id) {
        if (id == null) {
            return PAY;
        }
        for (CurrencyMovementType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return PAY;
    }
}
