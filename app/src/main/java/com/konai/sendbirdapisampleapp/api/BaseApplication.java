package com.konai.sendbirdapisampleapp.api;

import static com.konai.sendbirdapisampleapp.Constants.APP_ID;
import static com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME;
import static com.konai.sendbirdapisampleapp.Constants.USER_ID;
import static com.konai.sendbirdapisampleapp.Constants.TAG;
import android.app.Application;
import android.util.Log;

import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.handler.InitResultHandler;
import com.sendbird.uikit.SendbirdUIKit;
import com.sendbird.uikit.adapter.SendbirdUIKitAdapter;
import com.sendbird.uikit.interfaces.UserInfo;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SendbirdUIKit.init(
                new SendbirdUIKitAdapter() {
                    @Override
                    public String getAppId() {
                        return APP_ID; // Specify your Sendbird application ID.
                    }

                    @Override
                    public String getAccessToken() {
                        return "";
                    }

                    @Override
                    public UserInfo getUserInfo() {
                        return new UserInfo() {
                            @Override
                            public String getUserId() {
                                return USER_ID;  // Specify your user ID.
                            }

                            @Override
                            public String getNickname() {
                                return USER_NICKNAME;  // Specify your user nickname.
                            }

                            @Override
                            public String getProfileUrl() {
                                return "";
                            }
                        };
                    }

                    @Override
                    public InitResultHandler getInitResultHandler() {
                        return new InitResultHandler() {
                            @Override
                            public void onMigrationStarted() {
                                Log.d(TAG, "onMigrationStarted: ");
                                // DB migration has started.
                            }

                            @Override
                            public void onInitFailed(SendbirdException e) {
                                Log.d(TAG, "onInitFailed: ");
                                // If DB migration fails, this method is called.
                            }

                            @Override
                            public void onInitSucceed() {
                                Log.d(TAG, "onInitSucceed: ");
                                // If DB migration is successful, this method is called and you can proceed to the next step.
                                // In the sample app, the `LiveData` class notifies you on the initialization progress
                                // And observes the `MutableLiveData<InitState> initState` value in `SplashActivity()`.
                                // If successful, the `LoginActivity` screen
                                // Or the `HomeActivity` screen will show.
                            }
                        };
                    }
                },
                this
        );
    }
}
