/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.api;

import com.mycelium.wapi.api.request.*;
import com.mycelium.wapi.api.response.*;

public interface Wapi {

   /**
    * The current version of the API
    */
   int VERSION = 1;

   int ERROR_CODE_SUCCESS = 0;
   int ERROR_CODE_NO_SERVER_CONNECTION = 1;
   int ERROR_CODE_INCOMPATIBLE_API_VERSION = 2;
   int ERROR_CODE_INTERNAL_CLIENT_ERROR = 3;
   int ERROR_CODE_INVALID_SESSION = 4;
   int ERROR_CODE_INVALID_ARGUMENT = 5;
   int ERROR_CODE_INTERNAL_SERVER_ERROR = 99;

   String MYCELIUM_VERSION_HEADER = "MyceliumVersion";


   int MAX_TRANSACTION_INVENTORY_LIMIT = 1000;
   /**
    * Get the logger configured for this {@link Wapi}
    *
    * @return the logger configured for this {@link Wapi}
    */
   com.mycelium.WapiLogger getLogger();

   /**
    * Query the full set of unspent outputs for a set of addresses
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"addresses":["msxh4zZoVwdRXfgmAYYo2MpNrJi4snrH6C","mfv9QuzUD7ZtnHxfpVX2859hs2ZHC8TG16","mpii6kiLM5HffaJdeD4Smnpv5eWo7qfKQ5"]}'
    *       https://144.76.165.115/wapitestnet/wapi/queryUnspentOutputs
    */
   WapiResponse<QueryUnspentOutputsResponse> queryUnspentOutputs(QueryUnspentOutputsRequest request);

   /**
    * Query the transaction inventory of a set of addresses with a limit on how many transaction IDs to retrieve
    * Example HTTP POST:
    *curl   -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"addresses":["mfd7QG4vn2U4U5BgnTuw7dmjKsutDxkK6b","mysJrGMsYht9u3gBvKHFcNJsVEmaEPhUGA","mvMyQXzaHk7Z6u3vsbzT7qmQJo225ma9g3"],"limit":1000}'
    *       https://144.76.165.115/wapitestnet/wapi/queryTransactionInventory
    */
   WapiResponse<QueryTransactionInventoryResponse> queryTransactionInventory(QueryTransactionInventoryRequest request);

   /**
    * Get a set of transactions from a set of transaction IDs
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
    *       https://144.76.165.115/wapitestnet/wapi/getTransactions
    */
   WapiResponse<GetTransactionsResponse> getTransactions(GetTransactionsRequest request);

   /**
    * Broadcast a transaction
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"rawTransaction":"AQAAAAHqHGsQSIun5hjDDWm7iFMwm85xNLt+HBfI3LS3uQHnSQEAAABrSDBFAiEA6rlGk4wgIL3TvC2YHK4XiBW2vPYg82iCgnQi+YOUwqACIBpzVk756/07SRORT50iRZvEGUIn3Lh3bhaRE1aUMgZZASECDFl9wEYDCvB1cJY6MbsakfKQ9tbQhn0eH9C//RI2iE//////ApHwGgAAAAAAGXapFIzWtPXZR7lk8RtvE0FDMHaLtsLCiKyghgEAAAAAABl2qRSuzci59wapXUEzwDzqKV9nIaqwz4isAAAAAA=="}'
    *       https://144.76.165.115/wapitestnet/wapi/broadcastTransaction
    */
   WapiResponse<BroadcastTransactionResponse> broadcastTransaction(BroadcastTransactionRequest request);

   /**
    * Check the status of a transaction.
    * <p/>
    * This allows you to check whether it exists, has confirmed, or got its
    * timestamp updated.
    * Example HTTP POST:
    *curl   -k -X POST -H "Content-Type: application/json"
    *       -d '{"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
    *       https://144.76.165.115/wapitestnet/wapi/checkTransactions
    */
   WapiResponse<CheckTransactionsResponse> checkTransactions(CheckTransactionsRequest request);

   /**
    * Query exchange rates
    * <p/>
    * Query the exchange rates for available exchanges converted to a specific fiat currency
    * Example HTTP POST:
    * curl  -k -X POST -H "Content-Type: application/json"
    *       -d '{"version":1,"currency":"USD"}'
    *       https://144.76.165.115/wapitestnet/wapi/queryExchangeRates
    */
   WapiResponse<QueryExchangeRatesResponse> queryExchangeRates(QueryExchangeRatesRequest request);

   /**
    * Check if the wapi-service is running
    *
    */
   WapiResponse<PingResponse> ping();

   /**
    * Report a app crash back to the server which sends a mail to the devolopers
    *
    */
   WapiResponse<ErrorCollectorResponse> collectError(ErrorCollectorRequest request);

   /**
    * Get the current version-number for a certain branch (Android, iOS, ..)
    * and also get a collection of eventually blocked features if there is a bug discovered
    *
    * returns null (empty object) if there are no warnings or important updates available for this branch/version
    *
    * curl -k -X POST -H "Content-Type: application/json" -d '{"branch":"android", "currentVersion":"2.3.1", "locale":"de" }' https://144.76.165.115/wapitestnet/wapi/getVersionEx
    *
    */
   WapiResponse<VersionInfoExResponse> getVersionInfoEx(VersionInfoExRequest request);


   /**
    * Get the current miner fee estimation in Bitcoin-per-kB, to be included within the next 1,2 or 4 Blocks
    *
    *
    * returns an object with {1: fee_1, 2: fee_2, 4: fee_4}  (where fee_n is the fee needed per kB to be
    * included in the next n-Blocks, in satoshis
    *
    * curl -k -X POST -H "Content-Type: application/json" -d '{}' https://144.76.165.115/wapitestnet/wapi/getMinerFeeEstimations
    *
    */
   WapiResponse<MinerFeeEstimationResponse> getMinerFeeEstimations();
}
