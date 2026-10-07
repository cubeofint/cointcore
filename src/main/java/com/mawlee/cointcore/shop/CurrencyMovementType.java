package com.mawlee.cointcore.shop;

public enum CurrencyMovementType {
    PAY("pay"),
    TRADER_BUY("trader_buy"),
    TRADER_SELL("trader_sell"),
    ADMIN_SET("admin_set"),
    ADMIN_ADD("admin_add"),
    SITE_TO_SERVER("site_to_server"),
    SERVER_TO_SITE("server_to_site");

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
