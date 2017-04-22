package com.my.first.translator.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.my.first.translator.R;
import com.my.first.translator.activities.MainActivity;
import com.my.first.translator.classes.Translation;
import com.my.first.translator.classes.TranslationTask;
import com.my.first.translator.databases.TranslationsDataBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.SpeechKit;
import ru.yandex.speechkit.Synthesis;
import ru.yandex.speechkit.Vocalizer;
import ru.yandex.speechkit.VocalizerListener;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class TranslationFragment extends Fragment {

    public TextView mTranslationView;
    public TextView sourceLanguageView;
    public TextView targetLanguageView;
    private ImageButton clearView;
    private ImageView upperSpeakerView;
    private ImageView lowerSpeakerView;
    private ImageView microphoneView;
    private ImageView markView;
    public String sourceLanguage;
    public String targetLanguage;
    private EditText mEditTextView;
    private ProgressBar mProgressBar;
    // Пары ключ - значнение для языков вида "Русский" - "ru".
    public TreeMap<String, String> allLanguages = new TreeMap<>();
    // Возможные направления перевода для словаря.
    public ArrayList<String> dictDirs = new ArrayList<>();
    // Перевод, который будет представлен в экране с историей переводов.
    // Хранит значение sourceLanguage, сохранённое перед поворотом экрана, до момента, пока это
    // значение не будет присвоено sourceLanguage вновь созданной Activity. Пришлось к нему прибегнуть,
    // тк при пересоздании mEditTextView значение sourceLanguage сбрасывается до "Автоопределение",
    // если язык явно не определён.
    private String tempSourceLanguage;
    // Направление перевода.
    private String lang;
    private Translation lastTranslation;
    private boolean translationInProgress;
    private Vocalizer vocalizer;
    private Recognizer recognizer;
    // Язык, поддерживаемый для распознания речи.
    private String recognizedLanguage;
    private String speechKey = "52836c7a-c212-480d-a1f2-417fcd5f612b";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_translation, container, false);
        SpeechKit.getInstance().configure(getActivity().getApplicationContext(), speechKey);
        mTranslationView = (TextView) rootView.findViewById(R.id.translation);
        sourceLanguageView = (TextView) rootView.findViewById(R.id.sourceLanguage);
        targetLanguageView = (TextView) rootView.findViewById(R.id.targetLanguage);
        clearView = (ImageButton) rootView.findViewById(R.id.close);
        upperSpeakerView = (ImageView) rootView.findViewById(R.id.speaker);
        lowerSpeakerView = (ImageView) rootView.findViewById(R.id.speaker2);
        microphoneView = (ImageView) rootView.findViewById(R.id.microphone);
        markView = (ImageView) rootView.findViewById(R.id.mark);
        mEditTextView = (EditText) rootView.findViewById(R.id.editText);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        // Языки оригинала и перевода извлекаются из списков с недавними языками, которые
        // хранятся в виде строк. Текущий язык является первым словом в них.
        // Есди выбрано Автоопределение, то информация об этом хранится в отдельном файле SharedPreferences.
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(
                R.string.is_auto_detect_selected), false)) {
            sourceLanguage = getString(R.string.auto_detect);
        } else {
            sourceLanguage = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(getString(
                            R.string.recent_source_languages), getString(R.string.russian) + " ").split(" ")[0];
        }
        targetLanguage = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(
                        R.string.recent_target_languages), getString(R.string.english) + " ").split(" ")[0];
        sourceLanguageView.setText(sourceLanguage);
        targetLanguageView.setText(targetLanguage);
        if (savedInstanceState != null) {
            tempSourceLanguage = savedInstanceState.getString("sourceLanguage");
            lastTranslation = savedInstanceState.getParcelable("lastTranslation");
            if (lastTranslation != null) {
                mTranslationView.setText(Html.fromHtml(lastTranslation.getFullTranslation()));
                mTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
                refreshIconsVisibility();
            }
        }
        // Загрузка списка языков для переводчика и направлений перевода для словаря.
        mEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                sendRequest(0);
                return false;
            }
        });
        mEditTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Если язык не определён, то sourceLanguage = "Автоопределение", до момента,
                // когда язык определяется с помощью API переводчика. После изменение текста
                // язык снова становится неопределённым.
                if (sourceLanguageView.getText().equals(getString(R.string.auto_detect))) {
                    sourceLanguage = getString(R.string.auto_detect);
                    if (tempSourceLanguage != null) {
                        sourceLanguage = tempSourceLanguage;
                        tempSourceLanguage = null;
                    }
                    if (allLanguages.size() != 0) refreshLang();
                }
                refreshIconsVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mEditTextView.setHorizontallyScrolling(false);
        mEditTextView.setMaxLines(Integer.MAX_VALUE);
        mEditTextView.setPadding(20, 10, 10, 70);
        clearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditTextView.setText("");
                lastTranslation = null;
                refreshIconsVisibility();
            }
        });
        markView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastTranslation.setFavorite(!lastTranslation.isFavorite());
                TranslationsDataBase.changeFavorite(getActivity(), lastTranslation.getText(),
                        lastTranslation.getLang(), lastTranslation.isFavorite());
                refreshIconsVisibility();
            }
        });
        microphoneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getActivity(), RECORD_AUDIO) == PERMISSION_GRANTED) {
                    // Сброс записи, если она уже в процессе.
                    if (recognizer != null) {
                        recognizer.cancel();
                        recognizer = null;
                        mEditTextView.setText("");
                        microphoneView.setImageDrawable(getResources()
                                .getDrawable(R.drawable.ic_microphone_black_24dp));
                        return;
                    }
                    recognizer = Recognizer.create(recognizedLanguage, Recognizer.Model.NOTES,
                            new RecognizerListener() {
                                @Override
                                public void onRecordingBegin(Recognizer recognizer) {
                                    mEditTextView.setText(getString(R.string.recording));
                                    microphoneView.setImageDrawable(getResources()
                                            .getDrawable(R.drawable.ic_microphone_purple_24dp));
                                }

                                @Override
                                public void onSpeechDetected(Recognizer recognizer) {

                                }

                                @Override
                                public void onSpeechEnds(Recognizer recognizer) {

                                }

                                @Override
                                public void onRecordingDone(Recognizer recognizer) {
                                    microphoneView.setImageDrawable(getResources()
                                            .getDrawable(R.drawable.ic_microphone_black_24dp));
                                }

                                @Override
                                public void onSoundDataRecorded(Recognizer recognizer, byte[] bytes) {

                                }

                                @Override
                                public void onPowerUpdated(Recognizer recognizer, float v) {

                                }

                                @Override
                                public void onPartialResults(Recognizer recognizer, Recognition recognition, boolean b) {
                                    mEditTextView.setText(recognition.getBestResultText());
                                }

                                @Override
                                public void onRecognitionDone(Recognizer rec, Recognition recognition) {
                                    if (recognition.getBestResultText().split(" ").length <= 1)
                                        mEditTextView.setText(recognition.getBestResultText().replaceAll("[ .]", ""));
                                    else mEditTextView.setText(recognition.getBestResultText());
                                    sendRequest(0);
                                    recognizer = null;
                                }

                                @Override
                                public void onError(Recognizer rec, Error error) {
                                    mEditTextView.setText("");
                                    microphoneView.setImageDrawable(getResources()
                                            .getDrawable(R.drawable.ic_microphone_black_24dp));
                                    recognizer = null;
                                }
                            });
                    recognizer.start();
                }
            }
        });
        // При изменении типа LanguagesFragment, он удаляется и создаётся заново, вместо простой замены,
        // чтобы сохранить правильный (очевидный для пользователя) порядок в стеке переходов.
        sourceLanguageView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Fragment fragment = getChildFragmentManager().findFragmentById(R.id.container);
                if (fragment == null || !fragment.isAdded() || ((LanguagesFragment) fragment).isTarget) {
                    getChildFragmentManager().popBackStack();
                    getChildFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .add(R.id.container, LanguagesFragment.newInstance(false))
                            .addToBackStack(null).commit();
                } else getChildFragmentManager().popBackStack();
            }
        });
        targetLanguageView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Fragment fragment = getChildFragmentManager().findFragmentById(R.id.container);
                if (fragment == null || !fragment.isAdded() || !((LanguagesFragment) fragment).isTarget) {
                    getChildFragmentManager().popBackStack();
                    getChildFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .add(R.id.container, LanguagesFragment.newInstance(true))
                            .addToBackStack(null).commit();
                } else getChildFragmentManager().popBackStack();
            }
        });
        rootView.findViewById(R.id.exchange).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Автоопредение не может быть выбрано для языка перевода, поэтому вместо него
                // выбирается язык по умолчанию, при перестановке языков оригнала и перевода.
                if (sourceLanguage.equals(getString(R.string.auto_detect))) {
                    sourceLanguage = targetLanguage.equals(getString(R.string.english)) ?
                            getString(R.string.russian) : getString(R.string.english);
                }
                String temp = sourceLanguage;
                sourceLanguage = targetLanguage;
                targetLanguage = temp;
                refreshLang();
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                        .putBoolean(getActivity().getString(R.string.is_auto_detect_selected),
                                false).apply();
                sourceLanguageView.setText(sourceLanguage);
                targetLanguageView.setText(targetLanguage);
                // Запись языков в виде списков недавних языков в SharedPreferences в виде строк.
                LanguagesFragment.saveLanguageInString(getActivity(), true, targetLanguage);
                LanguagesFragment.saveLanguageInString(getActivity(), false, sourceLanguage);
                // Изменение типа LanguagesFragment, если открыт список выбора языков.
                LanguagesFragment fragment = (LanguagesFragment) getChildFragmentManager()
                        .findFragmentById(R.id.container);
                if (fragment != null && fragment.isAdded()) {
                    getChildFragmentManager().popBackStack();
                    getChildFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                    android.R.anim.fade_in, android.R.anim.fade_out)
                            .add(R.id.container, LanguagesFragment.newInstance(!fragment.isTarget))
                            .addToBackStack(null).commit();
                }
                // Перевод слова, после перестановки языков местами.
                if (lastTranslation != null) {
                    mEditTextView.setText(lastTranslation.getSimpleTranslation());
                    sendRequest(0);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sendRequest(1);
        sendRequest(2);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sourceLanguage", sourceLanguage);
        outState.putParcelable("lastTranslation", lastTranslation);
        if (recognizer != null) mEditTextView.setText("");
        super.onSaveInstanceState(outState);
    }


    // Поддерживает значение lang в актуальном состоянии.
    public void refreshLang() {
        // При неопределённом языке нельзя распознать запись голоса.
        if (sourceLanguage.equals(getString(R.string.auto_detect))) {
            recognizedLanguage = null;
            refreshIconsVisibility();
            return;
        }
        lang = allLanguages.get(sourceLanguage) + "-" + allLanguages.get(targetLanguage);
        // Определние языка для распознания речи
        switch (allLanguages.get(sourceLanguage)) {
            case "en":
                recognizedLanguage = Recognizer.Language.ENGLISH;
                break;
            case "ru":
                recognizedLanguage = Recognizer.Language.RUSSIAN;
                break;
            case "tr":
                recognizedLanguage = Recognizer.Language.TURKISH;
                break;
            case "uk":
                recognizedLanguage = Recognizer.Language.UKRAINIAN;
                break;
            default:
                recognizedLanguage = null;
                break;
        }
        refreshIconsVisibility();
    }

    public View.OnClickListener getOnSpeakerClickListener(final String language, final String text,
                                                          final int defaultDrawableId,
                                                          final int actionDrawableId,
                                                          final ImageView speaker) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vocalizer != null) vocalizer.cancel();
                vocalizer = Vocalizer.createVocalizer(language, text, true, Vocalizer.Voice.ERMIL);
                vocalizer.setListener(new VocalizerListener() {
                    @Override
                    public void onSynthesisBegin(Vocalizer vocalizer) {
                        speaker.setImageDrawable(getResources()
                                .getDrawable(actionDrawableId));
                    }

                    @Override
                    public void onSynthesisDone(Vocalizer vocalizer, Synthesis synthesis) {

                    }

                    @Override
                    public void onPlayingBegin(Vocalizer vocalizer) {

                    }

                    @Override
                    public void onPlayingDone(Vocalizer vocalizer) {
                        speaker.setImageDrawable(getResources()
                                .getDrawable(defaultDrawableId));
                    }

                    @Override
                    public void onVocalizerError(Vocalizer vocalizer, Error error) {
                        speaker.setImageDrawable(getResources()
                                .getDrawable(defaultDrawableId));
                    }
                });
                vocalizer.start();
            }
        };
    }

    // Функция запускает TranslationTask, которая отвечает за взаимодействие с сервером через JSON интерфейс.
    // Различные виды запросов определяются значением type.
    // type == 0 Если язык неопределён, то он определяется, далее формируется словарная статья,
    // если это возможно(type = 4), иначе перевод выполняется переводчиком(type = 3).
    // type == 1 Происходит запрос списка языков и его обработка.
    // type == 2 Происходит запрос списка поддерживаемых направлений перевода для словаря и их обработка.
    // type == 3 Происходит запрос на перевод текста и его обработка.
    // type == 4 Происходит запрос на формирование словарной статьи и его обработка.
    public void sendRequest(int type) {
        // При передачи текста в URL запрос, необходимо заменить пробелы на знак '+'.
        final String text = mEditTextView.getText().toString().trim().replaceAll(" ", "+");
        if (type == 0) {
            if (text.equals("")) return;
            translationInProgress = true;
            refreshIconsVisibility();
            // Если язык уже определён, то этап его определения пропускается и управление
            // передаётся другой AsyncTask, для его непосредственного перевода.
            if (!sourceLanguage.equals(getActivity().getString(R.string.auto_detect))) {
                if (checkTranslationInHistory(text, lang)) return;
                if (dictDirs.contains(lang) && !text.contains("+")) sendRequest(4);
                else sendRequest(3);
                return;
            }
        }
        // Интерфейс, котороый определяет метод, который будет вызван после завершения работы AsyncTask.
        TranslationTask.TaskListener taskListener = null;
        switch (type) {
            case 0:
                taskListener = new TranslationTask.TaskListener() {
                    @Override
                    public void onFinished(String strJson) {
                        try {
                            String language = new JSONObject(strJson).getString("lang");
                            if (allLanguages.containsValue(language)) {
                                // Изменение значения sourceLanguage на только что определённый язык,
                                // без изменения соотвествующего View.
                                for (Map.Entry<String, String> m : allLanguages.entrySet()) {
                                    if (m.getValue().equals(language)) sourceLanguage = m.getKey();
                                }
                                refreshLang();
                                // Рекурсивный переход, для последующей передачи управления AsyncTask перевода.
                                sendRequest(0);
                            } else {
                                translationInProgress = false;
                                refreshIconsVisibility();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                break;
            case 1:
                taskListener = new TranslationTask.TaskListener() {
                    @Override
                    public void onFinished(String strJson) {
                        try {
                            JSONObject langs = new JSONObject(strJson).getJSONObject("langs");
                            for (int i = 0; i < langs.names().length(); i++) {
                                allLanguages.put(langs.getString(langs.names().get(i).toString()),
                                        langs.names().getString(i));
                            }
                            refreshLang();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                break;
            case 2:
                taskListener = new TranslationTask.TaskListener() {
                    @Override
                    public void onFinished(String strJson) {
                        try {
                            JSONArray dirs = new JSONArray(strJson);
                            for (int i = 0; i < dirs.length(); i++) {
                                dictDirs.add(dirs.get(i).toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                break;
            case 3:
                taskListener = new TranslationTask.TaskListener() {
                    @Override
                    public void onFinished(String strJson) {
                        try {
                            JSONArray trns = new JSONObject(strJson).getJSONArray("text");
                            String fullTranslation = "<font color=\"#000000\"><big>" +
                                    trns.get(0) + "</big><br><br><br>" +
                                    "Переведено сервисом </font>" +
                                    "<a href=\"http://translate.yandex.ru/\">«Яндекс.Переводчик»</a>";
                            String simpleTranslation = trns.get(0).toString();
                            if (simpleTranslation.length() > 18)
                                fullTranslation = "<br>" + fullTranslation;
                            lastTranslation = new Translation(text.replaceAll("\\+", " "),
                                    simpleTranslation, fullTranslation, lang, false);
                            setTranslation(lastTranslation, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                break;
            case 4:
                taskListener = new TranslationTask.TaskListener() {
                    @Override
                    public void onFinished(String strJson) {
                        try {
                            String spelling = "<font color=\"#000000\"><b><big>" +
                                    "" + text + "</big></b></font>";
                            String translation = "<font color=\"#000000\"><big>";
                            String simpleTranslation = "";
                            String examples = "";
                            JSONArray def = new JSONObject(strJson).getJSONArray("def");
                            // Перевести с помощью переводчика, если сформировать словарную статью не удалось.
                            if (def.length() == 0) {
                                sendRequest(3);
                                return;
                            }
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
                            String link = "<br><br><font color=\"#000000\">Реализовано с помощью сервиса </font>" +
                                    "<a href=\"https://tech.yandex.ru/dictionary/\">«Яндекс.Словарь»</a>";
                            lastTranslation = new Translation(text.replaceAll("\\+", " "), simpleTranslation,
                                    spelling + translation + examples + link, lang, false);
                            setTranslation(lastTranslation, false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                break;
        }
        AsyncTaskCompat.executeParallel(new TranslationTask(type, taskListener), text, lang);
    }

    // Определяет какие значки должны быть видимыми, исходя из текущего состояния фрагмента.
    public void refreshIconsVisibility() {
        clearView.setVisibility(View.VISIBLE);
        microphoneView.setVisibility(View.VISIBLE);
        upperSpeakerView.setVisibility(View.VISIBLE);
        lowerSpeakerView.setVisibility(View.VISIBLE);
        markView.setVisibility(View.VISIBLE);
        mTranslationView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        if (recognizedLanguage == null) {
            microphoneView.setVisibility(View.INVISIBLE);
            mEditTextView.setPadding(20, 10, 10, 70);
        } else mEditTextView.setPadding(20, 10, 70, 70);
        if (sourceLanguage.equals(getString(R.string.auto_detect)))
            upperSpeakerView.setVisibility(View.INVISIBLE);
        if (lastTranslation == null) {
            lowerSpeakerView.setVisibility(View.INVISIBLE);
            markView.setVisibility(View.INVISIBLE);
            mTranslationView.setVisibility(View.INVISIBLE);
        }
        if (mEditTextView.getText().length() == 0) {
            upperSpeakerView.setVisibility(View.INVISIBLE);
            clearView.setVisibility(View.INVISIBLE);
        }
        if (translationInProgress) mTranslationView.setVisibility(View.INVISIBLE);
        else mProgressBar.setVisibility(View.INVISIBLE);
        if (upperSpeakerView.getVisibility() == View.VISIBLE)
            upperSpeakerView.setOnClickListener(getOnSpeakerClickListener(allLanguages.get(sourceLanguage),
                    mEditTextView.getText().toString(), R.drawable.ic_speaker_black_24dp,
                    R.drawable.ic_speaker_purple_24dp, upperSpeakerView));
        if (lowerSpeakerView.getVisibility() == View.VISIBLE)
            lowerSpeakerView.setOnClickListener(getOnSpeakerClickListener(allLanguages.get(targetLanguage),
                    lastTranslation.getSimpleTranslation(), R.drawable.ic_speaker_black_24dp,
                    R.drawable.ic_speaker_purple_24dp, lowerSpeakerView));
        if (lastTranslation != null && lastTranslation.isFavorite())
            markView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_purple_24dp));
        else
            markView.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_black_24dp));
    }

    // Отображает перевод на экране.
    // Производится смена выбранных языков, если перевод загружен из истории.
    public void setTranslation(Translation translation, boolean changeLang) {
        lastTranslation = translation;
        mEditTextView.setText(translation.getText());
        mTranslationView.setText(Html.fromHtml(translation.getFullTranslation()));
        mTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
        if (changeLang) {
            String[] langs = translation.getLang().split("-");
            for (Map.Entry<String, String> m : allLanguages.entrySet()) {
                if (m.getValue().equals(langs[0])) sourceLanguage = m.getKey();
                if (m.getValue().equals(langs[1])) targetLanguage = m.getKey();
            }
            sourceLanguageView.setText(sourceLanguage);
            targetLanguageView.setText(targetLanguage);
        } else {
            // Если перевод не загружен из истории, то его следует добавить в базу данных
            // и список истории переводов.
            TranslationsDataBase.addTranslationToDataBase(getActivity(), lastTranslation);
            ((MainActivity) getActivity()).allTranslations.add(0, lastTranslation);
        }
        translationInProgress = false;
        refreshIconsVisibility();
    }

    // Проверяет наличие перевода в истории. Если найден, то загружается из неё без обращения к серверу.
    // Перевод однозначно определяется по тексту оригинала и направлению перевода.
    public boolean checkTranslationInHistory(String text, String lang) {
        Translation translation = new Translation();
        translation.setText(text);
        translation.setLang(lang);
        boolean inHistory = ((MainActivity) getActivity()).allTranslations.contains(translation);
        if (inHistory) {
            for (Translation t : ((MainActivity) getActivity()).allTranslations) {
                if (t.equals(translation)) translation = t;
            }
            // Если перевод найдён, то он удаляется из списка истории и базы данных,
            // чтобы снова быть туда добавленным последним.
            ((MainActivity) getActivity()).allTranslations.remove(translation);
            TranslationsDataBase.deleteTranslation(getActivity(), translation.getText(),
                    translation.getLang());
            setTranslation(translation, false);
        }
        return inHistory;
    }
}