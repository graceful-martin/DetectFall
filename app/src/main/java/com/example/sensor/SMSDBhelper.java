package com.example.sensor;

import android.database.*;
import android.database.sqlite.*;
import android.content.*;
//...중략

//SMSDBhelper 클래스
public class SMSDBhelper {
    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;

    private static final String DATABASE_NAME = "smslists.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "ContactList";
    public static final String COLUMN_CONTACT  = "contact";
    public static final String _ID = "id";
    private final Context mCtx;

    private static final String DATABASE_CREATE =
            "CREATE TABLE " +
                    TABLE_NAME + "("+
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT" +","+
                    COLUMN_CONTACT + " TEXT NOT NULL" + ");";

    public SMSDBhelper(Context context){
        this.mCtx =context;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }
    }

    public SMSDBhelper open() throws SQLException {
        mDbHelper = new SMSDBhelper.DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    public void close() {
        mDbHelper.close();
    }
    public void addNewContact(String contact){
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CONTACT,contact);
        mDb.insert(TABLE_NAME,null,cv);
    }
    //SQLite는 기존의 RDBMS에서 사용하는 raw query와 자체적인 메서드를 이용한 쿼리 두 가지를 모두 지원한다.
    //아래 코드는 WHERE 조건절이 있는 delete 액션 코드와 저장된 전체 정보를 가져오는 select 쿼리 코드이다.
    public void removeContact(String contact){
        mDb.delete(TABLE_NAME, "contact"+"=?",new String[]{contact});
    }
    public void removeAllContact(){
        mDb.delete(TABLE_NAME, null, null);
    }
    public Cursor getAllContacts(){
        return mDb.query(TABLE_NAME,null,null,null,null,null,COLUMN_CONTACT);
    }
}