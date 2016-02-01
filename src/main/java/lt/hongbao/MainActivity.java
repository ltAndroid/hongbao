package lt.hongbao;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private  Toolbar toolbar;
    private Button startNotfic;
    private Button startServiceForWX;
    private static final Intent starHongbaoIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);//开启插件的Intent
    private  static  final Intent startNotficIntent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");//开启通知监听的插件
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }
    private void initView() {
        toolbar= (Toolbar) findViewById(R.id.toolbar);
        startNotfic= (Button) findViewById(R.id.startMonitor);
        startServiceForWX= (Button) findViewById(R.id.startServiceForWX);
    }

    private void initData() {
    }

    private void initListener() {
        startNotfic.setOnClickListener(this);
        startServiceForWX.setOnClickListener(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        isOpenThis();
    }
    //是否开启抢红包插件
    private void isOpenThis() {
        boolean isOpenNotification=isOpenNotificationServices();
        boolean isOpenWXService=isOpenHongBao();
        startNotfic.setText(isOpenNotification? "辅助功能已打开" : "点击打开辅助功能");
        startServiceForWX.setText(isOpenWXService ? "红包插件已开启" : "点击打开红包插件");
    }
    /**
     * 是否打开抢红包的服务
     * @return
     */
    private boolean isOpenHongBao() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = manager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo info : runningServices) {
           if (info.getId().equals(getPackageName() + "/.HongbaoService")) {
                return true;
            }
        }
        return false;
    }
    /**
     * 是否打开监听微信红包通知消息的服务
     * @return
     */
    public boolean isOpenNotificationServices() {
        ContentResolver contentResolver = getContentResolver();
        String enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");

        if (!TextUtils.isEmpty(enabledListeners)) {
            return enabledListeners.contains(getPackageName() + "/" + getPackageName() + ".NotificationService");
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //开启辅助功能
            case  R.id.startMonitor:
                startActivity(startNotficIntent);
                break;
            case  R.id.startServiceForWX:
                startActivity(starHongbaoIntent);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
