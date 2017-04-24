package com.my.first.translator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.my.first.translator.R;
import com.my.first.translator.classes.TranslationsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LanguagesFragment extends Fragment {

    // Для выбора языков оригинала и перевода создаются фрагменты с разным значениям isTarget.
    // Они отличаются стороной выравнивания списка, различными недавними выборами языков и наличием
    // возможности выбора автоопределения для оригинала.
    public boolean isTarget;
    private ListView mListView;
    private ArrayList<String> recentLanguages;
    private ArrayList<String> mainList = new ArrayList<>();
    String targetLanguage;
    String sourceLanguage;

    // Создание фрагмента для списка языков оригинала или первода в зависимости от значения isTarget.
    public static LanguagesFragment newInstance(boolean isTarget) {
        LanguagesFragment fragment = new LanguagesFragment();
        Bundle args = new Bundle();
        args.putBoolean("is_target", isTarget);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_languages, container, false);
        targetLanguage = ((TranslationFragment) getParentFragment()).targetLanguage;
        sourceLanguage = ((TranslationFragment) getParentFragment()).sourceLanguage;
        isTarget = getArguments().getBoolean("is_target");
        mListView = (ListView) rootView.findViewById(R.id.list_view);
        recentLanguages = getArrayFromString(getActivity(), isTarget);
        // Последовательное добавление списков с недавними языками и всеми языками в общий список.
        mainList.addAll(recentLanguages);
        mainList.addAll(TranslationsManager.getInstance().getLanguages().keySet());
        mListView.setAdapter(adapter);
        return rootView;
    }

    private BaseAdapter adapter = new BaseAdapter() {
        // Строки, которым соответствует выбранный язык.
        ArrayList<View> markedViews = new ArrayList<>();

        @Override
        public int getCount() {
            // Помимо строк с языками в списке присутсвуют 2 подзаголовка и возможность
            // выбора автоопределния для оригинала.
            return isTarget ? mainList.size() + 2 : mainList.size() + 3;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            // Для оригинала первой строкой идёт Автоопредение, после чего значение position
            // понижается, чтобы соответсвовать строкам языков для перевода.
            if (!isTarget && position == 0) {
                view = View.inflate(getActivity(), R.layout.list_item, null);
                final TextView text = ((TextView) view.findViewById(R.id.textView));
                text.setText(getString(R.string.auto_detect));
                // Выделить строку с выбранным языком
                if (text.getText().toString().equals(isTarget ? targetLanguage : sourceLanguage)) {
                    markSelected(view, markedViews);
                    markedViews.add(view);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((TranslationFragment) getParentFragment()).sourceLanguage = text.getText().toString();
                        ((TextView) getActivity().findViewById(isTarget ? R.id.targetLanguage :
                                R.id.sourceLanguage)).setText(text.getText().toString());
                        ((TranslationFragment) getParentFragment()).refreshRecognizedLanguage();
                        // Вместо добавляения Автоопределения в список недавних языков, создаётся
                        // отдельная пометка, чтобы предотвратить его отображение в группе
                        // недавно использованных языков.
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                .putBoolean(getActivity().getString(R.string.is_auto_detect_selected),
                                        true).apply();
                        ((TranslationFragment) getParentFragment()).translate();
                        getParentFragment().getChildFragmentManager().popBackStack();
                    }
                });
                return view;
            } else if (!isTarget) position--;
            if (position == 0 || position == recentLanguages.size() + 1) {
                // Создание строк для подзаголовков.
                view = View.inflate(getActivity(), R.layout.header_list_item, null);
                TextView text = ((TextView) view.findViewById(R.id.textView));
                text.setText(getString(position == 0 ? R.string.recently_used : R.string.all_languages));
                if (isTarget) text.setGravity(Gravity.END);
            } else {
                // Создание строк с языками.
                view = View.inflate(getActivity(), R.layout.list_item, null);
                final TextView text = ((TextView) view.findViewById(R.id.textView));
                final ImageView checkMark = (ImageView) view.findViewById(R.id.checkMark);
                text.setText(position < recentLanguages.size() + 1 ?
                        mainList.get(position - 1) : mainList.get(position - 2));
                // Выделить строку с выбранным языком.
                if (text.getText().toString().equals(isTarget ? targetLanguage : sourceLanguage)) {
                    markSelected(view, markedViews);
                    markedViews.add(view);
                }
                // Задаётся выравнивание по правой стороне для списка языков перевода.
                if (isTarget) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) text.getLayoutParams();
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    text.setLayoutParams(params);
                    params = (RelativeLayout.LayoutParams) checkMark.getLayoutParams();
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    checkMark.setLayoutParams(params);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String language = text.getText().toString();
                        // Нельзя допустить, чтобы языки оригинала и перевода совпали.
                        if (language.equals((isTarget) ? sourceLanguage : targetLanguage)) {
                            getParentFragment().getChildFragmentManager().popBackStack();
                            getParentFragment().getChildFragmentManager().executePendingTransactions();
                            getActivity().findViewById(R.id.exchange).performClick();
                            return;
                        }
                        if (isTarget) ((TranslationFragment) getParentFragment()).targetLanguage = language;
                        else ((TranslationFragment) getParentFragment()).sourceLanguage = language;
                        markSelected(view, markedViews);
                        saveLanguageInString(getActivity(), isTarget, language);
                        ((TextView) getActivity().findViewById(isTarget ? R.id.targetLanguage :
                                R.id.sourceLanguage)).setText(text.getText().toString());
                        ((TranslationFragment) getParentFragment()).refreshRecognizedLanguage();
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                                .putBoolean(getActivity().getString(R.string.is_auto_detect_selected),
                                        false).apply();
                        ((TranslationFragment) getParentFragment()).translate();
                        getParentFragment().getChildFragmentManager().popBackStack();
                    }
                });
            }
            return view;
        }
    };

    // Пометить нужную строку галочкой и цветом. Снять выделение для предыдущих выбранных строк,
    // если необходимо.
    private void markSelected(View view, ArrayList<View> markedViews) {
        for (View v : markedViews) {

            if (!((TextView) v.findViewById(R.id.textView)).getText().toString().equals(isTarget ?
                    targetLanguage : sourceLanguage)) {
                v.findViewById(R.id.checkMark).setVisibility(View.INVISIBLE);
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorBackground));
            }
        }
        view.findViewById(R.id.checkMark).setVisibility(View.VISIBLE);
        view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorCheck));
    }

    // Запись ArrayList в SharedPreferences невозможна, поэтому для сохранения
    // списка недавних языков создаётся строка, в которой языки разделены через
    // пробел с сохранением необходимой последовательности и лимитом на количество
    // языков в 3. Пример строки: "Русский Английский Китайский"
    // Языки не должны повторяться.
    public static void saveLanguageInString(Context context, boolean isTarget, String language) {
        ArrayList<String> recentLanguages = getArrayFromString(context, isTarget);
        int size = recentLanguages.size();
        String array = language;
        boolean shouldntRemoveLast = size < 3 || recentLanguages.contains(language);
        for (int i = 0; i < (shouldntRemoveLast ? size : size - 1); i++) {
            if (language.equals(recentLanguages.get(i))) continue;
            array += " " + recentLanguages.get(i);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(isTarget ? R.string.recent_target_languages :
                        R.string.recent_source_languages), array).apply();
    }

    // Преобразование записанной строки в ArrayList недавних языков.
    public static ArrayList<String> getArrayFromString(Context context, boolean isTarget) {
        ArrayList<String> list;
        try {
            list = new ArrayList<>(Arrays.asList(PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString(context.getString(isTarget ? R.string.recent_target_languages :
                            R.string.recent_source_languages), null).split(" ")));
        } catch (NullPointerException e) {
            list = new ArrayList<>();
            list.add(context.getString(isTarget ? R.string.english : R.string.russian));
        }
        return list;
    }
}
