package cn.leon.superwechat.task;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Response;

import java.util.ArrayList;
import java.util.HashMap;

import cn.leon.superwechat.I;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.activity.BaseActivity;
import cn.leon.superwechat.bean.Contact;
import cn.leon.superwechat.data.ApiParams;
import cn.leon.superwechat.data.GsonRequest;
import cn.leon.superwechat.data.OkHttpUtils;

/**
 * Created by leon on 2016/5/22.
 */
public class DownloadContactListTask extends BaseActivity {
    public static final String TAG = DownloadContactListTask.class.getName();
    Context mContext;
    String userName;
    String downloadContactListUrl;

    public DownloadContactListTask(Context mContext, String userName) {
        this.mContext = mContext;
        this.userName = userName;
        initDownloadContactListUrl();
    }

    /*http://10.0.2.2:8080/SuperWeChatServer/Server?
    request=download_contact_all_list&m_contact_user_name=*/
    public void initDownloadContactListUrl() {
        try {
            downloadContactListUrl = new ApiParams().with(I.Contact.USER_NAME, userName)
                    .getRequestUrl(I.REQUEST_DOWNLOAD_CONTACT_ALL_LIST);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() {
        executeRequest(new GsonRequest<Contact[]>(downloadContactListUrl, Contact[].class,
                responseDownloadContactListListener(),errorListener()));
    }

    private Response.Listener<Contact[]> responseDownloadContactListListener() {
        return new Response.Listener<Contact[]>() {
            @Override
            public void onResponse(Contact[] contacts) {
                Log.e(TAG,"responseDownloadContactListListener,contacts="+contacts);
                if (contacts == null) {
                    return;
                }
                Log.e(TAG,"responseDownloadContactListListener,contacts.length="+contacts.length);
                ArrayList<Contact> list = OkHttpUtils.array2List(contacts);
                SuperWeChatApplication instance = SuperWeChatApplication.getInstance();
                ArrayList<Contact> contactList = instance.getContactList();
                contactList.clear();
                contactList.addAll(list);
                HashMap<String, Contact> userList = SuperWeChatApplication.getInstance().getUserList();
                userList.clear();
                for (Contact contact:
                     list) {
                    userList.put(contact.getMContactCname(), contact);
                }
                mContext.sendStickyBroadcast(new Intent("update_contact_List"));
            }
        };
    }

}
