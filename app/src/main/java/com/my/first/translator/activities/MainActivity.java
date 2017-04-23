package com.my.first.translator.activities;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.my.first.translator.R;
import com.my.first.translator.classes.CustomViewPager;
import com.my.first.translator.classes.Translation;
import com.my.first.translator.databases.TranslationsDataBase;
import com.my.first.translator.fragments.HistoryFragment;
import com.my.first.translator.fragments.TranslationFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public CustomViewPager mPager;
    public BottomNavigationView navigationView;
    // Переводы загружаются из базы данных при запуске приложения. В дальшейшем, для оптимизации
    // скорости работы, всё взамодействие с предыдущими переводами производится через сформированный
    // список истории переводов, который в дальнейшем обновляется при необходимости.
    public ArrayList<Translation> allTranslations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null)
            allTranslations = savedInstanceState.getParcelableArrayList("allTranslations");
        else allTranslations = TranslationsDataBase.getTranslationsFromDataBase(this);
        mPager = (CustomViewPager) findViewById(R.id.pager);
        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
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
                Fragment fragment = (Fragment) mPager.getAdapter().instantiateItem(mPager, position);
                if (fragment instanceof HistoryFragment) {
                    if (fragment.getView() != null) {
                        EditText editText = ((EditText) fragment.getView().findViewById(R.id.editText));
                        editText.setText("");
                        editText.setCursorVisible(false);
                    }
                    ((HistoryFragment) fragment).refreshContainer(allTranslations);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("allTranslations", allTranslations);
    }

    public static void buttonEffect(View button) {
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xe0f47521, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }
}
