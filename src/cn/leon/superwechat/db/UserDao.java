package cn.leon.superwechat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.leon.superwechat.I;
import cn.leon.superwechat.bean.User;

/**
 * Created by leon on 2016/5/19.
 * 将登录成功的用户保存到本地应用的数据库里
 * 本地apk数据库,远端数据库,环信数据库
 */
public class UserDao extends SQLiteOpenHelper {
    public static final String Id = "_id";
    public static final String TABLE_NAME = "user";

    public UserDao(Context context) {
        super(context, "user.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_NAME + "( " +
                I.User.USER_ID + " integer primary key autoincrement, " +
                I.User.USER_NAME + " varchar unique not null, " +
                I.User.NICK + " varchar, " +
                I.User.PASSWORD + " varchar, " +
                I.User.UN_READ_MSG_COUNT + " int default(0) " +
                ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean addUser(User user) {
        ContentValues values = new ContentValues();
        values.put(I.User.USER_ID, user.getMUserId());
        values.put(I.User.USER_NAME, user.getMUserName());
        values.put(I.User.NICK, user.getMUserNick());
        values.put(I.User.PASSWORD, user.getMUserPassword());
        values.put(I.User.UN_READ_MSG_COUNT, user.getMUserUnreadMsgCount());
        SQLiteDatabase db = getWritableDatabase();
        long insert = db.insert(I.User.TABLE_NAME, null, values);
        return insert > 0;
    }

    public boolean updateUser(User user) {
        ContentValues values = new ContentValues();
        values.put(I.User.USER_ID, user.getMUserId());
        values.put(I.User.NICK, user.getMUserNick());
        values.put(I.User.PASSWORD, user.getMUserPassword());
        values.put(I.User.UN_READ_MSG_COUNT, user.getMUserUnreadMsgCount());
        SQLiteDatabase db = getWritableDatabase();
        long insert = db.update(I.User.TABLE_NAME, values, "where" + I.User.USER_NAME + "=?",
                new String[]{user.getMUserName()});
        return insert > 0;
    }

    public User findUserByName(String userName) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select * from " + TABLE_NAME + " where " + I.User.USER_NAME + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{userName});
        if (cursor.moveToNext()) {
            int uid = cursor.getInt(cursor.getColumnIndex(I.User.USER_ID));
            String nick = cursor.getString(cursor.getColumnIndex(I.User.NICK));
            String password = cursor.getString(cursor.getColumnIndex(I.User.PASSWORD));
            int unReadMsgCount = cursor.getInt(cursor.getColumnIndex(I.User.UN_READ_MSG_COUNT));
            return new User(uid, userName, password, nick, unReadMsgCount);
        }
        cursor.close();
        return null;
    }
}
