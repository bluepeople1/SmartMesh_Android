package com.lingtuan.firefly.wallet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lingtuan.firefly.R;
import com.lingtuan.firefly.base.BaseActivity;
import com.lingtuan.firefly.ui.WebViewUI;
import com.lingtuan.firefly.util.Utils;
import com.lingtuan.firefly.util.netutil.NetRequestUtils;
import com.lingtuan.firefly.wallet.vo.TransVo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * transaction record detail
 * Created by Administrator on 2017/12/19.
 */

public class TransactionDetailActivity extends BaseActivity{

    private ImageView transDetailImg;//Transfer status img
    private TextView transDetailMoney;//Transfer amount
    private TextView transDetailMoneyType;//eth or smt
    private TextView transDetailType;//Transfer status
    private TextView transDetailFrom;//from
    private TextView transDetailTo;//to
    private TextView transDetailFee;//gas fee
    private TextView transDetailNumber;//ticket number
    private TextView transDetailBlockNumber;//block number
    private TextView transDetailTime;//transfer time
    private ImageView transDetailQuickMark;//transfer quickmark
    private TextView transDetailCopy;//transfer copy qrcode

    private TransVo transVo;
    private int recordType;//0 eth 1 smt

    private Timer timer;
    private TimerTask timerTask;

    @Override
    protected void setContentView() {
        setContentView(R.layout.transaction_detail_layout);
        getPassData();
    }

    private void getPassData() {
        transVo = (TransVo) getIntent().getSerializableExtra("transVo");
        recordType = getIntent().getIntExtra("recordType",0);

    }

    @Override
    protected void findViewById() {
        transDetailImg = (ImageView) findViewById(R.id.trans_detail_type_img);
        transDetailMoney = (TextView) findViewById(R.id.trans_detail_money);
        transDetailMoneyType = (TextView) findViewById(R.id.trans_detail_money_type);
        transDetailType = (TextView) findViewById(R.id.trans_detail_type);
        transDetailFrom = (TextView) findViewById(R.id.trans_detail_from);
        transDetailTo = (TextView) findViewById(R.id.trans_detail_to);
        transDetailFee = (TextView) findViewById(R.id.trans_detail_fee);
        transDetailNumber = (TextView) findViewById(R.id.trans_detail_number);
        transDetailBlockNumber = (TextView) findViewById(R.id.trans_detail_block_number);
        transDetailTime = (TextView) findViewById(R.id.trans_detail_time);
        transDetailCopy = (TextView) findViewById(R.id.trans_detail_copy);
        transDetailQuickMark = (ImageView) findViewById(R.id.trans_detail_quick_mark);
    }

    @Override
    protected void setListener() {
        transDetailCopy.setOnClickListener(this);
        transDetailNumber.setOnClickListener(this);
    }

    @Override
    protected void initData() {

        setTitle(getString(R.string.transcation_detail));

        if (transVo != null){
            if (transVo.getValue().startsWith("+") || transVo.getValue().startsWith("-")){
                transDetailMoney.setText(transVo.getValue().substring(1,transVo.getValue().length()));
            }else{
                transDetailMoney.setText(transVo.getValue());
            }

            transDetailNumber.setText(transVo.getTx());
            transDetailTime.setText(Utils.transDetailTime(transVo.getTime()));
            if (!TextUtils.isEmpty(transVo.getTxurl())){
                transDetailQuickMark.setImageBitmap(createQRCodeBitmap(transVo.getTxurl(),Utils.dip2px(TransactionDetailActivity.this,120)));
            }
            transDetailFee.setText(getString(R.string.eth_er_lower,transVo.getFee()));

            if (transVo.getTxBlockNumber() <= 0){
                transDetailBlockNumber.setText(getString(R.string.wallet_trans_detail_block_none));
            }else{
                transDetailBlockNumber.setText(transVo.getTxBlockNumber() + "");
            }

            if (transVo.getType() == 0){
                transDetailMoneyType.setText(getString(R.string.eth_er_lower_1));
            }else{
                transDetailMoneyType.setText(getString(R.string.smt_er_lower_1));
            }

            if (transVo.getValue().contains("+")){
                transDetailFrom.setText(transVo.getToAddress());
                if (!TextUtils.isEmpty(transVo.getFromAddress())){
                    transDetailTo.setText(transVo.getFromAddress());
                }
            }else{
                transDetailTo.setText(transVo.getToAddress());
                if (!TextUtils.isEmpty(transVo.getFromAddress())){
                    transDetailFrom.setText(transVo.getFromAddress());
                }
            }


            switch (transVo.getState()){
                case -1:
                    transDetailImg.setImageResource(R.drawable.trans_detail_wait);
                    transDetailType.setVisibility(View.VISIBLE);
                    transDetailType.setText(getString(R.string.wallet_trans_detail_type_0));
                    transDetailState();
                    break;
                case 0:
                    transDetailImg.setImageResource(R.drawable.trans_detail_wait);
                    transDetailType.setVisibility(View.VISIBLE);
                    transDetailType.setText(getString(R.string.wallet_trans_detail_type_1,transVo.getBlockNumber() - transVo.getTxBlockNumber() + 1));
                    transDetailState();
                    break;
                case 1:
                    transDetailImg.setImageResource(R.drawable.trans_detail_success);
                    transDetailType.setVisibility(View.GONE);
                    break;
                case 2:
                    transDetailImg.setImageResource(R.drawable.trans_detail_failed);
                    transDetailType.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private Bitmap createQRCodeBitmap(String content , int widthAndHeight) {
        Hashtable<EncodeHintType, Object> qrParam = new Hashtable<>();
        qrParam.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        qrParam.put(EncodeHintType.CHARACTER_SET, "utf-8");
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content,BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight, qrParam);
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] data = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (bitMatrix.get(x, y))
                        data[y * w + x] = 0xff000000;
                    else
                        data[y * w + x] = -1;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(data, 0, w, 0, 0, w, h);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.trans_detail_copy:
                Utils.copyText(TransactionDetailActivity.this,transVo.getTxurl());
                break;
            case R.id.trans_detail_number:
                if (!TextUtils.isEmpty(transVo.getTxurl())){
                    Intent intent = new Intent(TransactionDetailActivity.this, WebViewUI.class);
                    intent.putExtra("loadUrl", transVo.getTxurl());
                    intent.putExtra("title", getString(R.string.transcation_search));
                    startActivity(intent);
                }
                break;
        }
    }


    /**
     * Turn on the timer to call the interface every 12 seconds
     * */
    private void transDetailState(){
        if(timer == null){
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (transVo.getState() == -1){
                        getTranscationBlock();
                    }else if (transVo.getState() == 0){
                        getBlockNumber(transVo.getTxBlockNumber());
                    }
                }
            };
            timer.schedule(timerTask,0,10000);
        }
    }

    /**
     * Get the latest block number
     * @param transBlockNumber The block number where the transaction hash is located
     * */
    private void getBlockNumber(final int transBlockNumber){
        try {
            NetRequestUtils.getInstance().getBlockNumber(TransactionDetailActivity.this,new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonString = response.body().string();
                        JSONObject object = new JSONObject(jsonString);
                        if (object.optInt("errcod") == 0){
                            int blockNumber = object.optJSONObject("data").optInt("blockNumber",0);
                            Message message = Message.obtain();
                            message.what = 0;
                            message.arg1 = blockNumber - transBlockNumber;
                            mHandler.sendMessage(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the block number of the transaction hash
     * */
    private void getTranscationBlock(){
        try {
            NetRequestUtils.getInstance().getTxBlockNumber(TransactionDetailActivity.this,transVo.getTx(),new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonString = response.body().string();
                        JSONObject object = new JSONObject(jsonString);
                        if (object.optInt("errcod") == 1001222){
                            int transBlockNumber = object.optJSONObject("data").optInt("blockNumber",0);
                            transVo.setState(0);
                            transVo.setTxBlockNumber(transBlockNumber);
                            Message message = Message.obtain();
                            message.what = 1;
                            mHandler.sendMessage(message);
                        }else if (object.optInt("errcod") == 1001221){
                            int transBlockNumber = object.optJSONObject("data").optInt("blockNumber",0);
                            transVo.setState(2);
                            transVo.setTxBlockNumber(transBlockNumber);
                            Message message = Message.obtain();
                            message.what = 2;
                            mHandler.sendMessage(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int blockNumber = msg.arg1;
                    if (blockNumber >= 11){
                        transDetailImg.setImageResource(R.drawable.trans_detail_success);
                        transDetailType.setVisibility(View.GONE);
                        transVo.setState(1);
                        if (timer  != null){
                            timer.cancel();
                            timer = null;
                        }
                        if (timerTask != null){
                            timerTask.cancel();
                            timerTask = null;
                        }
                    }else{
                        transDetailType.setVisibility(View.VISIBLE);
                        transDetailType.setText(getString(R.string.wallet_trans_detail_type_1,blockNumber + 1));
                    }
                    break;
                case 1:
                    transDetailBlockNumber.setText(transVo.getTxBlockNumber() + "");
                    break;
                case 2:
                    transDetailBlockNumber.setText(transVo.getTxBlockNumber() + "");
                    transDetailImg.setImageResource(R.drawable.trans_detail_failed);
                    transDetailType.setVisibility(View.GONE);
                    if (timer  != null){
                        timer.cancel();
                        timer = null;
                    }
                    if (timerTask != null){
                        timerTask.cancel();
                        timerTask = null;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null){
            timer.cancel();
            timer = null;
        }
        if (timerTask != null){
            timerTask.cancel();
            timerTask = null;
        }
    }
}
