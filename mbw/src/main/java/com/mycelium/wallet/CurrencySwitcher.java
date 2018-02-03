/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import com.google.api.client.util.Lists;
import com.google.common.base.Strings;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.currency.CurrencySum;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.util.*;

public class CurrencySwitcher {
   private final ExchangeRateManager exchangeRateManager;

   private List<String> fiatCurrencies;
   private CoinUtil.Denomination bitcoinDenomination;

   // the last selected/shown fiat currency
   private String currentFiatCurrency;

   // the last shown currency (usually same as fiat currency, but in some spots we cycle through all currencies including Bitcoin)
   private String currentCurrency;

   public CurrencySwitcher(final ExchangeRateManager exchangeRateManager, final Set<String> fiatCurrencies, final String currentCurrency, final CoinUtil.Denomination bitcoinDenomination) {
      this.exchangeRateManager = exchangeRateManager;
      ArrayList<String> currencies = Lists.newArrayList(fiatCurrencies);
      Collections.sort(currencies);
      this.fiatCurrencies = currencies;
      this.bitcoinDenomination = bitcoinDenomination;

      this.currentCurrency = currentCurrency;

      // if BTC is selected or current currency is not in list of available currencies (e.g. after update)
      // select a default one or none
      if (currentCurrency.equals(CurrencyValue.BTC) || !fiatCurrencies.contains(currentCurrency)) {
         if (fiatCurrencies.size() == 0) {
            this.currentFiatCurrency = "";  // no fiat currency selected
         } else {
            this.currentFiatCurrency = currencies.get(0);
         }
      } else {
         this.currentFiatCurrency = currentCurrency;
      }
   }

   public ExchangeRateManager getExchangeRateManager() {
      return exchangeRateManager;
   }

   public void setCurrency(final String setToCurrency) {
      //TODO need no accurate detect is colu currency
      if (!setToCurrency.equals(CurrencyValue.BTC)
              && !setToCurrency.equals("RMC")
              && !setToCurrency.equals("MT")
              && !setToCurrency.equals("MSS")) {
         currentFiatCurrency = setToCurrency;
      }
      currentCurrency = setToCurrency;
   }

   public String getCurrentFiatCurrency() {
      return currentFiatCurrency;
   }

   public String getCurrentCurrency() {
      return currentCurrency;
   }

   public String getCurrentCurrencyIncludingDenomination() {
      if (currentCurrency.equals(CurrencyValue.BTC)) {
         // use denomination only for btc
         return bitcoinDenomination.getUnicodeName();
      } else {
         return currentCurrency;
      }
   }


   public List<String> getCurrencyList() {
      //make a copy to prevent others from changing our internal list
      return new ArrayList<String>(fiatCurrencies);
   }

   public int getFiatCurrenciesCount() {
      return fiatCurrencies.size();
   }

   public int getCurrenciesCount() {
      return fiatCurrencies.size() + 1;  // BTC is always available
   }

   public void setCurrencyList(final Set<String> fiatCurrencies) {
      // convert the set to a list and sort it
      ArrayList<String> currencies = Lists.newArrayList(fiatCurrencies);
      Collections.sort(currencies);

      //if we de-selected our current active currency, we switch it
      if (!currencies.contains(currentFiatCurrency)) {
         if (currencies.isEmpty()) {
            //no fiat
            setCurrency("");
         } else {
            setCurrency(currencies.get(0));
         }
      }
      //copy to prevent changes by caller
      this.fiatCurrencies = new ArrayList<String>(currencies);
   }

   public String getNextCurrency(boolean includeBitcoin) {
      List<String> currencies = getCurrencyList();

      //just to be sure we dont cycle through a single one
      if (!includeBitcoin && currencies.size() <= 1) {
         return currentFiatCurrency;
      }

      int index = currencies.indexOf(currentCurrency);
      index++; //hop one forward

      if (index >= currencies.size()) {
         // we are at the end of the fiat-list. return BTC if we should include Bitcoin, otherwise wrap around
         if (includeBitcoin) {
            // only set currentCurrency, but leave currentFiat currency as it was
            currentCurrency = CurrencyValue.BTC;
         } else {
            index -= currencies.size(); //wrap around
            currentCurrency = currencies.get(index);
            currentFiatCurrency = currentCurrency;
         }
      } else {
         currentCurrency = currencies.get(index);
         currentFiatCurrency = currentCurrency;
      }

      exchangeRateManager.requestOptionalRefresh();

      return currentCurrency;
   }

   public CoinUtil.Denomination getBitcoinDenomination() {
      return bitcoinDenomination;
   }

   public void setBitcoinDenomination(CoinUtil.Denomination _bitcoinDenomination) {
      this.bitcoinDenomination = _bitcoinDenomination;
   }

   public String getBtcValueString(long satoshis) {
      return getBtcValueString(satoshis, true);
   }

   public String getBtcValueString(long satoshis, boolean includeUnit) {
      CoinUtil.Denomination d = getBitcoinDenomination();
      String valueString = CoinUtil.valueString(satoshis, d, true);
      if (includeUnit) {
         return valueString + " " + d.getUnicodeName();
      } else {
         return valueString;
      }
   }

   public String getBtcValueString(long satoshis, boolean includeUnit, int precision) {
      CoinUtil.Denomination d = getBitcoinDenomination();
      String valueString = CoinUtil.valueString(satoshis, d, precision);
      if (includeUnit) {
         return valueString + " " + d.getUnicodeName();
      } else {
         return valueString;
      }
   }


   public boolean isFiatExchangeRateAvailable() {
      if (Strings.isNullOrEmpty(currentFiatCurrency)) {
         // we dont even have a fiat currency...
         return false;
      }

      // check if there is a rate available
      ExchangeRate rate = exchangeRateManager.getExchangeRate(getCurrentFiatCurrency());
      return rate != null && rate.price != null;
   }

   public String getFormattedFiatValue(CurrencyValue value, boolean includeCurrencyCode) {
      if (value == null){
         return "";
      }

      CurrencyValue targetCurrency = getAsFiatValue(value);

      if (Strings.isNullOrEmpty(currentFiatCurrency)) {
         return "";
      }

      if (targetCurrency == null) {
         //todo
         return "";
      } else {
         if (includeCurrencyCode) {
            return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination());
         } else {
            return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination());
         }
      }
   }

   public String getFormattedFiatValue(CurrencyValue value, boolean includeCurrencyCode, int precision) {
      if (Strings.isNullOrEmpty(currentFiatCurrency)) {
         return "";
      }

      CurrencyValue targetCurrency = getAsFiatValue(value);

      if (targetCurrency == null) {
         return "";
      } else {
         if (includeCurrencyCode) {
            return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination(), precision);
         } else {
            return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination(), precision);
         }
      }
   }

   public String getFormattedValue(CurrencyValue currencyValue, boolean includeCurrencyCode) {
      if (currencyValue == null){
         return "";
      }
      CurrencyValue targetCurrency = getAsValue(currencyValue);
      if (includeCurrencyCode) {
         return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination());
      } else {
         return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination());
      }
   }

   public String getFormattedValue(CurrencyValue currencyValue, boolean includeCurrencyCode, int precision) {
      if (currencyValue == null){
         return "";
      }
      CurrencyValue targetCurrency = getAsValue(currencyValue);
      if (includeCurrencyCode) {
         return Utils.getFormattedValueWithUnit(targetCurrency, getBitcoinDenomination(), precision);
      } else {
         return Utils.getFormattedValue(targetCurrency, getBitcoinDenomination(), precision);
      }
   }

   public CurrencyValue getAsFiatValue(CurrencyValue value){
      if (value == null){
         return null;
      }
      if (Strings.isNullOrEmpty(currentFiatCurrency)) {
         return null;
      }
      return CurrencyValue.fromValue(value, getCurrentFiatCurrency(), exchangeRateManager);
   }

   public CurrencyValue getAsValue(CurrencyValue value){
      if (value == null){
         return null;
      }
      return CurrencyValue.fromValue(value, getCurrentCurrency(), exchangeRateManager);
   }

   /**
    * Get the exchange rate price for the currently selected currency.
    * <p>
    * Returns null if the current rate is too old or for a different currency.
    * In that the case the caller could choose to call refreshRates() and supply a handler to get a callback.
    */
   public synchronized Double getExchangeRatePrice() {
      ExchangeRate rate = exchangeRateManager.getExchangeRate(currentFiatCurrency);
      return rate == null ? null : rate.price;
   }

   public CurrencyValue getValueFromSum(CurrencySum sum) {
      return sum.getSumAsCurrency(currentCurrency, exchangeRateManager);
   }
}
