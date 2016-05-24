package cn.leon.superwechat.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import cn.leon.superwechat.Constant;
import cn.leon.superwechat.I;
import cn.leon.superwechat.R;
import cn.leon.superwechat.SuperWeChatApplication;
import cn.leon.superwechat.applib.controller.HXSDKHelper;
import cn.leon.superwechat.DemoHXSDKHelper;
import cn.leon.superwechat.bean.Contact;
import cn.leon.superwechat.bean.User;
import cn.leon.superwechat.data.RequestManager;
import cn.leon.superwechat.domain.EMUser;

import com.android.volley.toolbox.NetworkImageView;
import com.easemob.util.HanziToPinyin;
import com.squareup.picasso.Picasso;

public class UserUtils {
    /**
     * 根据username获取相应user，由于demo没有真实的用户数据，这里给的模拟的数据；
     * @param username
     * @return
     */
    public static EMUser getUserInfo(String username){
        EMUser user = ((DemoHXSDKHelper) HXSDKHelper.getInstance()).getContactList().get(username);
        if(user == null){
            user = new EMUser(username);
        }

        if(user != null){
            //demo没有这些数据，临时填充
        	if(TextUtils.isEmpty(user.getNick()))
        		user.setNick(username);
        }
        return user;
    }

	public static Contact getUserBeanInfo(String userName) {
		Contact contact = SuperWeChatApplication.getInstance().getUserList().get(userName);
		return contact;
	}

	/**
     * 设置用户头像
     * @param username
     */
    public static void setUserAvatar(Context context, String username, ImageView imageView){
    	EMUser user = getUserInfo(username);
        if(user != null && user.getAvatar() != null){
            Picasso.with(context).load(user.getAvatar()).placeholder(cn.leon.superwechat.R.drawable.default_avatar).into(imageView);
        }else{
            Picasso.with(context).load(cn.leon.superwechat.R.drawable.default_avatar).into(imageView);
        }
    }

	public static void setUserBeanAvatar(String userName, NetworkImageView imageView) {
		Contact contact = getUserBeanInfo(userName);
		if (contact != null && contact.getMContactCname() != null) {
			setUserAvatar(getUserAvatarPath(userName),imageView);
		}
	}

	private static void setUserAvatar(String url, NetworkImageView imageView) {
		if (url == null || url.isEmpty() || imageView == null) {
			return;
		}
		imageView.setDefaultImageResId(R.drawable.default_avatar);
		imageView.setImageUrl(url, RequestManager.getImageLoader());
		imageView.setErrorImageResId(R.drawable.default_avatar);
	}

	private static String getUserAvatarPath(String userName) {
		if (userName == null || userName.isEmpty()) {
			return null;
		}
		return I.REQUEST_DOWNLOAD_AVATAR_USER + userName;
	}

	/**
     * 设置当前用户头像
     */
	public static void setCurrentUserAvatar(Context context, ImageView imageView) {
		EMUser user = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().getCurrentUserInfo();
		if (user != null && user.getAvatar() != null) {
			Picasso.with(context).load(user.getAvatar()).placeholder(cn.leon.superwechat.R.drawable.default_avatar).into(imageView);
		} else {
			Picasso.with(context).load(cn.leon.superwechat.R.drawable.default_avatar).into(imageView);
		}
	}

	public static void setCurrentUserBeanAvater(NetworkImageView imageView) {
		User user = SuperWeChatApplication.getInstance().getUser();
		imageView.setDefaultImageResId(R.drawable.default_avatar);
		if (user != null) {
			imageView.setImageUrl(I.REQUEST_DOWNLOAD_AVATAR_USER + user.getMUserName(), RequestManager.getImageLoader());
		} else {
			imageView.setErrorImageResId(R.drawable.default_avatar);
		}
	}

    /**
     * 设置用户昵称
     */
    public static void setUserNick(String username,TextView textView){
    	EMUser user = getUserInfo(username);
    	if(user != null){
    		textView.setText(user.getNick());
    	}else{
    		textView.setText(username);
    	}
    }

    public static void 	setUserBeanNick(String username,TextView textView){
		Contact contact = getUserBeanInfo(username);
		if(contact != null){
			if (contact.getMUserNick() != null) {
				textView.setText(contact.getMUserNick());
			} else if (contact.getMContactCname() != null) {
				textView.setText(contact.getMContactCname());
			}
    	}else{
    		textView.setText(username);
    	}
    }

    /**
     * 设置当前用户昵称
     */
    public static void setCurrentUserNick(TextView textView){
    	EMUser user = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().getCurrentUserInfo();
    	if(textView != null){
    		textView.setText(user.getNick());
    	}
    }
    public static void setCurrentUserBeanNick(TextView textView){
		User user = SuperWeChatApplication.getInstance().getUser();
		if(textView != null){
    		textView.setText(user.getMUserNick());
    	}
    }

	/**
	 *
	 * @param newUser
     */
	public static void saveUserInfo(EMUser newUser) {
		if (newUser == null || newUser.getUsername() == null) {
			return;
		}
		((DemoHXSDKHelper) HXSDKHelper.getInstance()).saveContact(newUser);
	}

	public static void setUserHearder(String username, Contact user) {
		String headerName = null;
		if (!TextUtils.isEmpty(user.getMUserNick())) {
			headerName = user.getMUserNick();
		} else {
			headerName = user.getMContactCname();
		}
		if (username.equals(Constant.NEW_FRIENDS_USERNAME)
				||username.equals(Constant.GROUP_USERNAME)) {
			user.setHeader("");
		} else if (Character.isDigit(headerName.charAt(0))) {
			user.setHeader("#");
		} else {
			user.setHeader(HanziToPinyin.getInstance().get(headerName.substring(0, 1)).get(0).target.substring(0, 1)
					.toUpperCase());
			char header = user.getHeader().toLowerCase().charAt(0);
			if (header < 'a' || header > 'z') {
				user.setHeader("#");
			}
		}
	}

}
