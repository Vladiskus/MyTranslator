package com.my.first.translator.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.my.first.translator.R;
import com.my.first.translator.activities.MainActivity;
import com.my.first.translator.classes.Translation;
import com.my.first.translator.classes.TranslationsManager;

import java.util.ArrayList;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class HistoryFragment extends Fragment {

    private ViewGroup mContainerView;
    private EditText searchFieldView;
    private ImageView deleteView;
    private boolean isFavorites;
    TranslationsManager translationsManager = TranslationsManager.getInstance();
    private ArrayList<Translation> allTranslations = new ArrayList<>();


    // Возможны две вариации фрагментов в зависимости от того, представляет ли фрагмент всю историю
    // переводов или только избранные элементы.
    public static HistoryFragment newInstance(boolean isFavorites) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putBoolean("isFavorites", isFavorites);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        mContainerView = (ViewGroup) rootView.findViewById(R.id.container);
        searchFieldView = (EditText) rootView.findViewById(R.id.editText);
        deleteView = (ImageView) rootView.findViewById(R.id.delete);
        isFavorites = getArguments().getBoolean("isFavorites");
        allTranslations = translationsManager.getTranslations(getActivity());
        deleteView.setOnClickListener(deleteAllListener);
        rootView.findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFieldView.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                        .getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(searchFieldView, InputMethodManager.SHOW_IMPLICIT);
                searchFieldView.setCursorVisible(true);
            }
        });
        searchFieldView.setHint(isFavorites ? R.string.search_in_favorites : R.string.search_in_history);
        // При поиске в истории значок корзины заменяется на крестик и теперь, вместо удаления
        // всех переводов, просто очищает поле с текстом поиска.
        searchFieldView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                searchFieldView.setCursorVisible(hasFocus);
                deleteView.setImageDrawable(getResources().getDrawable(hasFocus ?
                        R.drawable.ic_close_black_24dp : R.drawable.ic_delete_white_24dp));
                deleteView.setOnClickListener(hasFocus ? clearListener : deleteAllListener);
            }
        });
        searchFieldView.addTextChangedListener(textWatcher);
        refreshContainer(allTranslations);
        return rootView;
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String text = s.toString().toLowerCase();
            // Формирует новый подходящий список и запрашивает изменение mContainerView
            // в соответствии с ним.
            ArrayList<Translation> searchList = new ArrayList<>();
            for (Translation translation : allTranslations) {
                if ((translation.getText().toLowerCase().contains(text) ||
                        translation.getSimpleTranslation().toLowerCase().contains(text)))
                    searchList.add(translation);
            }
            refreshContainer(searchList);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    View.OnClickListener deleteAllListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DeleteDialogFragment.newInstance(isFavorites).show(HistoryFragment.this
                    .getChildFragmentManager(), "DeleteDialogFragment");
        }
    };

    View.OnClickListener clearListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            searchFieldView.setText("");
            ((MainActivity) getActivity()).hideKeyboard();
        }
    };

    // Приводит имеющийся список элементов в контейнере к новому изменённому виду.
    // Такой подход применятся, чтобы достичь анимированной перестановки элементов.
    // Если изначальный список длинее необходимого, то лишние его элементы удаляются,
    // если же короче, то наоборот добавляются недостающие.
    // Удаление происходит из объекта ViewGroup, верхние элементы которого будут тут же
    // проваливаться на нижнюю освободившуюся ячейку (если под нижними подразумевать элементы с
    // меньшим индексом).
    public void refreshContainer(ArrayList<Translation> newList) {
        // Выделение избранных переводов из общего списка, если пользователь находится в папке Избранное.
        ArrayList<Translation> realList = new ArrayList<>();
        realList.addAll(newList);
        if (isFavorites) {
            for (Translation translation : newList) {
                if (!translation.isFavorite()) realList.remove(translation);
            }
        }
        boolean isAppropriate;
        for (int i = 0; i < realList.size(); i++) {
            try {
                // Соответствие переводов однозначно определяется совпадением текста оригинала и
                // направления перевода.
                isAppropriate = ((TextView) mContainerView.getChildAt(i).findViewById(R.id.text))
                        .getText().equals(realList.get(i).getText()) && ((TextView) mContainerView
                        .getChildAt(i).findViewById(R.id.lang)).getText().toString()
                        .equals(realList.get(i).getLang());
            } catch (Exception e) {
                isAppropriate = false;
            }
            if (!isAppropriate) {
                if (mContainerView.getChildCount() >= realList.size()) {
                    mContainerView.removeViewAt(i);
                    i--;
                } else {
                    Translation translation = realList.get(i);
                    addTranslation(translation, i);
                }
            } else {
                ImageView mark = ((ImageView) mContainerView.getChildAt(i).findViewById(R.id.mark));
                if (realList.get(i).isFavorite())
                    mark.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_purple_24dp));
                else
                    mark.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_black_24dp));
            }
        }
        // Если изначальный список длинее необходимого, то после некоторого числа удалений в нём
        // останутся только лишние элементы, имеющие индексы, которых уже нет в необходимом нам списке.
        if (realList.size() < mContainerView.getChildCount()) {
            for (int i = mContainerView.getChildCount() - 1; i >= realList.size(); i--) {
                mContainerView.removeViewAt(i);
            }
        }
        // Если список пуст, то выводится специальное сообщение.
        if (getView() == null) return;
        View view = getView().findViewById(R.id.no_results);
        view.setVisibility(View.INVISIBLE);
        if (realList.size() == 0 && getView() != null) {
            if (!isFavorites) {
                ((TextView) view.findViewById(R.id.textView)).setText(getString(R.string.no_matches));
                if (allTranslations.size() == 0)
                    ((TextView) view.findViewById(R.id.textView)).setText(getString(R.string.no_translations));
                ((ImageView) view.findViewById(R.id.imageView)).setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_history_black_48dp));
            } else {
                ((TextView) view.findViewById(R.id.textView)).setText(getString(R.string.no_favorites));
                ((ImageView) view.findViewById(R.id.imageView)).setImageDrawable(getResources().getDrawable(
                        R.drawable.ic_mark_black_48dp));
            }
            view.setVisibility(View.VISIBLE);
        }
    }

    // Добавление элемента к объекту ViewGroup.
    public void addTranslation(final Translation translation, int index) {
        final View convertView = View.inflate(getActivity(), R.layout.list_position, null);
        final String text = translation.getText();
        final String simpleTranslation = translation.getSimpleTranslation();
        final String lang = translation.getLang();
        final boolean isFavorite = translation.isFavorite();
        ((TextView) convertView.findViewById(R.id.text)).setText(text);
        ((TextView) convertView.findViewById(R.id.translation)).setText(simpleTranslation);
        ((TextView) convertView.findViewById(R.id.lang)).setText(lang);
        final ImageView mark = ((ImageView) convertView.findViewById(R.id.mark));
        if (isFavorite)
            mark.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_purple_24dp));
        else
            mark.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_black_24dp));
        mark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Translation translation : allTranslations) {
                    if (translation.getText().equals(text) && translation.getLang().equals(lang)) {
                        if (isFavorite)
                            mark.setImageDrawable(getResources().getDrawable(R.drawable.ic_mark_black_24dp));
                        translationsManager.changeFavorite(translation, getActivity());
                        break;
                    }
                }
                refreshContainer(allTranslations);
            }
        });
        convertView.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Translation> copyList = new ArrayList<>();
                copyList.addAll(allTranslations);
                for (Translation translation : copyList) {
                    if (translation.getText().equals(text) && translation.getLang().equals(lang)) {
                        if (isFavorites) translationsManager.changeFavorite(translation, getActivity());
                        else translationsManager.deleteTranslation(translation, getActivity());
                        break;
                    }
                }
                refreshContainer(allTranslations);
            }
        });
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager viewPager = ((MainActivity) getActivity()).mPager;
                ((TranslationFragment) viewPager.getAdapter().instantiateItem(viewPager, 0))
                        .setTranslation(translation, true);
                ((MainActivity) getActivity()).navigationView.setSelectedItemId(R.id.navigation_translator);
                viewPager.setCurrentItem(0);
            }
        });
        mContainerView.addView(convertView, index);
    }

}
