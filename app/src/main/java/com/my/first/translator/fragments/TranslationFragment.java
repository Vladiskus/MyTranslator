package com.my.first.translator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.my.first.translator.R;
import com.my.first.translator.classes.Translation;
import com.my.first.translator.classes.TranslationsManager;

import java.util.Locale;
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
    private ImageButton exchangeView;
    private ImageView upperSpeakerView;
    private ImageView lowerSpeakerView;
    private ImageView microphoneView;
    private ImageView markView;
    public String sourceLanguage;
    public String targetLanguage;
    private EditText mEditTextView;
    private ProgressBar mProgressBar;
    private TranslationsManager translationsManager = TranslationsManager.getInstance();
    // Пары ключ - значение для языков вида "Русский" - "ru".
    // Возможные направления перевода для словаря.
    private TreeMap<String, String> allLanguages = new TreeMap<>();
    // Перевод, который будет представлен в экране с историей переводов.
    // Хранит значение sourceLanguage, сохранённое перед поворотом экрана, до момента, пока это
    // значение не будет присвоено sourceLanguage вновь созданной Activity. Пришлось к нему прибегнуть,
    // тк при пересоздании mEditTextView значение sourceLanguage сбрасывается до "Автоопределение",
    // если язык явно не определён.
    private String tempSourceLanguage;
    // Направление перевода.
    private Translation lastTranslation;
    private boolean translationInProgress;
    private Vocalizer vocalizer;
    private Recognizer recognizer;
    // Язык, поддерживаемый для распознания речи.
    private String recognizedLanguage;
    private String speechKey = "52836c7a-c212-480d-a1f2-417fcd5f612b";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_translation, container, false);
        SpeechKit.getInstance().configure(getActivity().getApplicationContext(), speechKey);
        if (!Locale.getDefault().getLanguage().equals(PreferenceManager
                .getDefaultSharedPreferences(getActivity()).getString(getString(R.string.current_language), ""))) {
            translationsManager.deleteData(getActivity());
        }
        translationsManager.loadData(getActivity(), new TranslationsManager.TranslationListener() {
            @Override
            public void onFinished(Translation translation, String newSourceLanguage) {
                mTranslationView = (TextView) rootView.findViewById(R.id.translation);
                sourceLanguageView = (TextView) rootView.findViewById(R.id.sourceLanguage);
                targetLanguageView = (TextView) rootView.findViewById(R.id.targetLanguage);
                clearView = (ImageButton) rootView.findViewById(R.id.close);
                exchangeView = (ImageButton) rootView.findViewById(R.id.exchange);
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
                    if (sourceLanguage.equals(getString(R.string.russian)))
                        recognizedLanguage = Recognizer.Language.RUSSIAN;
                    refreshIconsVisibility();
                }
                targetLanguage = PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getString(getString(
                                R.string.recent_target_languages), getString(R.string.english) + " ").split(" ")[0];
                sourceLanguageView.setText(sourceLanguage);
                targetLanguageView.setText(targetLanguage);
                // После загрузки языков с помошью полученных пар ключ-значение необходимо будет проверить
                // поддерживает ли текущий язык голосовой ввод.
                allLanguages = translationsManager.getLanguages();
                if (savedInstanceState != null) {
                    tempSourceLanguage = savedInstanceState.getString("sourceLanguage");
                    lastTranslation = savedInstanceState.getParcelable("lastTranslation");
                    if (lastTranslation != null) {
                        mTranslationView.setText(Html.fromHtml(lastTranslation.getFullTranslation()));
                        mTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
                        refreshRecognizedLanguage();
                    }
                }
                mEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        translate();
                        return false;
                    }
                });
                mEditTextView.addTextChangedListener(textWatcher);
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
                        translationsManager.changeFavorite(lastTranslation, getActivity());
                        refreshIconsVisibility();
                    }
                });
                microphoneView.setOnClickListener(micOnClickListener);
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
                exchangeView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Автоопредение не может быть выбрано для языка перевода, поэтому вместо него
                        // выбирается уже определённый язык или язык по умолчанию, при перестановке языков
                        // оригнала и перевода.
                        if (sourceLanguage.equals(getString(R.string.auto_detect))) {
                            sourceLanguage = targetLanguage.equals(getString(R.string.english)) ?
                                    getString(R.string.russian) : getString(R.string.english);
                        }
                        String temp = sourceLanguage;
                        sourceLanguage = targetLanguage;
                        targetLanguage = temp;
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                .putBoolean(getActivity().getString(R.string.is_auto_detect_selected),
                                        false).apply();
                        sourceLanguageView.setText(sourceLanguage);
                        refreshRecognizedLanguage();
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
                        refreshRecognizedLanguage();
                        // Перевод слова, после перестановки языков местами.
                        if (lastTranslation != null) {
                            mEditTextView.setText(lastTranslation.getSimpleTranslation());
                            translate();
                        }
                    }
                });
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sourceLanguage", sourceLanguage);
        outState.putParcelable("lastTranslation", lastTranslation);
        if (recognizer != null) mEditTextView.setText("");
        super.onSaveInstanceState(outState);
    }

    private TextWatcher textWatcher = new TextWatcher() {
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
            }
            refreshIconsVisibility();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private View.OnClickListener micOnClickListener = new View.OnClickListener() {
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
                                if (getActivity() == null) return;
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
                                if (getActivity() == null) return;
                                mEditTextView.setText(recognition.getBestResultText().toLowerCase()
                                        .replaceAll("(?m)^i ", "I ").replaceAll(" i ", " I "));
                            }

                            @Override
                            public void onRecognitionDone(Recognizer rec, Recognition recognition) {
                                if (getActivity() == null) return;
                                String text = recognition.getBestResultText().toLowerCase()
                                        .replaceAll("(?m)^i ", "I ").replaceAll(" i ", " I ");
                                if (recognition.getBestResultText().split(" ").length <= 1)
                                    mEditTextView.setText(text.replaceAll("[ .]", ""));
                                else mEditTextView.setText(text);
                                translate();
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
    };

    private View.OnClickListener getOnSpeakerClickListener(final String language,
                                                           final String text,
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
                        getOnSpeakerClickListener(language, text, defaultDrawableId,
                                actionDrawableId, speaker);
                    }
                });
                vocalizer.start();
            }
        };
    }

    // Запрос перевода текста у TranslationsManager.
    public void translate() {
        String text = mEditTextView.getText().toString().trim().replaceAll(" ", "+");
        if (text.equals("")) return;
        translationInProgress = true;
        refreshIconsVisibility();
        TranslationsManager.TranslationListener listener = new TranslationsManager.TranslationListener() {
            @Override
            public void onFinished(Translation translation, String newSourceLanguage) {
                if (getActivity() == null) return;
                if (translation == null) {
                    translationInProgress = false;
                    refreshIconsVisibility();
                } else {
                    sourceLanguage = newSourceLanguage;
                    setTranslation(translation, false);
                }
                getView().findViewById(R.id.editLayout).requestFocus();
            }
        };
        translationsManager.translate(text, sourceLanguage, targetLanguage, listener, getActivity());
    }

    // Отображает перевод на экране.
    // Производится смена выбранных языков, если перевод загружен из истории.
    public void setTranslation(Translation translation, boolean changeLang) {
        lastTranslation = translation;
        mTranslationView.setText(Html.fromHtml(translation.getFullTranslation()));
        mTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
        if (changeLang) {
            mEditTextView.setText(translation.getText());
            String[] langs = translation.getLang().split("-");
            for (Map.Entry<String, String> m : allLanguages.entrySet()) {
                if (m.getValue().equals(langs[0])) sourceLanguage = m.getKey();
                if (m.getValue().equals(langs[1])) targetLanguage = m.getKey();
            }
            sourceLanguageView.setText(sourceLanguage);
            refreshRecognizedLanguage();
            targetLanguageView.setText(targetLanguage);
        }
        translationInProgress = false;
        refreshIconsVisibility();
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

    // Проверяет, поддерживается ли текущий язык системой распознания голоса.
    public void refreshRecognizedLanguage() {
        try {
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
        } catch (NullPointerException e) {
            recognizedLanguage = null;
        }
        refreshIconsVisibility();
    }
}