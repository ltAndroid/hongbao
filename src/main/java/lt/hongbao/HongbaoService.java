package lt.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;


public class HongbaoService extends AccessibilityService {
    private AccessibilityNodeInfo mReceiveNode, mUnpackNode;

    private boolean mLuckyMoneyPicked, mLuckyMoneyReceived, mNeedUnpack, mNeedBack;

    private String lastFetchedHongbaoId = null;
    private long lastFetchedTime = 0;

    private AccessibilityNodeInfo rootNodeInfo;

    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private final static String WECHAT_NOTIFICATION_TIP = "微信红包";

    private static final int MAX_CACHE_TOLERANCE = 5000;
    private boolean mCycle = false;

    String TAG = "hongbao";

    /**
     * AccessibilityEvent的回调方法
     *
     * @param event 事件
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        /* 检测通知消息 */
//        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && !mCycle) {
//            String tip = event.getText().toString();
//
//            if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return;
//
//            Parcelable parcelable = event.getParcelableData();
//            if (parcelable instanceof Notification) {
//                Notification notification = (Notification) parcelable;
//                try {
//                    notification.contentIntent.send();
//                } catch (PendingIntent.CanceledException e) {
//                }
//            }
//            return;
//        }

        this.rootNodeInfo = event.getSource();
        if (rootNodeInfo == null) return;
        mReceiveNode = null;
        mUnpackNode = null;
        checkNodeInfo();

        /* 如果已经接收到红包并且还没有戳开 */
        if (mLuckyMoneyReceived && !mLuckyMoneyPicked && (mReceiveNode != null)) {
            String id = getHongbaoText(mReceiveNode);
            long now = System.currentTimeMillis();
            if (this.shouldReturn(id, now - lastFetchedTime)) {
                return;
            }
            mCycle = true;
            lastFetchedHongbaoId = id;
            lastFetchedTime = now;
            final AccessibilityNodeInfo cellNode = mReceiveNode;
            handle.postDelayed(new Runnable() {//延迟5毫秒,提升
                @Override
                public void run() {
                    cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }, 5);

            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
        }
        /* 如果戳开但还未领取 */
        if (mNeedUnpack && (mUnpackNode != null)) {
            AccessibilityNodeInfo cellNode = mUnpackNode;
            cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mNeedUnpack = false;
        }
        if (mNeedBack) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            mCycle = false;
            mNeedBack = false;
        }
    }

    @Override
    public void onInterrupt() {

    }

    /**
     * 检查节点信息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkNodeInfo() {
        if (this.rootNodeInfo == null) return;

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        List<AccessibilityNodeInfo> nodes1 = this.findAccessibilityNodeInfosByTexts(this.rootNodeInfo, new String[]{
                WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH});
        if (!nodes1.isEmpty()) {
            String nodeId = Integer.toHexString(System.identityHashCode(this.rootNodeInfo));
            if (!nodeId.equals(lastFetchedHongbaoId)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = nodes1.get(nodes1.size() - 1);
            }
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
//        List<AccessibilityNodeInfo> nodes2 = this.findAccessibilityNodeInfosByTexts(this.rootNodeInfo, new String[]{
//                WECHAT_OPEN_CH, WECHAT_OPEN_EN});
//        if (!nodes2.isEmpty()) {
//            mUnpackNode = nodes2;
//            mNeedUnpack = true;
//            return;
//        }
        if (this.rootNodeInfo.getChildCount() == 0) {
            return;
        }
        for (int i = 0, len = this.rootNodeInfo.getChildCount(); i < len; i++) {
            if (!mLuckyMoneyPicked) {
                return;
            }
            AccessibilityNodeInfo node2 = this.rootNodeInfo.getChild(i);
            checkNoe(node2, i + "");
//            if (node2 != null && node2.getClassName().equals("android.widget.Button")) {
//                Log.e(TAG, "红包找到");
//                mUnpackNode = node2;
//                mNeedUnpack = true;
//                break;
//            }
        }
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        if (mLuckyMoneyPicked) {
            List<AccessibilityNodeInfo> nodes3 = this.findAccessibilityNodeInfosByTexts(this.rootNodeInfo, new String[]{
                    WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
                    WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN});
            if (!nodes3.isEmpty()) {
                mNeedBack = true;
                mLuckyMoneyPicked = false;
            }
        }
    }

    public void checkNoe(AccessibilityNodeInfo node, final String s) {
        if (null == node) return;
        final int count = node.getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                checkNoe(childNode, "--" + s + "--" + i);
            }
        } else {
            if (node.getClassName().equals("android.widget.Button")) {
                mUnpackNode = node;
                mNeedUnpack = true;
                return;
            }
        }
    }

    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */
    private String getHongbaoText(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return null;
        }

        return content;
    }

    /**
     * 批量化执行AccessibilityNodeInfo.findAccessibilityNodeInfosByText(text).
     * 由于这个操作影响性能,将所有需要匹配的文字一起处理,尽早返回
     *
     * @param nodeInfo 窗口根节点
     * @param texts    需要匹配的字符串们
     * @return 匹配到的节点数组
     */
    private List<AccessibilityNodeInfo> findAccessibilityNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String[] texts) {
        for (String text : texts) {
            if (text == null) continue;

            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(text);

            if (!nodes.isEmpty()) return nodes;
        }
        return new ArrayList<>();
    }

    /**
     * 判断是否返回,减少点击次数
     * 现在的策略是当红包文本和缓存不一致时,戳
     * 文本一致且间隔大于MAX_CACHE_TOLERANCE时,戳
     *
     * @param id       红包id
     * @param duration 红包到达与缓存的间隔
     * @return 是否应该返回
     */
    private boolean shouldReturn(String id, long duration) {
        // ID为空
        if (id == null) return true;
        // 名称和缓存不一致
        if (duration < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongbaoId)) return true;

        return false;
    }

    Handler handle = new Handler();
}
