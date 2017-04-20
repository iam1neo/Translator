package ru.iam1.translator;

import java.util.HashMap;

/**
 * Created by iam1 on 20.04.2017.
 */

public class Translator {
    public int currentTab;//активная вкладка
    public HashMap<String,String> langs;//коды языков и расшифровки

    public Translator(String langs){
        currentTab = R.id.navigation_translate;
        //разберем строку с языками
        this.langs = new HashMap<>();
        String[] l1,l2;
        l1 = langs.split(";");
        for (int i = 0; i < l1.length; i++){
            l2 = l1[i].split("=");
            this.langs.put(l2[0],l2[1]);
        }
    }

}
