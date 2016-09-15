package com.kaltura.playersdk.casting;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.players.KChromeCastPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by Gleb on 9/13/16.
 */
public class KCastProviderV3Impl implements KCastProvider {
    private static final String TAG = "KCastProviderImpl";
    public static  String nameSpace = "urn:x-cast:com.kaltura.cast.player";
    private SessionManager mSessionManager;
    private CastSession mCastSession;
    private CastContext mCastContext;
    private KCastProviderListener mProviderListener;
    private KCastMediaRemoteControl mCastMediaRemoteControl;
    private KCastKalturaChannel mChannel;
    private KCastInternalListener mInternalListener;
    private Context mContext;
    private String mCastAppId;
    private boolean mApplicationStarted;
    private boolean isReconnedted  = true;

    public KCastProviderV3Impl(Context context, String castAppId) {
        mContext = context;
        mCastContext = CastContext.getSharedInstance(context);
        //mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mSessionManager  = mCastContext.getSessionManager();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
        mCastSession = mSessionManager.getCurrentCastSession();
        mCastAppId = castAppId;
    }

    public void addCastStateListener(CastStateListener castStateListener) {
        mCastContext.addCastStateListener(castStateListener);
    }

    public void removeCastStateListener(CastStateListener castStateListener) {
        mCastContext.removeCastStateListener(castStateListener);
    }

    public void setInternalListener(KCastInternalListener internalListener) {
        mInternalListener = internalListener;
    }

    private SessionManagerListener mSessionManagerListener = new SessionManagerListener() {
        @Override
        public void onSessionStarting(Session session) {
            LOGD(TAG, "SessionManagerListener onSessionStarting");
        }

        @Override
        public void onSessionStarted(Session session, String s) {
            LOGD(TAG, "SessionManagerListener onSessionStarted");
            isReconnedted = false;
            startReceiver(mContext);
        }

        @Override
        public void onSessionStartFailed(Session session, int i) {
            LOGD(TAG, "SessionManagerListener onSessionStartFailed");
        }

        @Override
        public void onSessionEnding(Session session) {
            LOGD(TAG, "SessionManagerListener onSessionEnding");
            disconnectFromCastDevice();
        }

        @Override
        public void onSessionEnded(Session session, int i) {
            LOGD(TAG, "SessionManagerListener onSessionEnded");
            disconnectFromCastDevice();
        }

        @Override
        public void onSessionResuming(Session session, String s) {
            LOGD(TAG, "SessionManagerListener onSessionResuming");
        }

        @Override
        public void onSessionResumed(Session session, boolean b) {
            LOGD(TAG, "SessionManagerListener onSessionResumed");
        }

        @Override
        public void onSessionResumeFailed(Session session, int i) {
            LOGD(TAG, "SessionManagerListener onSessionResumeFailed");
            disconnectFromCastDevice();
        }

        @Override
        public void onSessionSuspended(Session session, int i) {
            LOGD(TAG, "SessionManagerListener onSessionSuspended POS = " + mCastSession.getRemoteMediaClient().getApproximateStreamPosition());

        }
    };

    public KCastProviderListener getProviderListener() {
        return mProviderListener;
    }

    @Override
    public void startReceiver(Context context, boolean guestModeEnabled) {
        mCastSession = mSessionManager.getCurrentCastSession();
        mChannel = new KCastKalturaChannel(nameSpace, new KCastKalturaChannel.KCastKalturaChannelListener() {
            @Override
            public void readyForMedia(final String[] params) {
                if (!isRecconected()) {
                    sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                }
                // Receiver send the new content
                if (params != null) {
                    mCastMediaRemoteControl = new KChromeCastPlayer(mCastSession);
                    ((KChromeCastPlayer) mCastMediaRemoteControl).setMediaInfoParams(params);
                    if (mInternalListener != null) {
                        mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
                    }
                }
            }

            @Override
            public void textTeacksRecived(HashMap<String, Integer> textTrackHash) {
                getCastMediaRemoteControl().setTextTracks(textTrackHash);
            }

            @Override
            public void videoTracksReceived(List<Integer> videoTracksList) {
                getCastMediaRemoteControl().setVideoTracks(videoTracksList);
            }
        });

        if (mCastSession != null) {
            try {
                mCastSession.setMessageReceivedCallbacks(nameSpace, mChannel);
            } catch (IOException e) {
                LOGE(TAG, "Exception while creating channel", e);
            }
            if (!isRecconected()) {
                mCastSession.sendMessage(nameSpace, "{\"type\":\"show\",\"target\":\"logo\"}");
            }
            mApplicationStarted = true;
        }
    }

    @Override
    public void startReceiver(Context context) {
        startReceiver(context, false);
    }

    @Override
    public void disconnectFromCastDevice() {
        if (mInternalListener != null) {
            //mInternalListener.onCastStateChanged("hideConnectingMessage");
            mInternalListener.onCastStateChanged("chromecastDeviceDisConnected");
            mInternalListener.onStopCasting();
        }
        teardown();
    }

    @Override
    public KCastDevice getSelectedCastDevice() {
        if (mCastSession != null) {
            return new KCastDevice(mCastSession.getCastDevice());
        }
        return null;
    }

    @Override
    public void setKCastProviderListener(KCastProviderListener listener) {
        mProviderListener = listener;
    }

    @Override
    public KCastMediaRemoteControl getCastMediaRemoteControl() {
        return mCastMediaRemoteControl;
    }

    @Override
    public boolean isRecconected() {
        return isReconnedted;
    }

    @Override
    public boolean isConnected() {
        if (mCastSession != null) {
            return mCastSession.isConnected();
        }
        return false;
    }

    @Override
    public boolean isCasting() {
        if (mCastSession != null) {
            return mCastSession.getRemoteMediaClient().hasMediaSession();
        }
        return false;
    }

    public void sendMessage(final String message) {
        if (mCastSession != null) {
            mCastSession.sendMessage(nameSpace, message).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(@NonNull Status status) {
                    LOGD(TAG, "Message Sent OK: namespace:" + nameSpace + " message:" + message);
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    LOGE(TAG, "Sending message failed");
                }
            });
        }
    }

    public CastSession getCastSession() {
        return mCastSession;
    }

    private void teardown() {
        if (mCastSession != null) {
            LOGD(TAG, "START TEARDOWN mApiClient.isConnected() = " + mCastSession.isConnected() + " mApiClient.isConnecting() = " + mCastSession.isConnecting()) ;
            if (mCastSession != null) {
                if (mCastSession.getRemoteMediaClient() != null) {
                    mCastSession.getRemoteMediaClient().removeListener(null);
                    try {
                        mCastSession.removeMessageReceivedCallbacks(nameSpace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mApplicationStarted) {
                boolean isConnected = mCastSession.isConnected();
                boolean isConnecting = mCastSession.isConnecting();
                if (isConnected || isConnecting) {
                    try {
                        if (isConnected) {
                            mCastSession.getRemoteMediaClient().stop();
                        }
                        if (mChannel != null) {
                            mCastSession.removeMessageReceivedCallbacks(nameSpace);
                            mChannel = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mApplicationStarted = false;
            }
            mCastSession = null;
        }
    }
}