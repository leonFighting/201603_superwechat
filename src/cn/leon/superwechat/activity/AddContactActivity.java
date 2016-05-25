/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.leon.superwechat.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.leon.superwechat.I;
import cn.leon.superwechat.R;
import cn.leon.superwechat.applib.controller.HXSDKHelper;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.easemob.chat.EMContactManager;

import java.util.ArrayList;

import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.DemoHXSDKHelper;
import cn.leon.superwechat.bean.Contact;
import cn.leon.superwechat.bean.User;
import cn.leon.superwechat.data.ApiParams;
import cn.leon.superwechat.data.GsonRequest;
import cn.leon.superwechat.utils.UserUtils;

public class AddContactActivity extends BaseActivity {
    private EditText editText;
    private LinearLayout searchedUserLayout;
    private TextView nameText, mTextView;
    private Button searchBtn;
    private NetworkImageView avatar;
    private InputMethodManager inputMethodManager;
    private String toAddUsername;
    private ProgressDialog progressDialog;
    private TextView mtvNoting;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(cn.leon.superwechat.R.layout.activity_add_contact);
        mTextView = (TextView) findViewById(cn.leon.superwechat.R.id.add_list_friends);

        editText = (EditText) findViewById(cn.leon.superwechat.R.id.edit_note);
        String strAdd = getResources().getString(cn.leon.superwechat.R.string.add_friend);
        mTextView.setText(strAdd);
        String strUserName = getResources().getString(cn.leon.superwechat.R.string.user_name);
        editText.setHint(strUserName);
        searchedUserLayout = (LinearLayout) findViewById(cn.leon.superwechat.R.id.ll_user);
        nameText = (TextView) findViewById(cn.leon.superwechat.R.id.name);
        searchBtn = (Button) findViewById(cn.leon.superwechat.R.id.search);
        avatar = (NetworkImageView) findViewById(cn.leon.superwechat.R.id.avatar);
        mtvNoting = (TextView) findViewById(R.id.tv_noting_show);
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        setListener();
    }

    private void setListener() {
        setSearchContactListener();
        setAddContactLister();
    }

    private void setSearchContactListener() {
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String name = editText.getText().toString();
                String saveText = searchBtn.getText().toString();
                //输入用户名,非空验证
                if (TextUtils.isEmpty(name)) {
                    String st = getResources().getString(R.string.Please_enter_a_username);
                    startActivity(new Intent(mContext, AlertDialog.class).putExtra("msg", st));
                    return;
                }
                //不能添加自己
                if (SuperWeChatApplication.getInstance().getUserName().equals(name)) {
                    String str = getString(cn.leon.superwechat.R.string.not_add_myself);
                    startActivity(new Intent(mContext, AlertDialog.class).putExtra("msg", str));
                    return;
                }
                //搜索到的好友name赋值给toAddUsername
                toAddUsername = name;
                //访问远端服务器查看有无账号
                try {
                    String path = new ApiParams()
                            .with(I.User.USER_NAME, name)
                            .getRequestUrl(I.REQUEST_FIND_USER);
                    executeRequest(new GsonRequest<User>(path,User.class,
                            ResponseListener(),errorListener()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Response.Listener<User> ResponseListener() {
        return new Response.Listener<User>() {
            @Override
            public void onResponse(User user) {
                if (user != null) {
                    ArrayList<Contact> contactList =
                            SuperWeChatApplication.getInstance().getContactList();
                    //搜索是好友账号则跳转到好友资料
                    if (contactList.contains(user)) {
                        Intent intent = new Intent();
                        intent.setClass(mContext, UserProfileActivity.class);
                        intent.putExtra("username", user.getMUserName());
                        mContext.startActivity(intent);
                    } else {
                        //服务器存在此用户，显示此用户和添加按钮
                        searchedUserLayout.setVisibility(View.VISIBLE);
                        UserUtils.setUserBeanAvatar(user,avatar);
                        UserUtils.setUserBeanNick(user,nameText);
                    }
                } else {
                    mtvNoting.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private void setAddContactLister() {
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //将自己用户名，和对方用户名发送到环信服务器，环信推送好友请求
                if (((DemoHXSDKHelper) HXSDKHelper.getInstance()).getContactList().containsKey(nameText.getText().toString())) {
                    //提示已在好友列表中，无需添加
                    if (EMContactManager.getInstance().getBlackListUsernames().contains(nameText.getText().toString())) {
                        startActivity(new Intent(mContext, AlertDialog.class).putExtra("msg", "此用户已是你好友(被拉黑状态)，从黑名单列表中移出即可"));
                        return;
                    }
                    String strin = getString(cn.leon.superwechat.R.string.This_user_is_already_your_friend);
                    startActivity(new Intent(mContext, AlertDialog.class).putExtra("msg", strin));
                    return;
                }

                progressDialog = new ProgressDialog(mContext);
                String stri = getResources().getString(cn.leon.superwechat.R.string.Is_sending_a_request);
                progressDialog.setMessage(stri);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                new Thread(new Runnable() {
                    public void run() {

                        try {
                            //demo写死了个reason，实际应该让用户手动填入
                            String s = getResources().getString(cn.leon.superwechat.R.string.Add_a_friend);
                            EMContactManager.getInstance().addContact(toAddUsername, s);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    progressDialog.dismiss();
                                    String s1 = getResources().getString(cn.leon.superwechat.R.string.send_successful);
                                    Toast.makeText(getApplicationContext(), s1, Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    progressDialog.dismiss();
                                    String s2 = getResources().getString(cn.leon.superwechat.R.string.Request_add_buddy_failure);
                                    Toast.makeText(getApplicationContext(), s2 + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    public void back(View v) {
        finish();
    }


}
