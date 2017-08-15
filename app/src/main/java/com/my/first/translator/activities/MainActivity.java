package com.my.first.translator.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.my.first.translator.R;
import com.my.first.translator.classes.CustomViewPager;
import com.my.first.translator.classes.TranslationsManager;
import com.my.first.translator.fragments.HistoryFragment;
import com.my.first.translator.fragments.TranslationFragment;

public class MainActivity extends AppCompatActivity {

    public CustomViewPager mPager;
    public BottomNavigationView navigationView;
    private TranslationsManager translationsManager = TranslationsManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPager = (CustomViewPager) findViewById(R.id.pager);
        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        // Предотвращает удаление загруженных фрагментов во ViewPager, что целесообразно, тк
        // имеются всего 3 фрагмента.
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new TranslationFragment();
                    case 1:
                        return HistoryFragment.newInstance(false);
                    case 2:
                        return HistoryFragment.newInstance(true);
                    default:
                        return new Fragment();
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                hideKeyboard();
                Fragment fragment = (Fragment) mPager.getAdapter().instantiateItem(mPager, position);
                if (fragment instanceof HistoryFragment) {
                    if (fragment.getView() != null) {
                        EditText editText = ((EditText) fragment.getView().findViewById(R.id.editText));
                        editText.setText("");
                    }
                    ((HistoryFragment) fragment).refreshContainer(translationsManager.getTranslations());
                } else ((TranslationFragment) fragment).refreshIconsVisibility();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        navigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {

                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.navigation_translator:
                                mPager.setCurrentItem(0);
                                return true;
                            case R.id.navigation_history:
                                mPager.setCurrentItem(1);
                                return true;
                            case R.id.navigation_favorites:
                                mPager.setCurrentItem(2);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
    }

    public void hideKeyboard() {
        View focusView = getCurrentFocus();
        if (focusView != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            focusView.clearFocus();
        }
    }

    // Вызывает popBackStack() для child fragments, чего не происходит по умолчанию при физическом
    // нажатии кнопки назад.
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment frag : fm.getFragments()) {
            if (frag.isVisible()) {
                FragmentManager childFm = frag.getChildFragmentManager();
                if (childFm.getBackStackEntryCount() > 0) {
                    childFm.popBackStack();
                    return;
                }
            }
        }
        super.onBackPressed();
    }
}
