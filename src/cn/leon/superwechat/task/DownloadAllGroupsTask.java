package cn.leon.superwechat.task;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Response;

import java.util.ArrayList;

import cn.leon.superwechat.I;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.activity.BaseActivity;
import cn.leon.superwechat.bean.Group;
import cn.leon.superwechat.data.ApiParams;
import cn.leon.superwechat.data.GsonRequest;
import cn.leon.superwechat.data.OkHttpUtils;

/**
 * Created by leon on 2016/5/22.
 */
public class DownloadAllGroupsTask extends BaseActivity {
    private final String TAG = DownloadAllGroupsTask.class.getName();
    Context mContext;
    String userName;
    String downloadAllGroupsUrl;
    public DownloadAllGroupsTask(Context mContext, String userName) {
        this.mContext = mContext;
        this.userName = userName;
        initDownloadAllGroupsUrl();
    }
//    http://10.0.2.2:8080/SuperWeChatServer/Server?request=download_contact_all_list&m_contact_user_name=
    public void initDownloadAllGroupsUrl() {
        try {
            downloadAllGroupsUrl= new ApiParams().with(I.User.USER_NAME, userName)
                    .getRequestUrl(I.REQUEST_DOWNLOAD_GROUPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() {
        executeRequest(new GsonRequest<Group[]>(downloadAllGroupsUrl,Group[].class
        ,ResponseDownloadAllGroupsTaskListener(),errorListener()));
    }

    private Response.Listener<Group[]> ResponseDownloadAllGroupsTaskListener() {
        return new Response.Listener<Group[]>() {
            @Override
            public void onResponse(Group[] groups) {
                Log.e(TAG,"ResponseDownloadContactListListener,groups="+groups);
                if (groups == null) {
                    return;
                }
                ArrayList<Group> list = OkHttpUtils.array2List(groups);
                SuperWeChatApplication instance = SuperWeChatApplication.getInstance();
                ArrayList<Group> groupList = instance.getGroupList();
                groupList.clear();
                groupList.addAll(list);
                mContext.sendStickyBroadcast(new Intent("update_group_list"));
            }
        };
    }


}
