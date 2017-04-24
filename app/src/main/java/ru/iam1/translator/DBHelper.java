package ru.iam1.translator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class DBHelper extends SQLiteOpenHelper {

    final public static String HIST_ID = "hist_id";
    final public static String ORDER_ID = "order_id";
    final public static String IS_FAVORITE_FLAG = "is_favorite_flag";
    final public static String TEXT_TO_TRANSLATE = "text_to_translate";
    final public static String TRANSLATE = "translate";
    final public static String LANG_CODES = "lang_codes";

    public DBHelper(Context context) {
        // конструктор суперкласса
        super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table history ("
                + "hist_id integer primary key autoincrement,"
                + "order_id integer,"
                + "is_favorite_flag integer,"
                + "text_to_translate text,"
                + "translate text,"
                + "lang_codes text"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //добавить запись в историю
    public void insertHistory(String textToTranslate, String translate, String langCodes){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        // новая запись в истории должна стать первой, ей нужен максимальный order, найдем его (если записей нет, то 1)
        String order = ifnull(getFirstValue(db, "select ifnull(max(order_id),0)+1 from history", null), "1");
        //возможно раньше был подобный запрос, не за чем писать его снова, лучше обновить и передвинуть старый
        String histId = getFirstValue(db
                ,"select max(hist_id) from history where text_to_translate = ? and lang_codes = ?"
                , new String[]{textToTranslate,langCodes});
        if(histId!=null){
            //нашли. обновим старую запись
            db.execSQL("UPDATE history SET order_id = ?, translate = ? WHERE hist_id = ?", new String[]{order,translate,histId});
        }else {
            cv.put(ORDER_ID, order);
            cv.put(IS_FAVORITE_FLAG, 0);
            cv.put(TEXT_TO_TRANSLATE, textToTranslate);
            cv.put(TRANSLATE, translate);
            cv.put(LANG_CODES, langCodes);
            // вставляем запись и получаем ее ID
            long rowID = db.insert("history", null, cv);
        }
        //подчистим устаревшую историю
        clearTooOldHistory();
        close();
    }
    public ArrayList<Map<String, Object>> getAdapterDataHistory(){
        ArrayList<Map<String, Object>> data = null;
        // подключаемся к БД
        SQLiteDatabase db = getWritableDatabase();
        // делаем запрос всех данных из таблицы
        Cursor c = db.rawQuery("select * from history order by order_id desc",null);
        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {
            data = new ArrayList<>(c.getCount());
            Map<String, Object> m;
            do {
                m = new HashMap<>();
                m.put(HIST_ID, c.getString(c.getColumnIndex(HIST_ID)));
                m.put(IS_FAVORITE_FLAG, c.getString(c.getColumnIndex(IS_FAVORITE_FLAG)));
                m.put(TEXT_TO_TRANSLATE, c.getString(c.getColumnIndex(TEXT_TO_TRANSLATE)).replaceAll("[\\t\\n\\r]+"," "));
                m.put(TRANSLATE, c.getString(c.getColumnIndex(TRANSLATE)).replaceAll("[\\t\\n\\r]+"," "));
                m.put(LANG_CODES, c.getString(c.getColumnIndex(LANG_CODES)));
                data.add(m);
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        }
        c.close();
        close();
        // упаковываем данные в понятную для адаптера структуру
        return data;
    }

    //очистить всю историю (но не избранное)
    public void clearHistory(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from history where is_favorite_flag=0");
        close();
    }
    public void clearTooOldHistory(){
        SQLiteDatabase db = getWritableDatabase();
        String qwery = "delete from history " +
                "where is_favorite_flag=0 " +
                "and order_id<=( " +
                "  select max(order_id) " +
                "  from ( " +
                "    select " +
                "      order_id, " +
                "      (select count(*) from history where order_id>=t.order_id) row_num " +
                "    from history t " +
                "    where is_favorite_flag=0 " +
                "  ) " +
                "  where row_num > 100 " +
                ")";
        db.execSQL(qwery);
        close();
    }
    //добавить(удалить) только что переведенную запись в избранное
    public void addCurrentToFavorites(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE history SET is_favorite_flag = 1 WHERE order_id = (select max(order_id) from history)");
        close();
    }
    //добавить(удалить) запись в избранное
    public void changeFavoriteFlag(String histId,String newFlag){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE history SET is_favorite_flag = ? WHERE hist_id = ?", new String[]{newFlag,histId});
        close();
    }
    //получить список избранного
    public ArrayList<Map<String, Object>> getAdapterDataFavorites(){
        ArrayList<Map<String, Object>> data = null;
        // подключаемся к БД
        SQLiteDatabase db = getWritableDatabase();
        // делаем запрос всех данных из таблицы
        Cursor c = db.rawQuery("select * from history where is_favorite_flag=1 order by order_id desc",null);
        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {
            data = new ArrayList<>(c.getCount());
            Map<String, Object> m;
            do {
                m = new HashMap<>();
                m.put(HIST_ID, c.getString(c.getColumnIndex(HIST_ID)));
                m.put(IS_FAVORITE_FLAG, c.getString(c.getColumnIndex(IS_FAVORITE_FLAG)));
                m.put(TEXT_TO_TRANSLATE, c.getString(c.getColumnIndex(TEXT_TO_TRANSLATE)).replaceAll("[\\t\\n\\r]+"," "));
                m.put(TRANSLATE, c.getString(c.getColumnIndex(TRANSLATE)).replaceAll("[\\t\\n\\r]+"," "));
                m.put(LANG_CODES, c.getString(c.getColumnIndex(LANG_CODES)));
                data.add(m);
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        }
        c.close();
        close();
        // упаковываем данные в понятную для адаптера структуру
        return data;
    }
    //очистить все избранное (но не историю)
    public void clearFavorites(){
        SQLiteDatabase db = getWritableDatabase();
        String qwery = "delete from history where is_favorite_flag=1";
        db.execSQL(qwery);
        close();
    }

    //получить одиночное значение
    private String getFirstValue(SQLiteDatabase db, String select, String[] params){
        String selection=null;
        try {
            Cursor c = db.rawQuery(select, params);
            if (c.getCount() >= 1) {
                c.moveToFirst();
                selection = c.getString(0);
            }
            c.close();
        }catch (Exception e){}
        return selection;
    }
    private String ifnull(String a, String b){
        if(a==null || a.equals(""))
            return b;
        return a;
    }
}