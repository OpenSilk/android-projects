package org.opensilk.traveltime.data;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.firebase.iid.FirebaseInstanceId;

import org.opensilk.dagger2.ForApp;

import java.util.Arrays;

import javax.inject.Inject;

/**
 * AuthHelper hides auth implementation
 *
 * Created by drew on 10/30/17.
 */

public interface AuthHelper {

    String[] SCOPES = {
            CalendarScopes.CALENDAR,
    };

    /**
     * For now we will just use primary calendar
     */
    String CALENDAR_ID = "primary";

    /**
     * @return true if account has been previously selected
     */
    boolean isAccountSelected();

    /**
     * Sets the selected account
     * @param name
     */
    void selectAccount(String name);

    /**
     * @return an intent for the account chooser
     */
    Intent getAccountPickerIntent();

    /**
     * isAccountSelected() must return true before using this
     *
     * @return the setup calendar api for selected account
     */
    Calendar.Events calendarEvents();

    Calendar calendar();

    String getFirebaseToken();

    String getAccountId();

    class Default implements AuthHelper {

        private final Context context;
        private final GoogleAccountCredential credential;
        private final Settings settings;

        @Inject
        public Default(
                @ForApp Context context,
                Settings settings
        ) {
            this.context = context;
            this.credential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPES));
            this.settings = settings;
            init();
        }

        private void init() {
            String user = settings.getSelectedAccount();
            if (!TextUtils.isEmpty(user)){
                credential.setSelectedAccountName(user);
            }
        }

        @Override
        public boolean isAccountSelected() {
            return credential.getSelectedAccountName() != null;
        }

        @Override
        public void selectAccount(String name) {
            settings.setSelectedAccount(name);
            credential.setSelectedAccountName(name);
        }

        @Override
        public Intent getAccountPickerIntent() {
            return credential.newChooseAccountIntent();
        }

        @Override
        public Calendar.Events calendarEvents() {
            return calendar().events();
        }

        @Override
        public Calendar calendar() {
            return new Calendar.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(context.getPackageName())
                    .build();
        }

        @Override
        public String getFirebaseToken() {
            return FirebaseInstanceId.getInstance().getToken();
        }

        @Override
        public String getAccountId() {
            //just using the name, even though its not right. it should work though
            return credential.getSelectedAccount().name;
        }
    }
}
