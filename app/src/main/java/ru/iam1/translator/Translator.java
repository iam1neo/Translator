package ru.iam1.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by iam1 on 20.04.2017.
 */

public class Translator {
    public static String TAG_TRANSLATE = "translate";
    public static String TAG_BOOKMARKS = "bookmarks";
    public static String TAG_SETTINGS = "settings";

    public String[] lang_codes;//коды языков
    public String[] lang_names;//расшифровки языков

    public String currentTabTag;//активная вкладка
    public int lang_from_index=-1;//индекс языка текста
    public int lang_to_index=-1;//индекс языка перевода

    public Translator(String langs){
        //открывающаяся по умолчанию вкладка
        currentTabTag = TAG_TRANSLATE;

        //вспомогательные классы для сортировки языков по алфавиту
        class Pair{
            public String code;
            public String name;
            public Pair(String c, String n){
                code=c;
                name=n;
            }
        }
        class PairComparator implements Comparator<Pair> {
            @Override
            public int compare(Pair pair1, Pair pair2) {
                return pair1.name.compareTo(pair2.name);
            }
        }
        //разбиваем строку с языками и сортируем
        String[] l1,l2;
        l1 = langs.split(";");
        int i;
        ArrayList<Pair> langPairs = new ArrayList<>(l1.length);
        for (i = 0; i < l1.length; i++){
            l2 = l1[i].split("=");
            langPairs.add(new Pair(l2[0],l2[1]));
        }
        Collections.sort(langPairs,new PairComparator());

        //заполним массивы кодов и расшифровок поддерживаемых языков
        lang_codes = new String[l1.length];
        lang_names = new String[l1.length];
        for(i=0; i<l1.length; i++){
            lang_codes[i] = langPairs.get(i).code;
            lang_names[i] = langPairs.get(i).name;
            if(lang_codes[i].equals("ru"))
                lang_from_index = i;
            if(lang_codes[i].equals("en"))
                lang_to_index = i;
        }
    }
}
