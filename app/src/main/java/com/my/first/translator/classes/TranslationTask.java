package com.my.first.translator.classes;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TranslationTask extends AsyncTask<String, Void, String> {

    private String trnsKey = "trnsl.1.1.20170414T151045Z.21c6aef0ed5dc960.f8c1b78c7a0b54f132365db25c9f37e2b5e8887f";
    private String dictKey = "dict.1.1.20170416T124709Z.d95f2d631e1a917a.ff1d6c6460c9e1f1b26a52cfe3642ddcb6d78304";
    private HttpURLConnection urlConnection = null;
    private BufferedReader reader = null;
    private String resultJson = "";
    private int type;
    private final TaskListener taskListener;

    public interface TaskListener {
        void onFinished(String result);

    }

    public TranslationTask(int type, TaskListener taskListener) {
        this.type = type;
        this.taskListener = taskListener;
    }

    @Override
    protected String doInBackground(String... specs) {
        String urlString = null;
        switch (type) {
            case 0:
                urlString = "https://translate.yandex.net/api/v1.5/tr.json/detect?" +
                        "key=" + trnsKey + "&text=" + specs[0] + "&hint=ru,en";
                break;
            case 1:
                urlString = "https://translate.yandex.net/api/v1.5/tr.json/getLangs?" +
                        "key=" + trnsKey + "&ui=ru";
                break;
            case 2:
                urlString = "https://dictionary.yandex.net/api/v1/dicservice.json/" +
                        "getLangs?key=" + dictKey;
                break;
            case 3:
                urlString = "https://translate.yandex.net/api/v1.5/tr.json/translate?" +
                        "key=" + trnsKey + "&text=" + specs[0] + "&lang=" + specs[1];
                break;
            case 4:
                urlString = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" + "key="
                        + dictKey + "&lang=" + specs[1] + "&text=" + specs[0];
                break;
        }
        try {
            urlConnection = (HttpURLConnection) new URL(urlString).openConnection();
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
        if (taskListener != null) taskListener.onFinished(strJson);
    }
}
