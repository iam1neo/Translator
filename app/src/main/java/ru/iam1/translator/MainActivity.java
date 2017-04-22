package ru.iam1.translator;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private Translator translator;
    private Timer timer;//таймер для отложенного запроса перевода
    private TranslateAsyncTask task;

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

        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, translator.langNames);
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
        spn_lang_from.setSelection(translator.langFromIndex);
        // без этого кода значение списка не обновится
        spn_lang_from.post(new Runnable() {
            @Override
            public void run() {
                spn_lang_from.setSelection(translator.langFromIndex);
            }
        });
        // устанавливаем обработчик смены языка
        spn_lang_from.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.langToIndex){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    translator.langToIndex = translator.langFromIndex;
                    translator.langFromIndex = position;
                    Spinner spn_lang_to = (Spinner) findViewById(R.id.spn_lang_to);
                    spn_lang_to.setSelection(translator.langToIndex);
                }else{
                    translator.langFromIndex = position;
                    //обновляем перевод
                    restartTranslation();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Инициализация выпадающего списка Языка перевода
        final Spinner spn_lang_to = (Spinner) findViewById(R.id.spn_lang_to);
        spn_lang_to.setAdapter(adapter);
        spn_lang_to.setPrompt(getString(R.string.title_lang_to));
        // выделяем выбранный язык
        spn_lang_to.setSelection(translator.langToIndex);
        // без этого кода значение списка не обновится
        spn_lang_to.post(new Runnable() {
            @Override
            public void run() {
                spn_lang_to.setSelection(translator.langToIndex);
            }
        });
        // устанавливаем обработчик смены языка
        spn_lang_to.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.langFromIndex){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    translator.langFromIndex = translator.langToIndex;
                    translator.langToIndex = position;
                    Spinner spn_lang_from = (Spinner) findViewById(R.id.spn_lang_from);
                    spn_lang_from.setSelection(translator.langFromIndex);
                }else{
                    translator.langToIndex = position;
                    //обновляем перевод
                    restartTranslation();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //анимация смены вкладок
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            private int currentTabId=tabHost.getCurrentTab();
            public void onTabChanged(String tabTag)
            {
                View currentView = tabHost.getCurrentView();
                if (tabHost.getCurrentTab() > currentTabId){
                    currentView.setAnimation( inFromRightAnimation() );
                }else{
                    currentView.setAnimation( outToRightAnimation() );
                }
                currentTabId = tabHost.getCurrentTab();
                //запомним, какая вкладка теперь активная
                translator.currentTabTag = tabTag;
            }
        });

        //инициализация поля ввода переводимого текста
        EditText txt_text = (EditText)findViewById(R.id.txt_text);
        //txt_text.setHorizontallyScrolling(false);
        //добавим обработчик изменений в тексте, запуск таймера и асинхронного запроса к api
        initAutotranslation(txt_text);
        //меняем текст, запрос перевода запустится сам
        txt_text.setText(translator.textToTranslate);

        //инициализация текста перевода
        TextView txt_translate = (TextView)findViewById(R.id.txt_translate);
        txt_translate.setText(translator.translate);
    }

    protected void onStart(){
        log(this.getLocalClassName() + ".onStart");
        super.onStart();
    }

    protected void onResume(){
        log(this.getLocalClassName()+".onResume");
        super.onResume();
        if(!translator.isTranslated){
            //если при возврате к экрану перевода еще не было, запросим
            restartTranslation();
        }
    }

    protected void onPause(){
        log(this.getLocalClassName()+".onPause");
        super.onPause();
        //убиваем таймер и таск, если они есть
        destroyTimerTask();
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
                spn_lang_from.setSelection(translator.langToIndex);
                break;
            case R.id.btn_clear:
                EditText txt_text = (EditText)findViewById(R.id.txt_text);
                txt_text.setText("");
        }
    }


    //обработчик изменений в тексте, запуск таймера и асинхронного запроса к api
    private void initAutotranslation(EditText txt_text){
        txt_text.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
            public void afterTextChanged(Editable s) {
                log("afterTextChanged");
                log("s="+s);
                translator.textToTranslate = s.toString();
                //перезапускаем процесс перевода
                restartTranslation();
            }
        });
    }
    private void restartTranslation(){
        //убиваем предыдущий таймер и таск, если они есть
        destroyTimerTask();
        //пока нет перевода, рано добавлять в избранное
        translator.isTranslated = false;
        Button btnAddToFav = (Button)findViewById(R.id.btn_add_to_fav);
        btnAddToFav.setEnabled(false);
        //если нет текста, то переводить нечего (и добавлять в избранное тоже)
        if(translator.textToTranslate==null || !translator.textToTranslate.matches(".*\\S.*")){
            translator.isTranslated = true;
            translator.translate = translator.textToTranslate;
            TextView txtTranslate = (TextView)findViewById(R.id.txt_translate);
            txtTranslate.setText(translator.translate);
        }else{
            timer = new Timer();
            TimerTask mMyTimerTask = new MyTimerTask();
            timer.schedule(mMyTimerTask, getResources().getInteger(R.integer.timer_to_translate_ms));
        }
    }
    //отключение таймера и асинхронного запроса
    private void destroyTimerTask(){
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if(task != null){
            task.cancel(true);
            task = null;
        }
    }
    //после успешного завершения таймера запускаем асинхронный запрос перевода
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createTask();
                }
            });
        }
    }
    public void createTask(){
        destroyTimerTask();
        log("timer calls async task");
        task = new TranslateAsyncTask(this);
        task.execute();
    }
    class TranslateAsyncTask extends AsyncTask<Void, Void, Void> {
        MainActivity act;
        String translate;

        public TranslateAsyncTask(MainActivity a){
            act=a;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            PhpClient php = new PhpClient(act);
            translate = php.getTranslate(
                        translator.textToTranslate,
                        translator.getLangCodeFrom(),
                        translator.getLangCodeTo()
            );
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            log("AsyncTasks End! translate:");
            log(translate);
            translator.translate = translate;
            translator.isTranslated = true;
            TextView txtTranslate = (TextView)findViewById(R.id.txt_translate);
            txtTranslate.setText(translate);
            if(translate!=null){
                //теперь можно добавлять в избранное
                Button btnAddToFav = (Button)findViewById(R.id.btn_add_to_fav);
                btnAddToFav.setEnabled(true);
            }
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
