package ru.iam1.translator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private Translator translator;
    private Timer timer;//таймер для отложенного запроса перевода
    private TranslateAsyncTask task;
    private DetectAsyncTask taskDetect;
    private AlertDialog alertClearHistDialog;
    private AlertDialog alertClearFavDialog;
    private DBHelper dbHelper;//работа с БД для хранения истории и избранного

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //восстановим сохраненное состояние переводчика, либо создадим
        translator = (Translator) getLastNonConfigurationInstance();
        if(translator==null){
            String langs = getIntent().getStringExtra(LoadingActivity.EXTRA_LANGS);
            translator = new Translator(langs);
        }
        // создаем объект для создания и управления версиями БД
        dbHelper = new DBHelper(this);

        // инициализация вкладок
        final TabHost tabHost = (TabHost) findViewById(R.id.tab_host);
        tabHost.setup();
        TabHost.TabSpec tabSpec;
        // создаем главную вкладку переводчика
        tabSpec = tabHost.newTabSpec(Translator.TAG_TRANSLATE);
        tabSpec.setIndicator(getString(R.string.title_translate));
        tabSpec.setContent(R.id.tab_translate);
        tabHost.addTab(tabSpec);

        //вкладка истории
        tabSpec = tabHost.newTabSpec(Translator.TAG_HISTORY);
        tabSpec.setIndicator(getString(R.string.title_history));
        tabSpec.setContent(R.id.tab_history);
        tabHost.addTab(tabSpec);

        //вкладка избранного
        tabSpec = tabHost.newTabSpec(Translator.TAG_FAVORITES);
        tabSpec.setIndicator(getString(R.string.title_favorites));
        tabSpec.setContent(R.id.tab_favorites);
        tabHost.addTab(tabSpec);

        //устанавливаем активную вкладку
        tabHost.setCurrentTabByTag(translator.currentTabTag);

        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, translator.langNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Инициализация выпадающего списка Языка текста
        final Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
        spnLangFrom.setAdapter(adapter);
        spnLangFrom.setPrompt(getString(R.string.title_lang_from));
        // выделяем выбранный язык
        spnLangFrom.setSelection(translator.langFromIndex);
        // без этого кода значение списка не обновится
        spnLangFrom.post(new Runnable() {
            @Override
            public void run() {
                spnLangFrom.setSelection(translator.langFromIndex);
            }
        });
        // устанавливаем обработчик смены языка
        spnLangFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.langToIndex){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    translator.langToIndex = translator.langFromIndex;
                    translator.langFromIndex = position;
                    Spinner spn_lang_to = (Spinner) findViewById(R.id.spnLangTo);
                    spn_lang_to.setSelection(translator.langToIndex);
                }else{
                    translator.langFromIndex = position;
                    //обновляем перевод
                    if(translator.currentTabTag.equals(Translator.TAG_TRANSLATE)){
                        restartTranslation();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Инициализация выпадающего списка Языка перевода
        final Spinner spnLangTo = (Spinner) findViewById(R.id.spnLangTo);
        spnLangTo.setAdapter(adapter);
        spnLangTo.setPrompt(getString(R.string.title_lang_to));
        // выделяем выбранный язык
        spnLangTo.setSelection(translator.langToIndex);
        // без этого кода значение списка не обновится
        spnLangTo.post(new Runnable() {
            @Override
            public void run() {
                spnLangTo.setSelection(translator.langToIndex);
            }
        });
        // устанавливаем обработчик смены языка
        spnLangTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if(position == translator.langFromIndex){
                    //если выбрали тот же язык, видимо хотели их поменять местами
                    translator.langFromIndex = translator.langToIndex;
                    translator.langToIndex = position;
                    Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
                    spnLangFrom.setSelection(translator.langFromIndex);
                }else{
                    translator.langToIndex = position;
                    //обновляем перевод
                    if(translator.currentTabTag.equals(Translator.TAG_TRANSLATE)){
                        restartTranslation();
                    }
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
                //отключение возможно оставшейся включенной клавиатуры
                View focus = getCurrentFocus();
                if (focus != null) {
                    InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
                //инициализация перевода при активности вкладки перевода
                if(tabTag.equals(Translator.TAG_TRANSLATE) && !translator.isTranslated){
                    //если при возврате к экрану перевода текст все еще не был переведен, запросим
                    restartTranslation();
                }
                //инициализация истории
                if(tabTag.equals(Translator.TAG_HISTORY))
                    initTabHistory();
                //инициализация избраного
                if(tabTag.equals(Translator.TAG_FAVORITES))
                    initTabFavorites();
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
        txt_translate.setMovementMethod(new ScrollingMovementMethod());
        txt_translate.setText(translator.translate);

        Button btnAuto = (Button) findViewById(R.id.btnAuto);
        btnAuto.setEnabled(true);

        //инициализация истории
        initTabHistory();
        //инициализация избранного
        initTabFavorites();

        //инициализация ссылки
        TextView txt_licence = (TextView)findViewById(R.id.txt_licence);
        txt_licence.setMovementMethod(LinkMovementMethod.getInstance());
    }

    protected void onStart(){
        super.onStart();
    }

    protected void onResume(){
        super.onResume();
        if(!translator.isTranslated && translator.currentTabTag.equals(Translator.TAG_TRANSLATE)){
            //если при возврате к экрану перевода текст все еще не был переведен, запросим
            restartTranslation();
        }
    }

    protected void onPause(){
        super.onPause();
        //убиваем таймер и таск, если они есть
        destroyTimerTask();
        if(taskDetect!=null)
            taskDetect.cancel(true);
    }

    protected void onStop(){
        super.onStop();
    }

    protected void onDestroy(){
        super.onDestroy();
        if(alertClearHistDialog!=null && alertClearHistDialog.isShowing()){
            alertClearHistDialog.cancel();
        }
        if(alertClearFavDialog!=null && alertClearFavDialog.isShowing()){
            alertClearFavDialog.cancel();
        }
    }

    //попытка вернуться к предыдущему экрану = выход
    public void onBackPressed() {
        //super.onBackPressed();
        finish();
    }

    @Override
    final public Object onRetainNonConfigurationInstance() {
        return translator;
    }

    //обработка нажатий на кнопки
    public void click(View v) {
        switch (v.getId()){
            case R.id.btnChangeLangs:
                //меняем первый язык на второй, второй сменится сам
                Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
                spnLangFrom.setSelection(translator.langToIndex);
                break;
            case R.id.btnClearText:
                //очищаем текстовое поле
                EditText txt_text = (EditText)findViewById(R.id.txt_text);
                txt_text.setText("");
                break;
            case R.id.btnAuto:
                //отключим возможно запущенный таймер и таск перевода, в них сейчас необходимости возможно нет
                destroyTimerTask();
                //и кнопку запуска определения языка. дважды подряд запускать не надо
                Button btnAuto = (Button) findViewById(R.id.btnAuto);
                btnAuto.setEnabled(false);
                //запускаем асинхронный запрос определения языка
                if(taskDetect!=null)
                    taskDetect.cancel(true);
                taskDetect = new DetectAsyncTask(this);
                taskDetect.execute();
                break;
            case R.id.btnAddToFav:
                try {
                    dbHelper.addCurrentToFavorites();
                }catch(Exception e){}
                break;
            case R.id.btnClearHistory:
                //до удаления все же спросим юзера, вдруг случайно нажал
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //все же хочет очистить, ладно
                                try {
                                    dbHelper.clearHistory();
                                }catch(Exception e){}
                                //обновляем вкладку истории
                                initTabHistory();
                                break;
                        }
                    }
                };
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle(android.R.string.dialog_alert_title);
                dialogBuilder.setMessage(R.string.clear_history_warning);
                dialogBuilder.setPositiveButton(android.R.string.yes, dialogClickListener);
                dialogBuilder.setNegativeButton(android.R.string.no, dialogClickListener);
                alertClearHistDialog = dialogBuilder.create();
                alertClearHistDialog.show();
                break;
            case R.id.btnClearFavorites:
                //до удаления все же спросим юзера, вдруг случайно нажал
                DialogInterface.OnClickListener dialogClickListener2 = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //все же хочет очистить, ладно
                                try {
                                    dbHelper.clearFavorites();
                                }catch(Exception e){}
                                //обновляем вкладку истории
                                initTabFavorites();
                                break;
                        }
                    }
                };
                AlertDialog.Builder dialogBuilder2 = new AlertDialog.Builder(this);
                dialogBuilder2.setTitle(android.R.string.dialog_alert_title);
                dialogBuilder2.setMessage(R.string.clear_favorites_warning);
                dialogBuilder2.setPositiveButton(android.R.string.yes, dialogClickListener2);
                dialogBuilder2.setNegativeButton(android.R.string.no, dialogClickListener2);
                alertClearFavDialog = dialogBuilder2.create();
                alertClearFavDialog.show();
                break;
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
                translator.textToTranslate = s.toString();
                //перезапускаем процесс перевода
                if(translator.currentTabTag.equals(Translator.TAG_TRANSLATE)){
                    restartTranslation();
                }
            }
        });
    }
    private void restartTranslation(){
        //убиваем предыдущий таймер и таск, если они есть
        destroyTimerTask();
        //пока нет перевода, рано добавлять в избранное
        translator.isTranslated = false;
        Button btnAddToFav = (Button)findViewById(R.id.btnAddToFav);
        btnAddToFav.setEnabled(false);
        //если текст пуст, нельзя определить язык текста
        Button btnAuto = (Button) findViewById(R.id.btnAuto);
        //если нет текста, то переводить нечего (и добавлять в избранное тоже)
        if(translator.textToTranslate==null || !translator.textToTranslate.matches("(?is)^.*\\S.*$")){
            if(btnAuto.isEnabled())
                btnAuto.setEnabled(false);
            translator.isTranslated = true;
            translator.translate = translator.textToTranslate;
            TextView txtTranslate = (TextView)findViewById(R.id.txt_translate);
            txtTranslate.setText(translator.translate);
        }else{
            if(!btnAuto.isEnabled())
                btnAuto.setEnabled(true);
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
            translator.translate = translate;
            translator.isTranslated = true;
            TextView txtTranslate = (TextView)findViewById(R.id.txt_translate);
            txtTranslate.setText(translate);
            if(translate!=null && !translate.equals("")){
                //добавим запись в историю
                try {
                    dbHelper.insertHistory(
                            translator.textToTranslate,
                            translator.translate,
                            translator.getLangCodeFrom() + "-" + translator.getLangCodeTo());
                }catch (Exception e){}
                //теперь можно разрешить добавлять в избранное
                Button btnAddToFav = (Button)findViewById(R.id.btnAddToFav);
                btnAddToFav.setEnabled(true);
            }else{
                Toast.makeText(act, R.string.get_translate_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    //асинхронный запрос определения языка текста
    class DetectAsyncTask extends AsyncTask<Void, Void, Void> {
        MainActivity act;
        String langCode;
        public DetectAsyncTask(MainActivity a){
            act=a;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Void doInBackground(Void... params) {
            PhpClient php = new PhpClient(act);
            langCode = php.getDetectLangCode(translator.textToTranslate);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //возвращаем активность кнопке определения языка
            Button btnAuto = (Button) findViewById(R.id.btnAuto);
            btnAuto.setEnabled(true);
            if(langCode!=null && !langCode.equals("")) {
                //запоминаем язык, возможно инициируя и запрос перевода
                Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
                spnLangFrom.setSelection(translator.getIdByLangCode(langCode));
            }else{
                Toast.makeText(act, R.string.detect_lang_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    //инициализация вкладки истории
    private void initTabHistory(){
        ArrayList<Map<String, Object>> data = null;
        SimpleAdapter sAdapter = null;
        try {
            data = dbHelper.getAdapterDataHistory();
        }catch (Exception e){}
        //не было ошибок
        if(data!=null) {
            // массив имен атрибутов, из которых будут читаться данные
            String[] from = {
                    DBHelper.HIST_ID,
                    DBHelper.IS_FAVORITE_FLAG,
                    DBHelper.TEXT_TO_TRANSLATE,
                    DBHelper.TRANSLATE,
                    DBHelper.LANG_CODES
            };
            // массив ID View-компонентов, в которые будут вставлять данные
            int[] to = {
                    R.id.histId,
                    R.id.histToFavorites,
                    R.id.histText,
                    R.id.histTranslate,
                    R.id.histLangs
            };

            // создаем адаптер
            sAdapter = new SimpleAdapter(this, data, R.layout.history_element, from, to);
            // Указываем адаптеру свой биндер
            sAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data,
                                            String textRepresentation) {
                    switch (view.getId()) {
                        // CheckBox
                        case R.id.histToFavorites:
                            ((CheckBox) view).setChecked(data.toString().equals("1"));
                            ((CheckBox) view).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                                    LinearLayout histLayout = (LinearLayout)compoundButton.getParent();
                                    TextView histId = (TextView)histLayout.findViewById(R.id.histId);
                                    try {
                                        if (isChecked) {
                                            dbHelper.changeFavoriteFlag("" + histId.getText(), "1");
                                        } else {
                                            dbHelper.changeFavoriteFlag("" + histId.getText(), "0");
                                        }
                                    }catch(Exception e){e.toString();}
                                }
                            });
                            return true;
                    }
                    return false;
                }
            });
        }

        // определяем список и присваиваем ему адаптер
        ListView listHistory = (ListView) findViewById(R.id.listHistory);
        listHistory.setAdapter(sAdapter);
        listHistory.setItemsCanFocus(false);//иначе чекбокс блокирует события
        listHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //выбрали пункт истории. надо перейти на вкладку перевода с этой записью
                String textToTranslate = ""+((TextView)view.findViewById(R.id.histText)).getText();
                String translate = ""+((TextView)view.findViewById(R.id.histTranslate)).getText();
                String langs = ""+((TextView)view.findViewById(R.id.histLangs)).getText();
                translator.textToTranslate = textToTranslate;
                translator.translate = translate;
                translator.isTranslated = true;
                translator.langFromIndex = translator.getIdByLangCode(langs.substring(0,2));
                translator.langToIndex = translator.getIdByLangCode(langs.substring(3));
                translator.currentTabTag = Translator.TAG_TRANSLATE;
                TabHost tabHost = (TabHost) findViewById(R.id.tab_host);
                tabHost.setCurrentTabByTag(translator.currentTabTag);
                Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
                spnLangFrom.setSelection(translator.langFromIndex);
                Spinner spnLangTo = (Spinner) findViewById(R.id.spnLangTo);
                spnLangTo.setSelection(translator.langToIndex);
                EditText txt_text = (EditText)findViewById(R.id.txt_text);
                txt_text.setText(translator.textToTranslate);
                TextView txt_translate = (TextView)findViewById(R.id.txt_translate);
                txt_translate.setText(translator.translate);
            }
        });
    }

    //инициализация вкладки истории
    private void initTabFavorites(){
        ArrayList<Map<String, Object>> data = null;
        SimpleAdapter sAdapter = null;
        try {
            data = dbHelper.getAdapterDataFavorites();
        }catch (Exception e){}
        //не было ошибок
        if(data!=null) {
            // массив имен атрибутов, из которых будут читаться данные
            String[] from = {
                    DBHelper.HIST_ID,
                    DBHelper.IS_FAVORITE_FLAG,
                    DBHelper.TEXT_TO_TRANSLATE,
                    DBHelper.TRANSLATE,
                    DBHelper.LANG_CODES
            };
            // массив ID View-компонентов, в которые будут вставлять данные
            int[] to = {
                    R.id.histId,
                    R.id.histToFavorites,
                    R.id.histText,
                    R.id.histTranslate,
                    R.id.histLangs
            };

            // создаем адаптер
            sAdapter = new SimpleAdapter(this, data, R.layout.history_element, from, to);
            // Указываем адаптеру свой биндер
            sAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data,
                                            String textRepresentation) {
                    switch (view.getId()) {
                        // CheckBox
                        case R.id.histToFavorites:
                            ((CheckBox) view).setChecked(data.toString().equals("1"));
                            ((CheckBox) view).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                                    LinearLayout histLayout = (LinearLayout)compoundButton.getParent();
                                    TextView histId = (TextView)histLayout.findViewById(R.id.histId);
                                    try {
                                        if (isChecked) {
                                            dbHelper.changeFavoriteFlag("" + histId.getText(), "1");
                                        } else {
                                            dbHelper.changeFavoriteFlag("" + histId.getText(), "0");
                                        }
                                    }catch(Exception e){e.toString();}
                                }
                            });
                            return true;
                    }
                    return false;
                }
            });
        }

        // определяем список и присваиваем ему адаптер
        ListView listFavorites = (ListView) findViewById(R.id.listFavorites);
        listFavorites.setAdapter(sAdapter);
        listFavorites.setItemsCanFocus(false);//иначе чекбокс блокирует события
        listFavorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //выбрали пункт избранного. надо перейти на вкладку перевода с этой записью
                String textToTranslate = ""+((TextView)view.findViewById(R.id.histText)).getText();
                String translate = ""+((TextView)view.findViewById(R.id.histTranslate)).getText();
                String langs = ""+((TextView)view.findViewById(R.id.histLangs)).getText();
                translator.textToTranslate = textToTranslate;
                translator.translate = translate;
                translator.isTranslated = true;
                translator.langFromIndex = translator.getIdByLangCode(langs.substring(0,2));
                translator.langToIndex = translator.getIdByLangCode(langs.substring(3));
                translator.currentTabTag = Translator.TAG_TRANSLATE;
                TabHost tabHost = (TabHost) findViewById(R.id.tab_host);
                tabHost.setCurrentTabByTag(translator.currentTabTag);
                Spinner spnLangFrom = (Spinner) findViewById(R.id.spnLangFrom);
                spnLangFrom.setSelection(translator.langFromIndex);
                Spinner spnLangTo = (Spinner) findViewById(R.id.spnLangTo);
                spnLangTo.setSelection(translator.langToIndex);
                EditText txt_text = (EditText)findViewById(R.id.txt_text);
                txt_text.setText(translator.textToTranslate);
                TextView txt_translate = (TextView)findViewById(R.id.txt_translate);
                txt_translate.setText(translator.translate);
            }
        });
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
