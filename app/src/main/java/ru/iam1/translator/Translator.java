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

    public String[] langCodes;//коды языков
    public String[] langNames;//расшифровки языков

    public String currentTabTag;//активная вкладка
    public int langFromIndex=-1;//индекс языка текста
    public int langToIndex=-1;//индекс языка перевода

    public String textToTranslate;//текст для перевода
    public String translate;//последний полученный перевод
    public boolean isTranslated=true;//был ли переведен текст

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
        langCodes = new String[l1.length];
        langNames = new String[l1.length];
        for(i=0; i<l1.length; i++){
            langCodes[i] = langPairs.get(i).code;
            langNames[i] = langPairs.get(i).name;
            if(langCodes[i].equals("ru"))
                langFromIndex = i;
            if(langCodes[i].equals("en"))
                langToIndex = i;
        }
    }

    //получение кода языков "из" и "в"
    public String getLangCodeFrom(){
        return langCodes[langFromIndex];
    }
    public String getLangCodeTo(){
        return langCodes[langToIndex];
    }

    //поиск индекса в массиве по коду языка
    public int getIdByLangCode(String langCode){
        for(int i=0;i<langCodes.length;i++){
            if(langCodes[i].equals(langCode))
                return i;
        }
        return -1;
    }
}
