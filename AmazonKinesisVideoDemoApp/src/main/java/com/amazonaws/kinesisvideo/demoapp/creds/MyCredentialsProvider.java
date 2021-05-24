package com.amazonaws.kinesisvideo.demoapp.creds;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.UserStateListener;
import com.amazonaws.mobile.config.AWSConfiguration;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MyCredentialsProvider extends CognitoCachingCredentialsProvider {

    private static final String TAG = MyCredentialsProvider.class.getSimpleName();

    private String cognitoLoginKey;
    private String currentCognitoIdToken;

    public MyCredentialsProvider(Context context, AWSConfiguration awsConfiguration) {
        super(context, awsConfiguration);

        if (awsConfiguration.optJsonObject("CognitoUserPool") != null) {
            try {
                JSONObject identityPoolJSON = awsConfiguration.optJsonObject("CognitoUserPool");
                String userpoolId = identityPoolJSON.getString("PoolId");
                String region = identityPoolJSON.getString("Region");
                cognitoLoginKey = "cognito-idp." + region + ".amazonaws.com/" + userpoolId;
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Cognito Identity; please check your awsconfiguration.json");
            }
        }

        AWSMobileClient.getInstance().addUserStateListener(new UserStateListener() {
            @Override
            public void onUserStateChanged(UserStateDetails userStateDetails) {
                switch (userStateDetails.getUserState()){
                    case GUEST:
                        Log.i(TAG, "user is in guest mode");
                        break;
                    case SIGNED_OUT:
                        Log.i(TAG, "user is signed out");
                        break;
                    case SIGNED_IN:
                        Log.i(TAG, "user is signed in");
                        setupLogins();
                        break;
                    case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                        Log.i(TAG, "need to login again");
                        break;
                    case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                        Log.i(TAG, "user logged in via federation, but currently needs new tokens");
                        break;
                    default:
                        Log.e(TAG, "unsupported");
                }
            }
        });
    }

    private void setupLogins() {
        try {
            String cognitoIdToken = AWSMobileClient.getInstance()
                .getTokens().getIdToken().getTokenString();
            if (!cognitoIdToken.equals(currentCognitoIdToken)) {
                Map<String, String> logins = new HashMap<>();
                logins.put(cognitoLoginKey, cognitoIdToken);
                setLogins(logins);
                currentCognitoIdToken = cognitoIdToken;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get id token from AWSMobileClient.");
        }
    }

    @Override
    public void refresh() {
        AWSMobileClient.getInstance().currentUserState();
        setupLogins();
        super.refresh();
    }
}
