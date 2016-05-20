/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.leon.superwechat.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.opengl.ETC1;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.easemob.EMError;
import com.easemob.chat.EMChatManager;

import cn.leon.superwechat.I;
import cn.leon.superwechat.R;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.listener.OnSetAvatarListener;

import com.easemob.exceptions.EaseMobException;

/**
 * 注册页
 */
public class RegisterActivity extends BaseActivity {
	private final static String TAG = RegisterActivity.class.getName();
	private EditText userNameEditText;
	private EditText userNickEditText;
	private EditText passwordEditText;
	private EditText confirmPwdEditText;
	Activity mContext;
	ImageView mivAvatar;
	OnSetAvatarListener mOnSetAvatarListener;
	private String avatarName;

	String username;
	String nick;
	String pwd;
	String confirm_pwd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(cn.leon.superwechat.R.layout.activity_register);
		mContext = this;
		initView();
		setListener();
	}

	private void setListener() {
		setOnRegisterListener();
		setOnLoginListener();
		setOnAvatarListener();
	}

	private void setOnAvatarListener() {
		findViewById(R.id.layout_user_avatar).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mOnSetAvatarListener = new OnSetAvatarListener(mContext, R.id.layout_Register, getAvatarName(), I.AVATAR_TYPE_USER_PATH);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			mOnSetAvatarListener.setAvatar(requestCode,data,mivAvatar);
		}
	}

	private void setOnLoginListener() {
		findViewById(R.id.BtnLogin).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				back(view);
			}
		});
	}

	private void initView() {
		mivAvatar = (ImageView) findViewById(R.id.iv_avatar);
		userNameEditText = (EditText) findViewById(cn.leon.superwechat.R.id.username);
		userNickEditText = (EditText) findViewById(R.id.nick);
		passwordEditText = (EditText) findViewById(cn.leon.superwechat.R.id.password);
		confirmPwdEditText = (EditText) findViewById(cn.leon.superwechat.R.id.confirm_password);
	}

	/**
	 * 注册
	 *
	 */
	public void setOnRegisterListener() {
		findViewById(R.id.BtnRegister).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				username = userNameEditText.getText().toString().trim();
				nick = userNickEditText.getText().toString().trim();
				pwd = passwordEditText.getText().toString().trim();
				confirm_pwd = confirmPwdEditText.getText().toString().trim();
				if (TextUtils.isEmpty(username)) {
					userNameEditText.requestFocus();
					userNameEditText.setError(getResources().getString(cn.leon.superwechat.R.string.User_name_cannot_be_empty));
					return;
				} else if (username.matches("[\\w][\\w\\d_]+")) {
					userNameEditText.requestFocus();
					userNameEditText.setError(getResources().getString(cn.leon.superwechat.R.string.User_name_cannot_be_wd));
				} else if (TextUtils.isEmpty(nick)) {
					userNickEditText.requestFocus();
					userNameEditText.setError(getResources().getString(cn.leon.superwechat.R.string.Nick_name_cannot_be_empty));
				} else if (TextUtils.isEmpty(pwd)) {
					passwordEditText.requestFocus();
					userNameEditText.setError(getResources().getString(cn.leon.superwechat.R.string.Password_cannot_be_empty));
					return;
				} else if (TextUtils.isEmpty(confirm_pwd)) {
					confirmPwdEditText.requestFocus();
					confirmPwdEditText.setError(getResources().getString(cn.leon.superwechat.R.string.Confirm_password_cannot_be_empty));
					return;
				} else if (!pwd.equals(confirm_pwd)) {
					confirmPwdEditText.requestFocus();
					confirmPwdEditText.setError(getResources().getString(cn.leon.superwechat.R.string.Two_input_password));
					return;
				}

				if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
					final ProgressDialog pd = new ProgressDialog(mContext);
					pd.setMessage(getResources().getString(cn.leon.superwechat.R.string.Is_the_registered));
					pd.show();

					new Thread(new Runnable() {
						public void run() {
							try {
								// 调用sdk注册方法
								EMChatManager.getInstance().createAccountOnServer(username, pwd);
								runOnUiThread(new Runnable() {
									public void run() {
										if (!RegisterActivity.this.isFinishing())
											pd.dismiss();
										// 保存用户名
										SuperWeChatApplication.getInstance().setUserName(username);
										Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
										finish();
									}
								});
							} catch (final EaseMobException e) {
								runOnUiThread(new Runnable() {
									public void run() {
										if (!RegisterActivity.this.isFinishing())
											pd.dismiss();
										int errorCode = e.getErrorCode();
										if (errorCode == EMError.NONETWORK_ERROR) {
											Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.network_anomalies), Toast.LENGTH_SHORT).show();
										} else if (errorCode == EMError.USER_ALREADY_EXISTS) {
											Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.User_already_exists), Toast.LENGTH_SHORT).show();
										} else if (errorCode == EMError.UNAUTHORIZED) {
											Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
										} else if (errorCode == EMError.ILLEGAL_USER_NAME) {
											Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(getApplicationContext(), getResources().getString(cn.leon.superwechat.R.string.Registration_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
										}
									}
								});
							}
						}
					}).start();

				}
			}
		});

	}

	public void back(View view) {
		finish();
	}

	public String getAvatarName() {
		avatarName = System.currentTimeMillis() + "";
		return avatarName;
	}
}
