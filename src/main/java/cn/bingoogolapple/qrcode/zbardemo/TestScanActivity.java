package cn.bingoogolapple.qrcode.zbardemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerActivity;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;

public class TestScanActivity extends AppCompatActivity implements QRCodeView.Delegate {
    private static final String TAG = TestScanActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY = 666;

    private QRCodeView mQRCodeView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_scan);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mQRCodeView = (ZBarView) findViewById(R.id.zbarview);
        mQRCodeView.setDelegate(this);
        mQRCodeView.startSpot();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera();
//        mQRCodeView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);

        mQRCodeView.showScanRect();
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void onScanQRCodeSuccess(String result) {

        if (TextUtils.isEmpty(result)) {
            Toast.makeText(TestScanActivity.this, "未发现二维码", Toast.LENGTH_SHORT).show();
        } else {
            try {
                result=RsaPriDecrypt(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (result.contains("|")&&result.split("\\|").length==6){
                vibrate();
                mQRCodeView.stopSpot();
                String message="";
                final String[] messageVue=result.split("\\|");
                String[] messageKey={"发件人:","发件人手机号:","发出地:","收件人:","收件手机号:","收件人地址:"};
                for (int i=0;i<messageVue.length;i++){
                    message+=messageKey[i]+messageVue[i]+"\n";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("派单处理")
                        .setMessage(message)
                        .setPositiveButton("拨打收件人电话", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(ContextCompat.checkSelfPermission(TestScanActivity.this, Manifest.permission.CALL_PHONE)
                                        != PackageManager.PERMISSION_GRANTED){
                                    Toast.makeText(TestScanActivity.this,"没有电话权限",Toast.LENGTH_LONG).show();

                                    //　没有该权限　申请打电话权限
                                    //  三个参数 第一个参数是 Context , 第二个参数是用户需要申请的权限字符串数组，第三个参数是请求码 主要用来处理用户选择的返回结果
                                }else {
                                    //　有该权限，直接打电话
                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    Uri data = Uri.parse("tel:" + messageVue[4]);
                                    intent.setData(data);
                                    startActivity(intent);
                                }

                            }
                        }).setNegativeButton("忽略", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mQRCodeView.startSpot();
                            }
                }).create().show();
                Toast.makeText(TestScanActivity.this, result, Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(TestScanActivity.this,"二维码不正确",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e(TAG, "打开相机出错");
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_spot:
                mQRCodeView.startSpot();
                break;
            case R.id.stop_spot:
                mQRCodeView.stopSpot();
                break;
            case R.id.start_spot_showrect:
                mQRCodeView.startSpotAndShowRect();
                break;
            case R.id.stop_spot_hiddenrect:
                mQRCodeView.stopSpotAndHiddenRect();
                break;
            case R.id.show_rect:
                mQRCodeView.showScanRect();
                break;
            case R.id.hidden_rect:
                mQRCodeView.hiddenScanRect();
                break;
            case R.id.start_preview:
                mQRCodeView.startCamera();
                break;
            case R.id.stop_preview:
                mQRCodeView.stopCamera();
                break;
            case R.id.open_flashlight:
                mQRCodeView.openFlashlight();
                break;
            case R.id.close_flashlight:
                mQRCodeView.closeFlashlight();
                break;
            case R.id.scan_barcode:
                mQRCodeView.changeToScanBarcodeStyle();
                break;
            case R.id.scan_qrcode:
                mQRCodeView.changeToScanQRCodeStyle();
                break;
            case R.id.choose_qrcde_from_gallery:
                /*
                从相册选取二维码图片，这里为了方便演示，使用的是
                https://github.com/bingoogolapple/BGAPhotoPicker-Android
                这个库来从图库中选择二维码图片，这个库不是必须的，你也可以通过自己的方式从图库中选择图片
                 */

                // 识别图片中的二维码还有问题，占时不要用
//                Intent photoPickerIntent = new BGAPhotoPickerActivity.IntentBuilder(this)
//                        .cameraFileDir(null)
//                        .maxChooseCount(1)
//                        .selectedPhotos(null)
//                        .pauseOnScroll(false)
//                        .build();
//                startActivityForResult(photoPickerIntent, REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 识别图片中的二维码还有问题，占时不要用
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_QRCODE_FROM_GALLERY) {
            final String picturePath = BGAPhotoPickerActivity.getSelectedPhotos(data).get(0);

            /*
            这里为了偷懒，就没有处理匿名 AsyncTask 内部类导致 Activity 泄漏的问题
            请开发在使用时自行处理匿名内部类导致Activity内存泄漏的问题，处理方式可参考 https://github.com/GeniusVJR/LearningNotes/blob/master/Part1/Android/Android%E5%86%85%E5%AD%98%E6%B3%84%E6%BC%8F%E6%80%BB%E7%BB%93.md
             */
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    Bitmap bitmap = getDecodeAbleBitmap(picturePath);
                    int picw = bitmap.getWidth();
                    int pich = bitmap.getHeight();
                    int[] pix = new int[picw * pich];
                    byte[] pixytes = new byte[picw * pich];
                    bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);
                    int R, G, B, Y;

                    for (int y = 0; y < pich; y++) {
                        for (int x = 0; x < picw; x++) {
                            int index = y * picw + x;
                            R = (pix[index] >> 16) & 0xff;     //bitwise shifting
                            G = (pix[index] >> 8) & 0xff;
                            B = pix[index] & 0xff;

                            //R,G.B - Red, Green, Blue
                            //to restore the values after RGB modification, use
                            //next statement
                            pixytes[index] = (byte) (0xff000000 | (R << 16) | (G << 8) | B);
                        }
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
                    byte[] data = new byte[(int) (bitmap.getHeight() * bitmap.getWidth() * 1.5)];
                    rgba2Yuv420(pixytes, data, bitmap.getWidth(), bitmap.getHeight());
                    return mQRCodeView.processData(data, bitmap.getWidth(), bitmap.getHeight(), true);
                }

                @Override
                protected void onPostExecute(String result) {
                    if (TextUtils.isEmpty(result)) {
                        Toast.makeText(TestScanActivity.this, "未发现二维码", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TestScanActivity.this, result, Toast.LENGTH_SHORT).show();

                    }
                }
            }.execute();
        }
    }


    /**
     * 将本地图片文件转换成可解码二维码的 Bitmap。为了避免图片太大，这里对图片进行了压缩。感谢 https://github.com/devilsen 提的 PR
     *
     * @param picturePath 本地图片文件路径
     * @return
     */
    private static Bitmap getDecodeAbleBitmap(String picturePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(picturePath, options);
            int sampleSize = options.outHeight / 400;
            if (sampleSize <= 0) {
                sampleSize = 1;
            }
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(picturePath, options);
        } catch (Exception e) {
            return null;
        }
    }

    public static void rgba2Yuv420(byte[] src, byte[] dst, int width, int height) {
        // Y
        for (int y = 0; y < height; y++) {
            int dstOffset = y * width;
            int srcOffset = y * width * 4;
            for (int x = 0; x < width && dstOffset < dst.length && srcOffset < src.length; x++) {
                dst[dstOffset] = src[srcOffset];
                dstOffset += 1;
                srcOffset += 4;
            }
        }
        /* Cb and Cr */
        for (int y = 0; y < height / 2; y++) {
            int dstUOffset = y * width + width * height;
            int srcUOffset = y * width * 8 + 1;

            int dstVOffset = y * width + width * height + 1;
            int srcVOffset = y * width * 8 + 2;
            for (int x = 0; x < width / 2 && dstUOffset < dst.length && srcUOffset < src.length && dstVOffset < dst.length && srcVOffset < src.length; x++) {
                dst[dstUOffset] = src[srcUOffset];
                dst[dstVOffset] = src[srcVOffset];

                dstUOffset += 2;
                dstVOffset += 2;

                srcUOffset += 8;
                srcVOffset += 8;
            }
        }
    }

    final private static String RSA_PRIVATE_KEY ="-----BEGIN PRIVATE KEY-----\n" +
            "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAK69YG6DR6I0x076\n" +
            "SjYWTRunnvbd7nZW1TqleFr5oRDt/tc2Oe0vaR21dpDXPMv0ujCl9JSbb+cnWSdM\n" +
            "o5GHHG8W29wZo/uis/1PYTIVbeMuM1qbmJNeijIo/FXzw5eLnL4RjaIYReJ/KFMs\n" +
            "Dvp1fKCtcxMWc76Rq3Lpnc0NfktFAgMBAAECgYAO0xIzTf/tyvx9vs48+HdiOT5L\n" +
            "Q1jPwV4ls5QaY8M+ZHKSX49aiAMq+hItB5Wo2AJTzkCJuwYrXjM6Gk5mMkg/IJX8\n" +
            "8UHoRjJ/bfvLn0a2s1ghsXw1Ghj3Gbb2uU3d53b9BgRh67FYzgSlwmzS5jvfirig\n" +
            "e1u4p/PuwFeaQTs2AQJBANTpsdi6SrTD5YD2FDyCgMEJnVfg8OPcMY9CT+KVfos3\n" +
            "/IxAT3hNbZ4/XPMHgja0yNVRSvZn9U90t7OWyMdMrBECQQDSGg9mKy+iqvQN/mF7\n" +
            "QJC+QLaPUyQ2/j8Yhs2yrBuz67tlj0JqCqqvq0YyR69bMyJQkrddvAnrErFv7vRl\n" +
            "nq/1AkEApT56+WoccQ9ZIC3cptnic++yXnIGg9Jx5G3i8kh0XjilmXSQOR5e5WLo\n" +
            "EPbS6QKGnIjrVTJ6AaDksk1kpsmrAQJBAMSV/7ygQfUZsjv5vip+EjECCg93QtZ1\n" +
            "9IG1eHhq04z40CJJ9mGUU3sFbiwTqP9TjBMKBKqfDES4++95DZKb9ZUCQQCeD1VL\n" +
            "yVzJ7nhvW6pkf0zQ8cXpgQ2fXgn4XBece8Re+Xn3WXIvq43pa9PJ5uu7EXspEeRu\n" +
            "M20+KKWihAk+RgHx\n" +
            "-----END PRIVATE KEY-----\n";

    public static String RsaPriDecrypt(String key) throws Exception {
        String privKeyPEM = RSA_PRIVATE_KEY.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");
        byte [] encoded = Base64.decode(privKeyPEM, Base64.DEFAULT);


        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privKey);

        byte[] decodedStr           = Base64.decode(key, Base64.DEFAULT);
        byte[] plainText            = cipher.doFinal(decodedStr);

        String   str = new String(plainText);
        return str;
    }
}