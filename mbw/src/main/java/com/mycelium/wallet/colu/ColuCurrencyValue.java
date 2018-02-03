package com.mycelium.wallet.colu;

import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.math.BigDecimal;

/**
 * Created by elvis on 27.07.17.
 */

public class ColuCurrencyValue extends ExactCurrencyValue {

    private String currency;
    private BigDecimal value;

    public ColuCurrencyValue(BigDecimal value, String currency) {
        this.currency = currency;
        this.value = value;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }
}
