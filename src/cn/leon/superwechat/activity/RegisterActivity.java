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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.easemob.EMError;
import com.easemob.chat.EMChatManager;

import cn.leon.superwechat.I;
import cn.leon.superwechat.R;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.bean.Message;
import cn.leon.superwechat.data.OkHttpUtils;
import cn.leon.superwechat.listener.OnSetAvatarListener;
import cn.leon.superwechat.utils.ImageUtils;
import cn.leon.superwechat.utils.Utils;

import com.easemob.exceptions.EaseMobException;

import java.io.File;

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
	private String avatarName;/**定义临时变量保存	头像名称，以便上传头像时调用*/

	String username;
	String nick;
	String pwd;
	String confirm_pwd;

	ProgressDialog pd;/**正在注册旋转dialog*/

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

	/**判断mOnSetAvatarListener的回调函数是否成功*/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			mOnSetAvatarListener.setAvatar(requestCode, data, mivAvatar);
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
		userNickEditText = (EditText) findViewById(cn.leon.superwechat.R.id.nick);
		passwordEditText = (EditText) findViewById(cn.leon.superwechat.R.id.et_password);
		confirmPwdEditText = (EditText) findViewById(cn.leon.superwechat.R.id.confirm_password);
	}

	/**
	 * 注册
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
					userNameEditText.setError(getResources().getString(R.string.User_name_cannot_be_empty));
					return;
				} else if (!username.matches("[\\w][\\w\\d_]+")) {
					userNameEditText.requestFocus();
					userNameEditText.setError(getResources().getString(R.string.User_name_cannot_be_wd));
				} else if (TextUtils.isEmpty(nick)) {
					userNickEditText.requestFocus();
					userNickEditText.setError(getResources().getString(R.string.Nick_name_cannot_be_empty));
				} else if (TextUtils.isEmpty(pwd)) {
					passwordEditText.requestFocus();
					passwordEditText.setError(getResources().getString(R.string.Password_cannot_be_empty));
					return;
				} else if (TextUtils.isEmpty(confirm_pwd)) {
					confirmPwdEditText.requestFocus();
					confirmPwdEditText.setError(getResources().getString(R.string.Confirm_password_cannot_be_empty));
					return;
				} else if (!pwd.equals(confirm_pwd)) {
					confirmPwdEditText.requestFocus();
					confirmPwdEditText.setError(getResources().getString(R.string.Two_input_password));
					return;
				}

				if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
					pd = new ProgressDialog(mContext);
					pd.setMessage(getResources().getString(cn.leon.superwechat.R.string.Is_the_registered));
					pd.show();
					registerAppServer();
				}
			}
		});

	}

	/**
	 * 1.首先注册远端服务器账号，并上传头像---okhttp
	 * 2.注册环信账号
	 * 3.如果环信的服务器注册失败，调用取消注册的方法，删除远端账号和图片---unRegister,okhttp
	 */
	private void registerAppServer() {
		/**返回头像在sd卡文件夹的绝对路径*/
		File file = new File(ImageUtils.getAvatarPath(mContext, I.AVATAR_TYPE_USER_PATH),
				avatarName + I.AVATAR_SUFFIX_JPG);
		OkHttpUtils<Message> utils = new OkHttpUtils<Message>();
		utils.url(SuperWeChatApplication.SERVER_ROOT)
				.addParam(I.KEY_REQUEST, I.REQUEST_REGISTER)
				.addParam(I.User.USER_NAME, username)
				.addParam(I.User.PASSWORD, pwd)
				.addParam(I.User.NICK, nick)
				.targetClass(Message.class)
				.addFile(file)
				.execute(new OkHttpUtils.OnCompleteListener<Message>() {
					@Override
					public void onSuccess(Message result) {
						if (result.isResult()) {
							registerEMServer();
						} else {
							pd.dismiss();
							Utils.showToast(mContext, Utils.getResourceString(mContext, result.getMsg()), Toast.LENGTH_LONG);
							Log.e(TAG, "register fail,error:" + result.getMsg());
						}
					}

					@Override
					public void onError(String error) {
						pd.dismiss();
						Utils.showToast(mContext, error, Toast.LENGTH_LONG);
						Log.e(TAG, "register fail,error:" + error);
					}
				});
	}

	/**注册环信的账号*/
	public void registerEMServer() {
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
					unRegister();
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


	public void back(View view) {
		finish();
	}

	public String getAvatarName() {
		avatarName = System.currentTimeMillis() + "";
		return avatarName;
	}

	private void unRegister() {
		OkHttpUtils<Message> utils = new OkHttpUtils<Message>();
		utils.url(SuperWeChatApplication.SERVER_ROOT)
				.addParam(I.KEY_REQUEST, I.REQUEST_UNREGISTER)
				.addParam(I.User.USER_NAME, username)
				.targetClass(Message.class)
				.execute(new OkHttpUtils.OnCompleteListener<Message>() {
					@Override
					public void onSuccess(Message result) {
						pd.dismiss();
						Utils.showToast(mContext,R.string.Registration_failed,Toast.LENGTH_SHORT);
					}

					@Override
					public void onError(String error) {
						pd.dismiss();
						Log.e(TAG, error);
					}
				});
	}
}