package lt.hongbao;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 */
@SuppressWarnings("NewApi")
public class NotificationService extends NotificationListenerService {
   public static  String TAG="NotificationService";
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (null != notification) {
            Bundle extras = notification.extras;
            if (null != extras) {
                List<String> textList = new ArrayList<String>();
                String title = extras.getString("android.title");
                if (!TextUtils.isEmpty(title)) textList.add(title);

                String detailText = extras.getString("android.text");
                if (!TextUtils.isEmpty(detailText)) textList.add(detailText);

                if (textList.size() > 0) {
                    for (String text : textList) {
                        Log.e(TAG, "onNotificationPosted: "+text );
                        if (!TextUtils.isEmpty(text) && text.contains("微信红包")) {
                            final PendingIntent pendingIntent = notification.contentIntent;
                            try {
                                pendingIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}
