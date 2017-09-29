package com.my.first.translator.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;

import com.my.first.translator.R;
import com.my.first.translator.activities.MainActivity;
import com.my.first.translator.databases.TranslationsDataBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Синглтон класс, отвечающий за работу с данными о переводах, включая взаимодействие с
// базой данных и сервером.
public class TranslationsManager {
    private static final TranslationsManager ourInstance = new TranslationsManager();
    private String trnsKey = "trnsl.1.1.20170414T151045Z.21c6aef0ed5dc960.f8c1b78c7a0b54f132365db25c9f37e2b5e8887f";
    private String dictKey = "dict.1.1.20170416T124709Z.d95f2d631e1a917a.ff1d6c6460c9e1f1b26a52cfe3642ddcb6d78304";
    // Пары ключ - значение для языков вида "Русский" - "ru".
    // Возможные направления перевода для словаря.
    private TreeMap<String, String> languages;
    // Направления перевода для словаря.
    private ArrayList<String> dictDirs;
    // История всех переводов.
    private ArrayList<Translation> allTranslations;

    private static final String LANGUAGES = "languages";
    private static final String DICT_DIRS = "dict_dirs";

    private boolean isReady = false;

    public interface TranslationListener {
        void onFinished(Translation translation, String newSourceLanguage);
    }

    public static TranslationsManager getInstance() {
        return ourInstance;
    }

    private TranslationsManager() {

    }

    // Переводы загружаются из базы данных при запуске приложения. В дальшейшем, для оптимизации
    // скорости работы, всё взамодействие с предыдущими переводами производится через сформированный
    // список истории переводов, который в дальнейшем обновляется при необходимости.
    public void loadData(Context context, final TranslationListener translationListener) {
        TranslationListener localListener = new TranslationListener() {
            @Override
            public void onFinished(Translation translation, String newSourceLanguage) {
                if (isReady) {
                    translationListener.onFinished(null, null);
                    isReady = false;
                } else isReady = true;
            }
        };
        if (languages == null || dictDirs == null || allTranslations == null) {
            refreshTranslations(context);
            languages = (TreeMap<String, String>) TranslationsDataBase.getObjectFromDataBase(context, LANGUAGES);
            dictDirs = (ArrayList<String>) TranslationsDataBase.getObjectFromDataBase(context, DICT_DIRS);
            if (languages == null || dictDirs == null) {
                loadLanguages(context, localListener);
                loadDictDirs(context, localListener);
            } else translationListener.onFinished(null, null);
        } else translationListener.onFinished(null, null);
    }

    public void deleteData(Context context) {
        TranslationsDataBase.deleteObjects(context);
        languages = null;
        dictDirs = null;
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.recent_target_languages),
                        context.getString(R.string.english) + " ").commit();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.recent_source_languages),
                        context.getString(R.string.russian) + " ").commit();
    }

    public TreeMap<String, String> getLanguages() {
        return languages;
    }

    public ArrayList<Translation> getTranslations(Context context) {
        if (allTranslations == null && context != null) refreshTranslations(context);
        return allTranslations;
    }

    // Осуществляется перевод. Проверяется определён ли язык оригинала, если нет, то он определяется.
    // Далее если текст представляет из себя одно слово и направление перевода поддерживается
    // словарём, то перевод проводится словарём, иначе переводчиком.
    public void translate(String text, String sourceLanguage, String targetLanguage,
                          TranslationListener translationListener, Context context) {
        if (sourceLanguage.equals(context.getString(R.string.auto_detect)))
            detectSourceLanguage(text, targetLanguage, translationListener, context);
        else {
            String lang = languages.get(sourceLanguage) + "-" + languages.get(targetLanguage);
            if (isAlreadyInHistory(text, lang, sourceLanguage, context, translationListener))
                return;
            if (dictDirs.contains(lang) && !text.contains("+"))
                translateWithDictionary(text, lang, sourceLanguage, context, translationListener);
            else translateWithTranslator(text, lang, sourceLanguage, context, translationListener);
        }
    }

    private void addTranslation(Translation translation, Context context) {
        allTranslations.add(0, translation);
        TranslationsDataBase.addTranslationToDataBase(context, translation);
    }

    public void deleteTranslation(Translation translation, Context context) {
        allTranslations.remove(translation);
        TranslationsDataBase.deleteTranslation(context, translation.getText(), translation.getLang());
    }

    public void resetFavorites(Context context) {
        for (Translation translation : allTranslations) {
            translation.setFavorite(false);
        }
        TranslationsDataBase.resetFavorites(context);
    }

    public void deleteAll(Context context) {
        allTranslations.clear();
        TranslationsDataBase.deleteAll(context);
    }

    public void changeFavorite(Translation translation, Context context) {
        translation.setFavorite(!translation.isFavorite());
        TranslationsDataBase.changeFavorite(context, translation.getText(),
                translation.getLang(), translation.isFavorite());
    }

    private void detectSourceLanguage(final String text, final String targetLanguage,
                                      final TranslationListener translationListener, final Context context) {
        String urlString = "https://translate.yandex.net/api/v1.5/tr.json/detect?" +
                "key=" + trnsKey + "&text=" + text + "&hint=ru,en";
        final TranslationTask.TaskListener taskListener = new TranslationTask.TaskListener() {
            @Override
            public void onFinished(String result) {
                try {
                    String language = new JSONObject(result).getString("lang");
                    if (languages.containsValue(language)) {
                        for (Map.Entry<String, String> m : languages.entrySet()) {
                            if (m.getValue().equals(language)) translate(text, m.getKey(),
                                    targetLanguage, translationListener, context);
                        }
                    } else {
                        translationListener.onFinished(null, null);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        AsyncTaskCompat.executeParallel(new TranslationTask(taskListener), urlString);
    }

    private void loadLanguages(final Context context, final TranslationListener translationListener) {
        String urlString = "https://translate.yandex.net/api/v1.5/tr.json/getLangs?" +
                "key=" + trnsKey + "&ui=" + Locale.getDefault().getLanguage();
        TranslationTask.TaskListener taskListener = new TranslationTask.TaskListener() {
            @Override
            public void onFinished(String result) {
                try {
                    languages = new TreeMap<>();
                    JSONObject langs = new JSONObject(result).getJSONObject("langs");
                    for (int i = 0; i < langs.names().length(); i++) {
                        languages.put(langs.getString(langs.names().get(i).toString()),
                                langs.names().getString(i));
                    }
                    TranslationsDataBase.addObjectToDataBase(context, languages, LANGUAGES);
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putString(context.getString(R.string.current_language),
                                    Locale.getDefault().getLanguage()).apply();
                    translationListener.onFinished(null, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        AsyncTaskCompat.executeParallel(new TranslationTask(taskListener), urlString);
    }

    private void loadDictDirs(final Context context, final TranslationListener translationListener) {
        String urlString = "https://dictionary.yandex.net/api/v1/dicservice.json/" +
                "getLangs?key=" + dictKey;
        TranslationTask.TaskListener taskListener = new TranslationTask.TaskListener() {
            @Override
            public void onFinished(String result) {
                try {
                    dictDirs = new ArrayList<>();
                    JSONArray dirs = new JSONArray(result);
                    for (int i = 0; i < dirs.length(); i++) {
                        dictDirs.add(dirs.get(i).toString());
                    }
                    TranslationsDataBase.addObjectToDataBase(context, dictDirs, DICT_DIRS);
                    translationListener.onFinished(null, null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        AsyncTaskCompat.executeParallel(new TranslationTask(taskListener), urlString);
    }

    public void refreshTranslations(Context context) {
        allTranslations = TranslationsDataBase.getTranslationsFromDataBase(context);
    }

    // Проверяет наличие перевода в истории. Если найден, то загружается из неё без обращения к серверу.
    // Перевод однозначно определяется по тексту оригинала и направлению перевода.
    private boolean isAlreadyInHistory(String text, String lang, String sourceLanguage, Context context,
                                       TranslationListener translationListener) {
        Translation translation = new Translation();
        translation.setText(text.replaceAll("\\+", " "));
        translation.setLang(lang);
        boolean inHistory = allTranslations.contains(translation);
        if (inHistory) {
            for (Translation t : allTranslations) {
                if (t.equals(translation)) translation = t;
            }
            translation.setFavorite(false);
            // Если перевод найдён, то он удаляется из списка истории и базы данных,
            // чтобы снова быть туда добавленным последним.
            deleteTranslation(translation, context);
            addTranslation(translation, context);
            translationListener.onFinished(translation, sourceLanguage);
        }
        return inHistory;
    }

    private void translateWithTranslator(final String text, final String lang, final String sourceLanguage,
                                         final Context context, final TranslationListener translationListener) {
        String urlString = "https://translate.yandex.net/api/v1.5/tr.json/translate?" +
                "key=" + trnsKey + "&text=" + text + "&lang=" + lang;
        TranslationTask.TaskListener taskListener = new TranslationTask.TaskListener() {
            @Override
            public void onFinished(String result) {
                try {
                    JSONArray trns = new JSONObject(result).getJSONArray("text");
                    String fullTranslation = "<font color=\"#000000\"><big>" +
                            trns.get(0) + "</big><br><br><br>" +
                            context.getString(R.string.translated_by) + " </font>" +
                            "<a href=\"" + context.getString(R.string.yandex_link1) + "\">" + context
                            .getString(R.string.yandex_translator) + "</a>";
                    String simpleTranslation = trns.get(0).toString();
                    if (simpleTranslation.length() > 18)
                        fullTranslation = "<br>" + fullTranslation;
                    Translation newTranslation = new Translation(text.replaceAll("\\+", " "),
                            simpleTranslation, fullTranslation, lang, false);
                    addTranslation(newTranslation, context);
                    translationListener.onFinished(newTranslation, sourceLanguage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        AsyncTaskCompat.executeParallel(new TranslationTask(taskListener), urlString);
    }

    private void translateWithDictionary(final String text, final String lang, final String sourceLanguage,
                                         final Context context, final TranslationListener translationListener) {
        String urlString = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" + "key="
                + dictKey + "&lang=" + lang + "&text=" + text;
        TranslationTask.TaskListener taskListener = new TranslationTask.TaskListener() {
            @Override
            public void onFinished(String result) {
                try {
                    JSONArray def = new JSONObject(result).getJSONArray("def");
                    // Перевести с помощью переводчика, если сформировать словарную статью не удалось.
                    if (def.length() == 0) {
                        translateWithTranslator(text, lang, sourceLanguage, context, translationListener);
                        return;
                    }
                    String spelling = "<font color=\"#000000\"><b><big>" +
                            "" + text + "</big></b></font>";
                    String translation = "<font color=\"#000000\"><big>";
                    String simpleTranslation = "";
                    String examples = "";
                    for (int i = 0; i < def.length(); i++) {
                        JSONArray tr = def.getJSONObject(i).getJSONArray("tr");
                        translation += "<br>";
                        if (def.length() > 1) translation += String.valueOf(i + 1) + ". ";
                        // Не более 4 переводов для каждого значения.
                        for (int j = 0; j < (tr.length() > 4 ? 4 : tr.length()); j++) {
                            translation += tr.getJSONObject(j).getString("text") + ", ";
                            if (j == 0 && i == 0)
                                simpleTranslation = tr.getJSONObject(0).getString("text");
                            try {
                                JSONArray ex = tr.getJSONObject(j).getJSONArray("ex");
                                for (int k = 0; k < ex.length(); k++) {
                                    examples += "<font color=\"#000000\">" +
                                            ex.getJSONObject(k).getString("text") + "</font> - ";
                                    JSONArray tr2 = ex.getJSONObject(k).getJSONArray("tr");
                                    for (int l = 0; l < tr2.length(); l++) {
                                        examples += tr2.getJSONObject(l).getString("text") + "<br>";
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        translation = translation.replaceAll(", $", "");
                    }
                    translation += "</big></font><br><br>";
                    String link = "<br><br><font color=\"#000000\">" + context
                            .getString(R.string.translated_by2) + " </font>" +
                            "<a href=\"" + context.getString(R.string.yandex_link2) + "\">" + context
                            .getString(R.string.yandex_dictionary) + "</a>";
                    Translation newTranslation = new Translation(text.replaceAll("\\+", " "), simpleTranslation,
                            spelling + translation + examples + link, lang, false);
                    addTranslation(newTranslation, context);
                    translationListener.onFinished(newTranslation, sourceLanguage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        AsyncTaskCompat.executeParallel(new TranslationTask(taskListener), urlString);
    }
}
