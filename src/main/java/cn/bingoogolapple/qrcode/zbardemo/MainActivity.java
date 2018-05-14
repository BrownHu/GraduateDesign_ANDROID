package cn.bingoogolapple.qrcode.zbardemo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        TextView UserTip=(TextView) findViewById(R.id.userTip);
        SpannableString span  =new SpannableString("当前登陆:胡师傅");
        span.setSpan(new ForegroundColorSpan(Color.RED), 5, 8,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new StyleSpan(Typeface.BOLD), 5, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        UserTip.setText(span);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.test_scan_qrcode:
                startActivity(new Intent(this, TestScanActivity.class));
                break;
            case R.id.log_off:
                Intent intent=new Intent(MainActivity.this,LoginActivity.class);
                SharedPreferences sprfMain;
                SharedPreferences.Editor editorMain;
                sprfMain= PreferenceManager.getDefaultSharedPreferences(this);
                editorMain=sprfMain.edit();
                editorMain.putBoolean("main",false);
                editorMain.commit();
                startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestCodeQRCodePermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CALL_PHONE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "二位码快递信息保护系统需要打开相机和散光灯以及拨打电话的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }
}
