package com.mymensor;


import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

public class MymAccAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = "MymAccAuthenticator";
    private final Context mContext;

    public MymAccAuthenticator(Context context)
    {
        super(context);
        this.mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "Called-addAccount");
        final Intent intent = new Intent(mContext, MymAccAuthenticatorActivity.class);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        if (options != null) {
            bundle.putAll(options);
        }
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "Called-confirmCred");
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account,
                               String authTokenType,
                               Bundle options) throws NetworkErrorException {

        final Bundle result = new Bundle();

        Log.d("MymAccAuthenticator:", "Called-getAuthTokn");

        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        Log.d(TAG,"peekAuthToken returned:"+ authToken);

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        } else {
            // If we get here, then we couldn't access the user's password - so we
            // need to re-prompt them for their credentials. We do that by creating
            // an intent to display our AuthenticatorActivity.
            final Intent intent = new Intent(mContext, MymAccAuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(MymAccAuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(MymAccAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(MymAccAuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.d(TAG, "Called-getAuthLbl");
        if (Constants.AUTHTOKEN_TYPE_FULL_ACCESS.equals(authTokenType))
            return Constants.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;
        else if (Constants.AUTHTOKEN_TYPE_READ_ONLY.equals(authTokenType))
            return Constants.AUTHTOKEN_TYPE_READ_ONLY_LABEL;
        else
            return authTokenType + " (Label)";
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "Called-updateCred");
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Log.d(TAG, "Called-hasFeatures");
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }
}



