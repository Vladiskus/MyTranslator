package com.my.first.translator.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Translation implements Parcelable {

    // Текст оригинала.
    private String text;
    // Текст перевода, представленный на экране истории переводов.
    private String simpleTranslation;
    // Текст перевода, представленный на основном экране.
    private String fullTranslation;
    // Направление перевода.
    private String lang;
    // Доабавлен ли перевод в избранные.
    private boolean isFavorite;

    public Translation(){}

    public Translation(String text, String simpleTranslation, String fullTranslation,
                       String lang, boolean isFavorite) {
        this.text = text;
        this.simpleTranslation = simpleTranslation;
        this.fullTranslation = fullTranslation;
        this.lang = lang;
        this.isFavorite = isFavorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {text, simpleTranslation, fullTranslation, lang,
                String.valueOf(isFavorite)});
    }

    private Translation(Parcel in){
        String[] data = new String[5];
        in.readStringArray(data);
        text = data[0];
        simpleTranslation = data[1];
        fullTranslation = data[2];
        lang = data[3];
        isFavorite = Boolean.getBoolean(data[4]);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        public Translation createFromParcel(Parcel in) {
            return new Translation(in);
        }

        public Translation[] newArray(int size) {
            return new Translation[size];
        }
    };

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSimpleTranslation() {
        return simpleTranslation;
    }

    public void setSimpleTranslation(String simpleTranslation) {
        this.simpleTranslation = simpleTranslation;
    }

    public String getFullTranslation() {
        return fullTranslation;
    }

    public void setFullTranslation(String fullTranslation) {
        this.fullTranslation = fullTranslation;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Translation that = (Translation) o;
        if (!text.equals(that.text)) return false;
        return lang.equals(that.lang);

    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + lang.hashCode();
        return result;
    }
}
