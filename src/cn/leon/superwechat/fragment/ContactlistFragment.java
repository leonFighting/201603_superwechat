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
package cn.leon.superwechat.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.activity.AddContactActivity;
import cn.leon.superwechat.activity.ChatActivity;
import cn.leon.superwechat.activity.GroupsActivity;
import cn.leon.superwechat.activity.MainActivity;
import cn.leon.superwechat.activity.NewFriendsMsgActivity;
import cn.leon.superwechat.activity.PublicChatRoomsActivity;
import cn.leon.superwechat.activity.RobotsActivity;
import cn.leon.superwechat.applib.controller.HXSDKHelper;
import cn.leon.superwechat.applib.controller.HXSDKHelper.HXSyncListener;
import com.easemob.chat.EMContactManager;
import cn.leon.superwechat.Constant;
import cn.leon.superwechat.DemoHXSDKHelper;
import cn.leon.superwechat.adapter.ContactAdapter;
import cn.leon.superwechat.bean.Contact;
import cn.leon.superwechat.db.InviteMessgeDao;
import cn.leon.superwechat.db.EMUserDao;
import cn.leon.superwechat.domain.EMUser;
import cn.leon.superwechat.utils.UserUtils;
import cn.leon.superwechat.widget.Sidebar;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;

/**
 * 联系人列表页
 * 
 */
public class ContactlistFragment extends Fragment {
	public static final String TAG = "ContactlistFragment";
	private ContactAdapter adapter;
	private ListView listView;
	private boolean hidden;
	private Sidebar sidebar;
	private InputMethodManager inputMethodManager;
	private List<String> blackList;
	ImageButton clearSearch;
	EditText query;
	HXContactSyncListener contactSyncListener;
	HXBlackListSyncListener blackListSyncListener;
	HXContactInfoSyncListener contactInfoSyncListener;
	View progressBar;
	Handler handler = new Handler();
    private Contact toBeProcessUser;
    private String toBeProcessUsername;
	ContactListChangedReceiver mReceiver;
	ArrayList<Contact> mContactist = new ArrayList<Contact>();

	class HXContactSyncListener implements HXSyncListener {
		@Override
		public void onSyncSucess(final boolean success) {
			EMLog.d(TAG, "on contact list sync success:" + success);
			ContactlistFragment.this.getActivity().runOnUiThread(new Runnable() {
				public void run() {
				    getActivity().runOnUiThread(new Runnable(){

		                @Override
		                public void run() {
		                    if(success){
		                        progressBar.setVisibility(View.GONE);
                                refresh();
		                    }else{
		                        String s1 = getResources().getString(cn.leon.superwechat.R.string.get_failed_please_check);
		                        Toast.makeText(getActivity(), s1, Toast.LENGTH_LONG).show();
		                        progressBar.setVisibility(View.GONE);
		                    }
		                }

		            });
				}
			});
		}
	}

	class HXBlackListSyncListener implements HXSyncListener{

        @Override
        public void onSyncSucess(boolean success) {
            getActivity().runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    blackList = EMContactManager.getInstance().getBlackListUsernames();
                    refresh();
                }

            });
        }

	};

	class HXContactInfoSyncListener implements HXSyncListener{

		@Override
		public void onSyncSucess(final boolean success) {
			EMLog.d(TAG, "on contactinfo list sync success:" + success);
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					progressBar.setVisibility(View.GONE);
					if(success){
						refresh();
					}
				}
			});
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(cn.leon.superwechat.R.layout.fragment_contact_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//防止被T后，没点确定按钮然后按了home键，长期在后台又进app导致的crash
		if(savedInstanceState != null && savedInstanceState.getBoolean("isConflict", false))
		    return;
		inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		listView = (ListView) getView().findViewById(cn.leon.superwechat.R.id.list);
		sidebar = (Sidebar) getView().findViewById(cn.leon.superwechat.R.id.sidebar);
		sidebar.setListView(listView);
		//黑名单列表
		blackList = EMContactManager.getInstance().getBlackListUsernames();
		// 获取设置contactlist
		getContactList();
		
		//搜索框
		query = (EditText) getView().findViewById(cn.leon.superwechat.R.id.query);
		query.setHint(cn.leon.superwechat.R.string.search);
		clearSearch = (ImageButton) getView().findViewById(cn.leon.superwechat.R.id.search_clear);
		query.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				adapter.getFilter().filter(s);
				if (s.length() > 0) {
					clearSearch.setVisibility(View.VISIBLE);
				} else {
					clearSearch.setVisibility(View.INVISIBLE);
					
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});
		clearSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				query.getText().clear();
				hideSoftKeyboard();
			}
		});
		
		// 设置adapter
		adapter = new ContactAdapter(getActivity(), cn.leon.superwechat.R.layout.row_contact, mContactist);
		listView.setAdapter(adapter);
		setListener();

		progressBar = (View) getView().findViewById(cn.leon.superwechat.R.id.progress_bar);

		contactSyncListener = new HXContactSyncListener();
		HXSDKHelper.getInstance().addSyncContactListener(contactSyncListener);
		
		blackListSyncListener = new HXBlackListSyncListener();
		HXSDKHelper.getInstance().addSyncBlackListListener(blackListSyncListener);
		
		contactInfoSyncListener = new HXContactInfoSyncListener();
		((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().addSyncContactInfoListener(contactInfoSyncListener);
		
		if (!HXSDKHelper.getInstance().isContactsSyncedWithServer()) {
			progressBar.setVisibility(View.VISIBLE);
		} else {
			progressBar.setVisibility(View.GONE);
		}
		registerContactListChangedReceiver();
	}

	private void setListener() {
		setContactItemClickListener();
		setContactListTouchListener();
		setAddContactListener();
	}

	private void setAddContactListener() {
		ImageView addContactView = (ImageView) getView().findViewById(cn.leon.superwechat.R.id.iv_new_contact);
		// 进入添加好友页
		addContactView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), AddContactActivity.class));
			}
		});
		registerForContextMenu(listView);
	}

	private void setContactListTouchListener() {
		listView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 隐藏软键盘
				if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
					if (getActivity().getCurrentFocus() != null)
						inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
								InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}
		});
	}

	private void setContactItemClickListener() {
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String username = adapter.getItem(position).getMContactCname();
				if (Constant.NEW_FRIENDS_USERNAME.equals(username)) {
					// 进入申请与通知页面
					EMUser user = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getContactList().get(Constant.NEW_FRIENDS_USERNAME);
					user.setUnreadMsgCount(0);
					startActivity(new Intent(getActivity(), NewFriendsMsgActivity.class));
				} else if (Constant.GROUP_USERNAME.equals(username)) {
					// 进入群聊列表页面
					startActivity(new Intent(getActivity(), GroupsActivity.class));
				} else if(Constant.CHAT_ROOM.equals(username)){
					//进入聊天室列表页面
					startActivity(new Intent(getActivity(), PublicChatRoomsActivity.class));
				}else if(Constant.CHAT_ROBOT.equals(username)){
					//进入Robot列表页面
					startActivity(new Intent(getActivity(), RobotsActivity.class));
				}else {
					// demo中直接进入聊天页面，实际一般是进入用户详情页
					startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("userId", adapter.getItem(position).getMContactCname()));
				}
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (((AdapterContextMenuInfo) menuInfo).position > 1) {
		    toBeProcessUser = adapter.getItem(((AdapterContextMenuInfo) menuInfo).position);
		    toBeProcessUsername = toBeProcessUser.getMContactCname();
			getActivity().getMenuInflater().inflate(cn.leon.superwechat.R.menu.context_contact_list, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == cn.leon.superwechat.R.id.delete_contact) {
			try {
                // 删除此联系人
                deleteContact(toBeProcessUser);
                // 删除相关的邀请消息
                InviteMessgeDao dao = new InviteMessgeDao(getActivity());
                dao.deleteMessage(toBeProcessUser.getMContactCname());
            } catch (Exception e) {
                e.printStackTrace();
            }
			return true;
		}else if(item.getItemId() == cn.leon.superwechat.R.id.add_to_blacklist){
			moveToBlacklist(toBeProcessUsername);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		this.hidden = hidden;
		if (!hidden) {
			refresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!hidden) {
			refresh();
		}
	}

	/**
	 * 删除联系人
	 * 
	 * @param tobeDeleteUser
	 */
	public void deleteContact(final Contact tobeDeleteUser) {
		String st1 = getResources().getString(cn.leon.superwechat.R.string.deleting);
		final String st2 = getResources().getString(cn.leon.superwechat.R.string.Delete_failed);
		final ProgressDialog pd = new ProgressDialog(getActivity());
		pd.setMessage(st1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					EMContactManager.getInstance().deleteContact(tobeDeleteUser.getMContactCname());
					// 删除db和内存中此用户的数据
					EMUserDao dao = new EMUserDao(getActivity());
					dao.deleteContact(tobeDeleteUser.getMContactCname());
					((DemoHXSDKHelper)HXSDKHelper.getInstance()).getContactList().remove(tobeDeleteUser.getMContactCname());
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							adapter.remove(tobeDeleteUser);
							adapter.notifyDataSetChanged();

						}
					});
				} catch (final Exception e) {
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st2 + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});

				}

			}
		}).start();

	}

	/**
	 * 把user移入到黑名单
	 */
	private void moveToBlacklist(final String username){
		final ProgressDialog pd = new ProgressDialog(getActivity());
		String st1 = getResources().getString(cn.leon.superwechat.R.string.Is_moved_into_blacklist);
		final String st2 = getResources().getString(cn.leon.superwechat.R.string.Move_into_blacklist_success);
		final String st3 = getResources().getString(cn.leon.superwechat.R.string.Move_into_blacklist_failure);
		pd.setMessage(st1);
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					//加入到黑名单
					EMContactManager.getInstance().addUserToBlackList(username,false);
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st2, Toast.LENGTH_SHORT).show();
							refresh();
						}
					});
				} catch (EaseMobException e) {
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							Toast.makeText(getActivity(), st3, Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).start();
		
	}
	
	// 刷新ui
	public void refresh() {
		try {
			// 可能会在子线程中调到这方法
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					getContactList();
					adapter.notifyDataSetChanged();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		if (contactSyncListener != null) {
			HXSDKHelper.getInstance().removeSyncContactListener(contactSyncListener);
			contactSyncListener = null;
		}
		
		if(blackListSyncListener != null){
		    HXSDKHelper.getInstance().removeSyncBlackListListener(blackListSyncListener);
		}
		
		if(contactInfoSyncListener != null){
			((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().removeSyncContactInfoListener(contactInfoSyncListener);
		}
		if (mReceiver != null) {
			getActivity().unregisterReceiver(mReceiver);
		}
		super.onDestroy();
	}
	
	public void showProgressBar(boolean show) {
		if (progressBar != null) {
			if (show) {
				progressBar.setVisibility(View.VISIBLE);
			} else {
				progressBar.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * 获取联系人列表，并过滤掉黑名单和排序
	 */
	private void getContactList() {
		ArrayList<Contact> contactList = SuperWeChatApplication.getInstance().getContactList();
		mContactist.clear();
		//获取本地好友列表
		mContactist.addAll(contactList);
		// 添加"群聊"
		Contact groupUser = new Contact();
		groupUser.setMContactId(-2);
		String strGroup = getActivity().getString(cn.leon.superwechat.R.string.group_chat);
		groupUser.setMContactCname(Constant.GROUP_USERNAME);
		groupUser.setMUserName(Constant.GROUP_USERNAME);
		groupUser.setMUserNick(strGroup);
		groupUser.setHeader("");
		if (mContactist.indexOf(groupUser)==-1) {
			mContactist.add(0,groupUser);
		}
		// 添加user"申请与通知"
		Contact newFriends = new Contact();
		newFriends.setMContactId(-1);
		newFriends.setMContactCname(Constant.NEW_FRIENDS_USERNAME);
		newFriends.setMUserName(Constant.GROUP_USERNAME);
		String strChat = getActivity().getString(cn.leon.superwechat.R.string.Application_and_notify);
		newFriends.setMUserNick(strChat);
		newFriends.setHeader("");
		if (mContactist.indexOf(newFriends)==-1) {
			mContactist.add(0,newFriends);
		}
		for (Contact contact : mContactist) {
			UserUtils.setUserHearder(contact.getMContactCname(),contact);
		}
		// 排序
		Collections.sort(this.mContactist, new Comparator<Contact>() {

			@Override
			public int compare(Contact lhs, Contact rhs) {
				return lhs.getHeader().compareTo(rhs.getHeader());
			}
		});


//        if(users.get(Constant.GROUP_USERNAME) != null)
//            this.contactList.add(0, users.get(Constant.GROUP_USERNAME));
//
//		// 把"申请与通知"添加到首位
//		if(users.get(Constant.NEW_FRIENDS_USERNAME) != null)
//		    this.contactList.add(0, users.get(Constant.NEW_FRIENDS_USERNAME));
		
	}
	
	void hideSoftKeyboard() {
        if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getActivity().getCurrentFocus() != null)
                inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	    if(((MainActivity)getActivity()).isConflict){
	    	outState.putBoolean("isConflict", true);
	    }else if(((MainActivity)getActivity()).getCurrentAccountRemoved()){
	    	outState.putBoolean(Constant.ACCOUNT_REMOVED, true);
	    }
	    
	}

	class ContactListChangedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			refresh();
		}
	}

	public void registerContactListChangedReceiver() {
		mReceiver = new ContactListChangedReceiver();
		IntentFilter filter = new IntentFilter("update_contact_List");
		getActivity().registerReceiver(mReceiver, filter);
	}
}
