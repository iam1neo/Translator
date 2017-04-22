package ru.iam1.translator;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends Activity {

    private Translator translator;

    private void log(Object o){
        System.out.println(o);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log(this.getLocalClassName() + ".onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //восстановим сохраненное состояние переводчика, либо создадим
        translator = (Translator) getLastNonConfigurationInstance();
        if(translator==null){
            String langs = getIntent().getStringExtra(LoadingActivity.EXTRA_LANGS);
            translator = new Translator(langs);
        }
        // инициализация вкладок
        final TabHost tabHost = (TabHost) findViewById(R.id.tab_host);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        // создаем главную вкладку переводчика
        tabSpec = tabHost.newTabSpec(Translator.TAG_TRANSLATE);
        tabSpec.setIndicator(getString(R.string.title_translate));
        tabSpec.setContent(R.id.tab_translate);
        tabHost.addTab(tabSpec);

        //вкладка закладок
        tabSpec = tabHost.newTabSpec(Translator.TAG_BOOKMARKS);
        tabSpec.setIndicator(getString(R.string.title_bookmarks));
        tabSpec.setContent(R.id.tab_bookmarks);
        tabHost.addTab(tabSpec);

        //вкладка настроек
        tabSpec = tabHost.newTabSpec(Translator.TAG_SETTINGS);
        tabSpec.setIndicator(getString(R.string.title_settings));
        tabSpec.setContent(R.id.tab_settings);
        tabHost.addTab(tabSpec);

        //устанавливаем активную вкладку
        tabHost.setCurrentTabByTag(translator.currentTabTag);

        // обработчик переключения вкладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabTag) {
                //запомним, какая вкладка теперь активная
                translator.currentTabTag = tabTag;
            }
        });

        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, translator.lang_names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.sort(new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareTo(rhs);
            }
        });

        //Инициализация выпадающего списка Языка текста
        final Spinner spn_lang_from = (Spinner) findViewById(R.id.spn_lang_from);
        spn_lang_from.setAdapter(adapter);
        spn_lang_from.setPrompt(getString(R.string.title_lang_from));
        // выделяем выбранный язык
        spn_lang_from.setSelection(translator.lang_from_index);
        // без этого кода значение списка не обновится
        spn_lang_from.post(new Runnable() {
            @Override
            public void run() {
                spn_lang_from.setSelection(translator.lang_from_index);
            }
        });
        // устанавливаем обработчик смены языка
        spn_lang_from.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.lang_to_index){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    translator.lang_to_index = translator.lang_from_index;
                    translator.lang_from_index = position;
                    Spinner spn_lang_to = (Spinner) findViewById(R.id.spn_lang_to);
                    spn_lang_to.setSelection(translator.lang_to_index);
                    log("change langs");
                }else{
                    translator.lang_from_index = position;
                }
                log("translator.lang_to_index = "+translator.lang_to_index);
                log("translator.lang_from_index = "+translator.lang_from_index);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Инициализация выпадающего списка Языка перевода
        final Spinner spn_lang_to = (Spinner) findViewById(R.id.spn_lang_to);
        spn_lang_to.setAdapter(adapter);
        spn_lang_to.setPrompt(getString(R.string.title_lang_to));
        // выделяем выбранный язык
        spn_lang_to.setSelection(translator.lang_to_index);
        // без этого кода значение списка не обновится
        spn_lang_to.post(new Runnable() {
            @Override
            public void run() {
                spn_lang_to.setSelection(translator.lang_to_index);
            }
        });
        // устанавливаем обработчик смены языка
        spn_lang_to.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.lang_from_index){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    log("change langs");
                    translator.lang_from_index = translator.lang_to_index;
                    translator.lang_to_index = position;
                    Spinner spn_lang_from = (Spinner) findViewById(R.id.spn_lang_from);
                    spn_lang_from.setSelection(translator.lang_from_index);
                }else{
                    translator.lang_to_index = position;
                }
                log("translator.lang_to_index = "+translator.lang_to_index);
                log("translator.lang_from_index = "+translator.lang_from_index);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //анимация смены вкладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            private int currentTab=tabHost.getCurrentTab();
            public void onTabChanged(String tabId)
            {
                View currentView = tabHost.getCurrentView();
                if (tabHost.getCurrentTab() > currentTab)
                {
                    currentView.setAnimation( inFromRightAnimation() );
                    log("animation from right");
                }
                else
                {
                    currentView.setAnimation( outToRightAnimation() );
                    log("animation to right");
                }
                currentTab = tabHost.getCurrentTab();
            }
        });
    }

    protected void onStart(){
        log(this.getLocalClassName() + ".onStart");
        super.onStart();
    }

    protected void onResume(){
        log(this.getLocalClassName()+".onResume");
        super.onResume();
    }

    protected void onPause(){
        log(this.getLocalClassName()+".onPause");
        super.onPause();
    }

    protected void onStop(){
        log(this.getLocalClassName()+".onStop");
        super.onStop();
    }

    protected void onDestroy(){
        log(this.getLocalClassName()+".onDestroy");
        super.onDestroy();
    }

    @Override
    final public Object onRetainNonConfigurationInstance() {
        return translator;
    }

    //обработка нажатий на кнопки
    public void click(View v) {
        switch (v.getId()){
            case R.id.btn_change_langs:
                //меняем первый язык на второй, второй сменится сам
                Spinner spn_lang_from = (Spinner) findViewById(R.id.spn_lang_from);
                spn_lang_from.setSelection(translator.lang_to_index);
                break;
        }
    }

    //попытка вернуться к предыдущему экрану = выход
    public void onBackPressed() {
        //super.onBackPressed();
        setResult(RESULT_OK, null);
        finish();
    }

    //анимация смены вкладок
    public Animation inFromRightAnimation()
    {
        Animation inFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(150);
        inFromRight.setInterpolator(new AccelerateInterpolator());
        return inFromRight;
    }

    public Animation outToRightAnimation()
    {
        Animation outtoLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoLeft.setDuration(150);
        outtoLeft.setInterpolator(new AccelerateInterpolator());
        return outtoLeft;
    }
}
