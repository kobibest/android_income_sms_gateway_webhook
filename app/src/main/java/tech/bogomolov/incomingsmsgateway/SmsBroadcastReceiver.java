package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        String format = bundle.getString("format");

        StringBuilder content = new StringBuilder();
        SmsMessage[] messages = new SmsMessage[pdus.length];

        for (int i = 0; i < pdus.length; i++) {
            if (format != null) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            } else {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }

            if (messages[i] != null) {
                content.append(messages[i].getDisplayMessageBody());
            }
        }

        if (messages[0] == null) {
            return;
        }

        String sender = messages[0].getOriginatingAddress();
        if (sender == null) {
            return;
        }

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        int detectedSlot = detectSim(bundle);
        int slotId = detectedSlot >= 0 ? detectedSlot + 1 : 0;
        String slotName = slotId > 0 ? "sim" + slotId : "undetected";

        for (ForwardingConfig config : configs) {

            if (!sender.equals(config.getSender()) && !config.getSender().equals(asterisk)) {
                continue;
            }

            if (!config.getIsSmsEnabled()) {
                continue;
            }

            if (config.getSimSlot() > 0 && config.getSimSlot() != slotId) {
                continue;
            }

            callWebHook(
                    config,
                    sender,
                    slotName,
                    content.toString(),
                    messages[0].getTimestampMillis()
            );
        }
    }

    protected void callWebHook(
            ForwardingConfig config,
            String sender,
            String slotName,
            String content,
            long timeStamp
    ) {

        String message = config.prepareMessage(sender, content, slotName, timeStamp);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data data = new Data.Builder()
                .putString(RequestWorker.DATA_URL, config.getUrl())
                .putString(RequestWorker.DATA_TEXT, message)
                .putString(RequestWorker.DATA_HEADERS, config.getHeaders())
                .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.getIgnoreSsl())
                .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.getChunkedMode())
                .putInt(RequestWorker.DATA_MAX_RETRIES, config.getRetriesNumber())
                .build();

        WorkRequest workRequest =
                new OneTimeWorkRequest.Builder(RequestWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .setInputData(data)
                        .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }

    private int detectSim(Bundle bundle) {
        int slotId = -1;
        Set<String> keySet = bundle.keySet();

        for (String key : keySet) {
            switch (key) {
                case "phone":
                case "slot":
                case "simId":
                case "simSlot":
                case "slot_id":
                case "simnum":
                case "slotId":
                case "slotIdx":
                case "android.telephony.extra.SLOT_INDEX":
                    slotId = bundle.getInt(key, -1);
                    break;

                default:
                    if (key.toLowerCase().contains("slot") || key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if ("0".equals(value) || "1".equals(value) || "2".equals(value)) {
                            slotId = bundle.getInt(key, -1);
                        }
                    }
            }

            if (slotId != -1) {
                break;
            }
        }

        return slotId;
    }
}
