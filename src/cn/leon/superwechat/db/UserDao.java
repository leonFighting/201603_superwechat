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
 */
public class UserDao extends SQLiteOpenHelper {

    public UserDao(Context context) {
        super(context, "user.db", null, 1);
    }






    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "DROP TABLE IF EXISTS "+ I.User.TABLE_NAME+" " +
                "CREATE TABLE "+I.User.TABLE_NAME +
                I.User.USER_ID+" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                I.User.USER_NAME+" TEXT NOT NULL," +
                I.User.PASSWORD+" TEXT NOT NULL," +
                I.User.NICK+" TEXT NOT NULL," +
                I.User.UN_READ_MSG_COUNT+" INTEGER DEFAULT 0" +");";
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
        db.close();
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
        db.close();
        return insert > 0;
    }

    public User findUserByName(String userName) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select* from " + I.User.TABLE_NAME + " where " + I.User.USER_NAME + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{userName});
        if (cursor.moveToNext()) {
            int uid = cursor.getInt(cursor.getColumnIndex(I.User.USER_ID));
            String nick = cursor.getString(cursor.getColumnIndex(I.User.NICK));
            String password = cursor.getString(cursor.getColumnIndex(I.User.PASSWORD));
            int unReadMsgCount = cursor.getInt(cursor.getColumnIndex(I.User.UN_READ_MSG_COUNT));
            return new User(uid, userName, password, nick, unReadMsgCount);
        }
        cursor.close();
        db.close();
        return null;
    }
}
