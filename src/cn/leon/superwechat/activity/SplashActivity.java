package cn.leon.superwechat.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroupManager;

import cn.leon.superwechat.DemoHXSDKHelper;
import cn.leon.superwechat.I;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.bean.User;
import cn.leon.superwechat.db.UserDao;
import cn.leon.superwechat.task.DownloadAllGroupsTask;
import cn.leon.superwechat.task.DownloadContactListTask;
import cn.leon.superwechat.task.DownloadPublicGroupsTask;

/**
 * 开屏页
 */
public class SplashActivity extends BaseActivity {
    Context mContext;
    private RelativeLayout rootLayout;
    private TextView versionText;

    private static final int sleepTime = 2000;

    @Override
    protected void onCreate(Bundle arg0) {
        mContext = this;
        setContentView(cn.leon.superwechat.R.layout.activity_splash);
        super.onCreate(arg0);

        rootLayout = (RelativeLayout) findViewById(cn.leon.superwechat.R.id.splash_root);
        versionText = (TextView) findViewById(cn.leon.superwechat.R.id.tv_version);

        versionText.setText(getVersion());
        AlphaAnimation animation = new AlphaAnimation(0.3f, 1.0f);
        animation.setDuration(1500);
        rootLayout.startAnimation(animation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DemoHXSDKHelper.getInstance().isLogined()) {
            String userName = SuperWeChatApplication.getInstance().getUserName();
            UserDao userDao = new UserDao(mContext);
            User user = userDao.findUserByName(userName);
            SuperWeChatApplication.getInstance().setUser(user);
            new DownloadContactListTask(mContext, userName).execute();
            new DownloadAllGroupsTask(mContext, userName).execute();
            new DownloadPublicGroupsTask(mContext, userName, I.PAGE_ID_DEFAULT, I.PAGE_SIZE_DEFAULT).execute();
        }
        new Thread(new Runnable() {
            public void run() {
                if (DemoHXSDKHelper.getInstance().isLogined()) {
                    // ** 免登陆情况 加载所有本地群和会话
                    //不是必须的，不加sdk也会自动异步去加载(不会重复加载)；
                    //加上的话保证进了主页面会话和群组都已经load完毕
                    long start = System.currentTimeMillis();
                    EMGroupManager.getInstance().loadAllGroups();
                    EMChatManager.getInstance().loadAllConversations();
                    long costTime = System.currentTimeMillis() - start;
                    //等待sleeptime时长
                    if (sleepTime - costTime > 0) {
                        try {
                            Thread.sleep(sleepTime - costTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    //进入主页面
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                } else {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                    }
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        }).start();

    }

    /**
     * 获取当前应用程序的版本号
     */
    private String getVersion() {
        String st = getResources().getString(cn.leon.superwechat.R.string.Version_number_is_wrong);
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packinfo = pm.getPackageInfo(getPackageName(), 0);
            String version = packinfo.versionName;
            return version;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return st;
        }
    }
}
