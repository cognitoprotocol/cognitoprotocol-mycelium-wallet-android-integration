package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.model.Transaction;

/**
 * Hardware wallets provide signatures so accounts can work without the private keys themselves.
 */
public interface ExternalSignatureProvider {
   Transaction getSignedTransaction(StandardTransactionBuilder.UnsignedTransaction unsigned, Bip44AccountExternalSignature forAccount);
   int getBIP44AccountType();
}
