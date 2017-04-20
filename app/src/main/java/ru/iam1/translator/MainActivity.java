package ru.iam1.translator;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Translator translator;
    private TextView mTextMessage;

    private void log(Object o){
        System.out.println(o);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_translate:
                    translator.currentTab = item.getItemId();
                    mTextMessage.setText(R.string.title_translate);
                    return true;
                case R.id.navigation_bookmarks:
                    translator.currentTab = item.getItemId();
                    mTextMessage.setText(R.string.title_bookmarks);
                    return true;
                case R.id.navigation_settings:
                    translator.currentTab = item.getItemId();
                    mTextMessage.setText(R.string.title_settings);
                    return true;
            }
            return false;
        }

    };

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

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(translator.currentTab);
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

    public void onBackPressed() {
        //super.onBackPressed();
        setResult(RESULT_OK, null);
        finish();
    }

}
