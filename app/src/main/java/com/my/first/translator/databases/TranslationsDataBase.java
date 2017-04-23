package com.my.first.translator.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.my.first.translator.classes.Translation;

import java.util.ArrayList;

public class TranslationsDataBase extends SQLiteOpenHelper implements BaseColumns {

    private static TranslationsDataBase mInstance;
    private static SQLiteDatabase myWritableDb;
    private static final String DATABASE_NAME = "translations_database.db";

    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "translations_table";

    private static final String TRANSLATION_TEXT = "text";
    private static final String TRANSLATION_SIMPLE_TRANSLATION = "simple_translation";
    private static final String TRANSLATION_FULL_TRANSLATION = "full_translation";
    private static final String TRANSLATION_LANG = "lang";
    private static final String TRANSLATION_IS_FAVORITE = "is_favorite";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
            + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TRANSLATION_TEXT + " TEXT, "
            + TRANSLATION_SIMPLE_TRANSLATION + " TEXT, "
            + TRANSLATION_FULL_TRANSLATION + " TEXT, "
            + TRANSLATION_LANG + " TEXT, "
            + TRANSLATION_IS_FAVORITE + " INTEGER);";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    private TranslationsDataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        onCreate(sqLiteDatabase);
    }

    @Override
    public void close() {
        super.close();
        if (myWritableDb != null) {
            myWritableDb.close();
            myWritableDb = null;
        }
    }

    private SQLiteDatabase getMyWritableDatabase() {
        if ((myWritableDb == null) || (!myWritableDb.isOpen())) {
            myWritableDb = this.getWritableDatabase();
        }
        return myWritableDb;
    }

    private static TranslationsDataBase getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TranslationsDataBase(context);
        }
        return mInstance;
    }

    public static void addTranslationToDataBase(Context context, Translation translation) {
        SQLiteDatabase sqdb = TranslationsDataBase.getInstance(context).getMyWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TRANSLATION_TEXT, translation.getText());
        cv.put(TRANSLATION_SIMPLE_TRANSLATION, translation.getSimpleTranslation());
        cv.put(TRANSLATION_FULL_TRANSLATION, translation.getFullTranslation());
        cv.put(TRANSLATION_LANG, translation.getLang());
        cv.put(TRANSLATION_IS_FAVORITE, translation.isFavorite() ? 1 : 0);
        sqdb.insert(TABLE_NAME, null, cv);
    }

    public static void changeFavorite(Context context, String text, String lang, boolean isFavorite) {
        SQLiteDatabase sqdb = TranslationsDataBase.getInstance(context).getMyWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TRANSLATION_IS_FAVORITE, isFavorite ? 1 : 0);
        sqdb.update(TABLE_NAME, cv, TRANSLATION_TEXT + " =? AND " + TRANSLATION_LANG + " =?",
                new String[]{text, lang});
    }

    public static void resetFavorites(Context context) {
        SQLiteDatabase sqdb = TranslationsDataBase.getInstance(context).getMyWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TRANSLATION_IS_FAVORITE, 0);
        sqdb.update(TABLE_NAME, cv, TRANSLATION_IS_FAVORITE + " =?", new String[]{"1"});
    }

    public static void deleteTranslation(Context context, String text, String lang) {
        SQLiteDatabase sqdb = TranslationsDataBase.getInstance(context).getMyWritableDatabase();
        sqdb.delete(TABLE_NAME, TRANSLATION_TEXT + " =? AND " + TRANSLATION_LANG + " =?",
                new String[]{text, lang});
    }

    public static void deleteAll(Context context) {
        SQLiteDatabase sqdb = TranslationsDataBase.getInstance(context).getMyWritableDatabase();
        sqdb.delete(TABLE_NAME, null, null);
    }

    public static ArrayList<Translation> getTranslationsFromDataBase(Context context) {
        SQLiteDatabase sqdb = getInstance(context).getMyWritableDatabase();
        Cursor cursor = sqdb.query(TABLE_NAME, null, null, null, null, null, _ID + " DESC", null);
        ArrayList<Translation> translations = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Translation translation = new Translation();
                translation.setText(cursor.getString(cursor.getColumnIndex(TRANSLATION_TEXT)));
                translation.setSimpleTranslation(cursor.getString(cursor
                        .getColumnIndex(TRANSLATION_SIMPLE_TRANSLATION)));
                translation.setFullTranslation(cursor.getString(cursor
                        .getColumnIndex(TRANSLATION_FULL_TRANSLATION)));
                translation.setLang(cursor.getString(cursor.getColumnIndex(TRANSLATION_LANG)));
                translation.setFavorite(cursor.getInt(cursor
                        .getColumnIndex(TRANSLATION_IS_FAVORITE)) == 1);
                translations.add(translation);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return translations;
    }
}
