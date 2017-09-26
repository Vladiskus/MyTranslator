package com.my.first.translator.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.my.first.translator.R;
import com.my.first.translator.classes.TranslationsManager;

// Диалог, созданный с целью уточнить, точно ли пользователь хочет удалить все переводы из списка.
public class DeleteDialogFragment extends DialogFragment {

    public static DeleteDialogFragment newInstance(boolean isFavorites) {
        DeleteDialogFragment fragment = new DeleteDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("isFavorites", isFavorites);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean isFavorites = getArguments().getBoolean("isFavorites");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(String.format(getString(R.string.are_you_sure),
                getString(isFavorites ? R.string.from_favorites : R.string.from_history)))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (isFavorites) TranslationsManager.getInstance().resetFavorites(getActivity());
                        else TranslationsManager.getInstance().deleteAll(getActivity());
                        ((HistoryFragment) DeleteDialogFragment.this.getParentFragment())
                                .refreshContainer(TranslationsManager.getInstance().getTranslations());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }
}