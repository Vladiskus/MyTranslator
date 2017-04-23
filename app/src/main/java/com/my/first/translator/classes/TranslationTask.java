package com.my.first.translator.classes;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranslationTask extends AsyncTask<String, Void, String> {

    private HttpURLConnection urlConnection = null;
    private BufferedReader reader = null;
    private String resultJson = "";
    private final TaskListener taskListener;

    public interface TaskListener {
        void onFinished(String result);

    }

    public TranslationTask(TaskListener taskListener) {
        this.taskListener = taskListener;
    }

    @Override
    protected String doInBackground(String... specs) {
        try {
            urlConnection = (HttpURLConnection) new URL(specs[0]).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            resultJson = buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultJson;
    }

    @Override
    protected void onPostExecute(String strJson) {
        super.onPostExecute(strJson);
        taskListener.onFinished(strJson);
    }
}
