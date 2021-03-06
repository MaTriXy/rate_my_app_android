package com.zendesk.ratemyapp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * This is a {@link DialogFragment DialogFragment} that will show a "Rate
 * My App" dialog, which steers positive reviews to the provided Play Store listing, and deflects
 * negative reviews to the feedback mechanism of your choice. It was written with the
 * Zendesk Support SDK in mind, but it does not actually gather or send feedback anywhere. It delivers
 * a call back to the provided {@link DialogActionListener}, which is then responsible for doing
 * something with it.
 *
 * As such, RateMyAppDialog is not dependent on Zendesk and can be used to direct feedback wherever
 * is desired.
 * <p>
 *     A full example is available in the rate_my_app_sample app, which
 *     shows how to use the dialog to create tickets with Zendesk, and how to use the dialog to
 *     send feedback via a {@link Intent#ACTION_SEND} Intent.
 * </p>
 *
 */
@SuppressWarnings("unused")
public class RateMyAppDialog extends DialogFragment {

    /**
     * This is the name of the tag that will be set on the
     * RateMyAppDialog that will be created by this button.
     */
    public static final String RMA_DIALOG_TAG = "rma_dialog";

    private static final String LOG_TAG = RateMyAppDialog.class.getSimpleName();

    static final String PREFS_FILE = "rateMyApp";
    static final String PREFS_DONT_ASK_VERSION_KEY = "appVersion";

    private DialogActionListener dialogActionListener;

    private RateMyAppConfig config;

    /**
     * Shows the dialog if {@link RateMyAppConfig#canShow()} returns true.
     *
     * This implementation shows a dialog with three buttons. Each button calls a corresponding
     * method in the provided {@link DialogActionListener}, which can be overridden by the client to
     * customise behaviour as desired.
     *
     * The methods called by the buttons are the package-private
     * {@link DialogActionListener#storeButtonClicked(DialogFragment, RateMyAppConfig)},
     * {@link DialogActionListener#feedbackButtonClicked(DialogFragment, RateMyAppConfig)}, and
     * {@link DialogActionListener#dontAskAgainClicked(DialogFragment, RateMyAppConfig)} methods,
     *  rather than the public
     *  {@link DialogActionListener#onStoreButtonClicked(DialogFragment, RateMyAppConfig)},
     *  {@link DialogActionListener#onFeedbackButtonClicked(DialogFragment, RateMyAppConfig)} and
     *  {@link DialogActionListener#onDontAskAgainClicked(DialogFragment, RateMyAppConfig)}.
     *
     *  In {@link DialogFragment#onDetach()}, the {@link DialogActionListener} is set to {@code null}
     *  and the {@link DialogFragment#dismiss()} is called.
     *
     * @param activity the {@link AppCompatActivity} on which to show the {@link DialogFragment}.
     * @param config the {@link RateMyAppConfig} which will be used to configure the dialog
     * @param actionListener the {@link DialogActionListener} to use as a callback object for user
     *                       actions on the dialog
     */
    public static void show(@NonNull AppCompatActivity activity, @NonNull RateMyAppConfig config,
                            @NonNull DialogActionListener actionListener) {
        if (!config.canShow()) {
            Log.d(RMA_DIALOG_TAG, "Can't show RateMyAppDialog due to configured rules.");
            return;
        }

        showAlways(activity, config, actionListener);
    }

    /**
     * Shows the dialog without checking the value of {@link RateMyAppConfig#canShow()}.
     *
     * This implementation shows a dialog with three buttons. Each button calls a corresponding
     * method in the provided {@link DialogActionListener}, which can be overridden by the client to
     * customise behaviour as desired.
     *
     * The methods called by the buttons are the package-private
     * {@link DialogActionListener#storeButtonClicked(DialogFragment, RateMyAppConfig)},
     * {@link DialogActionListener#feedbackButtonClicked(DialogFragment, RateMyAppConfig)}, and
     * {@link DialogActionListener#dontAskAgainClicked(DialogFragment, RateMyAppConfig)} methods,
     *  rather than the public
     *  {@link DialogActionListener#onStoreButtonClicked(DialogFragment, RateMyAppConfig)},
     *  {@link DialogActionListener#onFeedbackButtonClicked(DialogFragment, RateMyAppConfig)} and
     *  {@link DialogActionListener#onDontAskAgainClicked(DialogFragment, RateMyAppConfig)}.
     *
     *  In {@link DialogFragment#onDetach()}, the {@link DialogActionListener} is set to {@code null}
     *  and the {@link DialogFragment#dismiss()} is called.
     *
     * @param activity the {@link AppCompatActivity} on which to show the {@link DialogFragment}.
     * @param config the {@link RateMyAppConfig} which will be used to configure the dialog
     * @param actionListener the {@link DialogActionListener} to use as a callback object for user
     *                       actions on the dialog
     */
    public static void showAlways(@NonNull AppCompatActivity activity, @NonNull RateMyAppConfig config,
                                  @NonNull DialogActionListener actionListener) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        RateMyAppDialog fragment;
        if (fragmentManager.findFragmentByTag(RMA_DIALOG_TAG) == null) {
            fragment = new RateMyAppDialog();
            fragment.config = config;
        } else {
            fragment = (RateMyAppDialog) fragmentManager.findFragmentByTag(RMA_DIALOG_TAG);
        }
        fragment.dialogActionListener = actionListener;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);

        fragment.show(transaction, RMA_DIALOG_TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        setStyle(STYLE_NO_TITLE, getTheme());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup dialogViewGroup = (ViewGroup) inflater.inflate(R.layout.rma_dialog, container, false);

        addStoreButton(dialogViewGroup, inflater);
        addFeedbackButton(dialogViewGroup, inflater);
        addDontRemindMeAgainButton(dialogViewGroup, inflater);

        return dialogViewGroup;
    }

    private void addStoreButton(ViewGroup viewGroup, LayoutInflater inflater) {
        addDivider(inflater, viewGroup);

        TextView buttonTextView = (TextView) inflater.inflate(R.layout.rma_button, viewGroup, false);
        buttonTextView.setText(R.string.rate_my_app_dialog_positive_action_label);
        buttonTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogActionListener != null) {
                    dialogActionListener.storeButtonClicked(RateMyAppDialog.this, config);
                }
            }
        });
        viewGroup.addView(buttonTextView);
    }

    private void addFeedbackButton(ViewGroup viewGroup, LayoutInflater inflater) {
        addDivider(inflater, viewGroup);

        TextView buttonTextView = (TextView) inflater.inflate(R.layout.rma_button, viewGroup, false);
        buttonTextView.setText(R.string.rate_my_app_dialog_negative_action_label);
        buttonTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogActionListener != null) {
                    dialogActionListener.feedbackButtonClicked(RateMyAppDialog.this, config);
                }
            }
        });

        viewGroup.addView(buttonTextView);
    }

    private void addDontRemindMeAgainButton(ViewGroup viewGroup, LayoutInflater inflater) {
        addDivider(inflater, viewGroup);

        TextView buttonTextView = (TextView) inflater.inflate(R.layout.rma_button, viewGroup, false);
        buttonTextView.setText(R.string.rate_my_app_dialog_dismiss_action_label);
        buttonTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogActionListener != null) {
                    dialogActionListener.dontAskAgainClicked(RateMyAppDialog.this, config);
                }
            }
        });

        viewGroup.addView(buttonTextView);
    }

    private void storeVersion() {
        if (getActivity() != null && config.getAppVersion() != null) {
            SharedPreferences.Editor sharedPrefsEditor = getActivity()
                    .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit();
            sharedPrefsEditor.putString(PREFS_DONT_ASK_VERSION_KEY, config.getAppVersion());
            sharedPrefsEditor.apply();
        }
    }

    private void addDivider(LayoutInflater inflater, ViewGroup container) {
        View divider = inflater.inflate(R.layout.rma_divider, container, false);
        container.addView(divider);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        dialogActionListener = null;
        dismiss();
    }

    /**
     * This is a hacky workaround as per https://code.google.com/p/android/issues/detail?id=17423
     * to retain the Fragment on configuration change.
     */
    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
