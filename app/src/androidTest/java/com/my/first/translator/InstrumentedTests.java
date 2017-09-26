package com.my.first.translator;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.my.first.translator.classes.Translation;
import com.my.first.translator.classes.TranslationsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InstrumentedTests {

    private Context context;
    private TranslationsManager translationsManager;

    @Before
    public void init() {
        context = InstrumentationRegistry.getTargetContext();
        translationsManager = TranslationsManager.getInstance();
    }

    @Test
    public void checkTranslate() {
        translationsManager.loadData(context, new TranslationsManager.TranslationListener() {
            @Override
            public void onFinished(Translation translation, String newSourceLanguage) {
                translationsManager.translate("проверка", "Russian", "English",
                        new TranslationsManager.TranslationListener() {
                            @Override
                            public void onFinished(Translation translation, String newSourceLanguage) {
                                assertTrue(translation.getLang().equals("ru-en"));
                                assertTrue(translation.getSimpleTranslation().contains("check"));
                            }
                        }, context);
                translationsManager.translate("language", context.getString(R.string.auto_detect),
                        "Ukrainian", new TranslationsManager.TranslationListener() {
                            @Override
                            public void onFinished(Translation translation, String newSourceLanguage) {
                                assertEquals(newSourceLanguage, "en");
                                assertTrue(translation.getLang().equals("en-ua"));
                                assertTrue(translation.getSimpleTranslation().contains("мова"));
                            }
                        }, context);
                translationsManager.translate("много много слов", context.getString(R.string.auto_detect), "English",
                        new TranslationsManager.TranslationListener() {
                            @Override
                            public void onFinished(Translation translation, String newSourceLanguage) {
                                assertEquals(newSourceLanguage, "ru");
                                assertTrue(translation.getLang().equals("ru-en"));
                                assertTrue(translation.getSimpleTranslation().equals("many many words"));
                            }
                        }, context);
            }
        });
    }

    @Test
    public void checkWorkWithDB() {
        translationsManager.loadData(context, new TranslationsManager.TranslationListener() {
            @Override
            public void onFinished(Translation translation, String newSourceLanguage) {
                translationsManager.translate("проверка", "Russian", "English",
                        new TranslationsManager.TranslationListener() {
                            @Override
                            public void onFinished(Translation translation, String newSourceLanguage) {
                                assertTrue(translationsManager.getTranslations().size() == 1);
                                translationsManager.changeFavorite(translation, context);
                                assertTrue(translation.isFavorite());
                                translationsManager.refreshTranslations(context);
                                assertTrue(translationsManager.getTranslations().get(0).isFavorite());
                                translationsManager.resetFavorites(context);
                                assertTrue(!translationsManager.getTranslations().get(0).isFavorite());
                                translationsManager.refreshTranslations(context);
                                assertTrue(!translationsManager.getTranslations().get(0).isFavorite());
                                assertTrue(translationsManager.getTranslations().size() == 1);
                                translationsManager.translate("проверка", "Russian", "English",
                                        new TranslationsManager.TranslationListener() {
                                            @Override
                                            public void onFinished(Translation translation, String newSourceLanguage) {
                                                assertTrue(translationsManager.getTranslations().size() == 1);
                                                translationsManager.refreshTranslations(context);
                                                assertTrue(translationsManager.getTranslations().size() == 1);
                                                translationsManager.translate("second check", context.getString(R.string.auto_detect),
                                                        "Arabic", new TranslationsManager.TranslationListener() {
                                                            @Override
                                                            public void onFinished(Translation translation, String newSourceLanguage) {
                                                                assertTrue(translationsManager.getTranslations().size() == 2);
                                                                translationsManager.refreshTranslations(context);
                                                                assertTrue(translationsManager.getTranslations().size() == 2);
                                                                translationsManager.deleteTranslation(translation, context);
                                                                assertTrue(translationsManager.getTranslations().size() == 1);
                                                                translationsManager.refreshTranslations(context);
                                                                assertTrue(translationsManager.getTranslations().size() == 1);
                                                                translationsManager.deleteAll(context);
                                                                assertTrue(translationsManager.getTranslations().size() == 0);
                                                                translationsManager.refreshTranslations(context);
                                                                assertTrue(translationsManager.getTranslations().size() == 0);
                                                            }
                                                        }, context);
                                            }
                                        }, context);
                            }
                        }, context);
            }
        });
    }
}
