package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;

public class ChangellyOfferActivity extends Activity {
    public static final int RESULT_FINISH = 11;
    @BindView(R.id.tvFromAmount)
    TextView tvFromAmount;

    @BindView(R.id.tvSendToAddress)
    TextView tvSendToAddress;

    private ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;
    private Receiver receiver;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);
        ButterKnife.bind(this);
        receiver = new Receiver();
        for (String action : new String[]{ChangellyService.INFO_TRANSACTION, ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }
        createOffer();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void updateUI() {
        tvFromAmount.setText(getString(R.string.value_currency, offer.currencyFrom, offer.amountFrom));
        tvSendToAddress.setText(offer.payinAddress);
    }

    @OnClick(R.id.tvSendToAddress)
    void clickAddress() {
        Utils.setClipboardString(offer.payinAddress, this);
        toast("Address copied to clipboard");
    }

    @OnClick(R.id.tvFromAmount)
    void clickAmount() {
        Utils.setClipboardString(String.valueOf(offer.amountFrom), this);
        toast("Amount copied to clipboard");
    }

    @OnClick(R.id.exchange_more)
    void clickExchangeMore() {
        finish();
    }

    @OnClick(R.id.check_balance)
    void clickCheckBalance() {
        setResult(RESULT_FINISH);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void createOffer() {
        Double dblAmount = getIntent().getDoubleExtra(ChangellyService.AMOUNT, 0);
        String currency = getIntent().getStringExtra(ChangellyService.FROM);
        String receivingAddress = getIntent().getStringExtra(ChangellyService.DESTADDRESS);
        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_CREATE_TRANSACTION)
                .putExtra(ChangellyService.FROM, currency)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount)
                .putExtra(ChangellyService.DESTADDRESS, receivingAddress);
        startService(changellyServiceIntent);
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Waiting offer...");
        progressDialog.show();
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ChangellyService.INFO_TRANSACTION:
                    progressDialog.dismiss();
                    offer = (ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                    updateUI();
                    break;
                case INFO_ERROR:
                    progressDialog.dismiss();
                    new AlertDialog.Builder(ChangellyOfferActivity.this)
                            .setMessage("Exchange service not available now, try later")
                            .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }).create().show();
                    break;
            }
        }
    }

}