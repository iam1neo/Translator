package ru.iam1.translator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;


public class LoadingActivity extends Activity {
    public static String EXTRA_LANGS = "langs";
    Timer mTimer;
    AsyncTasks tasks;
    String langs;
    boolean isRunning=false;//активна ли активити
    boolean timer_finished=false;//прошло ли ожидание таймера
    boolean tasks_finished=false;//выполнены ли все необходимые процессы
    boolean dialog_finished=false;//завершен ли диалог

    private static int REQUEST_EXIT = 1;//код для главного меню и закрытия при возврате

    private static int DIALOG_NO_CONNECTION = 2;//код открытия предупреждения отсутствия соединения

    private void log(Object o){
        System.out.println(o);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log(this.getLocalClassName()+".onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        timer_finished = false;
        tasks_finished = false;
        dialog_finished = false;

        tasks = new AsyncTasks(this);
        tasks.execute();
    }
    //override onStart

    @Override
    protected void onResume(){
        log(this.getLocalClassName() + ".onResume");
        isRunning=true;
        super.onResume();
        if(!timer_finished) {
            destroyTimer();
            mTimer = new Timer();
            TimerTask mMyTimerTask = new MyTimerTask();
            mTimer.schedule(mMyTimerTask, getResources().getInteger(R.integer.start_logo_ms));
        }
        if(timer_finished && tasks_finished && dialog_finished){
            startMainActivity();
        }
    }

    protected void onPause(){
        log(this.getLocalClassName()+".onPause");
        isRunning=false;
        super.onPause();
        destroyTimer();
    }

    protected void onStop(){
        log(this.getLocalClassName()+".onStop");
        super.onStop();
    }

    protected void onDestroy(){
        log(this.getLocalClassName() + ".onDestroy");
        super.onDestroy();
    }


    private void destroyTimer(){
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            //final можно передать внутрь run
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timer_finished=true;
                    destroyTimer();
                    if(tasks_finished && dialog_finished)
                        startMainActivity();
                }
            });
        }
    }
    class AsyncTasks extends AsyncTask<Void, Void, Void> {
        LoadingActivity act;
        String getLangs;

        public AsyncTasks(LoadingActivity a){
            act=a;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            log("AsyncTasks Begin");
        }

        @Override
        protected Void doInBackground(Void... params) {
            PhpClient php = new PhpClient(act);
            getLangs = php.getLangs();
            //еще попытка на всякий случай
            if(getLangs==null)
                getLangs = php.getLangs();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            log("AsyncTasks End");
            tasks_finished = true;
            langs=getLangs;
            if(langs==null)
                openDialog(DIALOG_NO_CONNECTION);
            else
                dialog_finished = true;//диалогов и не будет

            if(act.isRunning && dialog_finished && timer_finished)
                startMainActivity();
        }
    }

    private void openDialog(int dialog_type) {
        log("openDialog");
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_NEUTRAL:
                        finish();
                }
                dialog_finished = true;
                if(tasks_finished && timer_finished)
                    startMainActivity();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error);
        if(dialog_type==DIALOG_NO_CONNECTION){
            builder.setMessage(R.string.get_langs_error);
            builder.setNeutralButton(R.string.ok, dialogClickListener);
        }
        builder.setCancelable(false);//чтоб не закрывался по кнопке Назад
        builder.show();
    }

    void startMainActivity(){
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra(EXTRA_LANGS,langs);
        startActivityForResult(intent,REQUEST_EXIT);
    }

    //при возврате из главного меню закрываем приложение
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }
}
