package com.devnull.radio;

import android.app.UiModeManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.devnull.radio.alarm.RadioAlarmManager;
import com.devnull.radio.history.TrackHistoryRepository;
import com.devnull.radio.players.mpd.MPDClient;
import com.devnull.radio.proxy.ProxySettings;
import com.devnull.radio.recording.RecordingsManager;
import com.devnull.radio.station.live.metadata.TrackMetadataSearcher;
import com.devnull.radio.utils.TvChannelManager;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import com.devnull.radio.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RadioDroidApp extends MultiDexApplication {

    private HistoryManager historyManager;
    private FavouriteManager favouriteManager;
    private RecordingsManager recordingsManager;
    private RadioAlarmManager alarmManager;
    private TvChannelManager tvChannelManager;

    private TrackHistoryRepository trackHistoryRepository;

    private MPDClient mpdClient;

    private CastHandler castHandler;

    private TrackMetadataSearcher trackMetadataSearcher;

    private ConnectionPool connectionPool;
    private OkHttpClient httpClient;

    private Interceptor testsInterceptor;

    public class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        GoogleProviderHelper.use(getBaseContext());

        connectionPool = new ConnectionPool();

        rebuildHttpClient();

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(newHttpClientForPicasso()));
        Picasso picassoInstance = builder.build();
        Picasso.setSingletonInstance(picassoInstance);

        CountryCodeDictionary.getInstance().load(this);
        CountryFlagsLoader.getInstance();

        historyManager = new HistoryManager(this);
        favouriteManager = new FavouriteManager(this);
        recordingsManager = new RecordingsManager();
        alarmManager = new RadioAlarmManager(this);

        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            tvChannelManager = new TvChannelManager(this);
            favouriteManager.addObserver(tvChannelManager);
        }

        trackHistoryRepository = new TrackHistoryRepository(this);

        mpdClient = new MPDClient(this);

        castHandler = new CastHandler();

        trackMetadataSearcher = new TrackMetadataSearcher(httpClient);

        recordingsManager.updateRecordingsList();
    }

    public void setTestsInterceptor(Interceptor testsInterceptor) {
        this.testsInterceptor = testsInterceptor;
    }

    public void rebuildHttpClient() {
        OkHttpClient.Builder builder = newHttpClient()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(new UserAgentInterceptor("KRR/" + BuildConfig.VERSION_NAME));

        httpClient = builder.build();
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public FavouriteManager getFavouriteManager() {
        return favouriteManager;
    }

    public RecordingsManager getRecordingsManager() {
        return recordingsManager;
    }

    public RadioAlarmManager getAlarmManager() {
        return alarmManager;
    }

    public TrackHistoryRepository getTrackHistoryRepository() {
        return trackHistoryRepository;
    }

    public MPDClient getMpdClient() {
        return mpdClient;
    }

    public CastHandler getCastHandler() {
        return castHandler;
    }

    public TrackMetadataSearcher getTrackMetadataSearcher() {
        return trackMetadataSearcher;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public OkHttpClient.Builder newHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectionPool(connectionPool);

        if (testsInterceptor != null) {
            builder.addInterceptor(testsInterceptor);
        }

        if (!setCurrentOkHttpProxy(builder)) {
            Toast toast = Toast.makeText(this, getResources().getString(R.string.ignore_proxy_settings_invalid), Toast.LENGTH_SHORT);
            toast.show();
        }
        return Utils.enableTls12OnPreLollipop(builder);
    }

    public OkHttpClient.Builder newHttpClientWithoutProxy() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectionPool(connectionPool);

        if (testsInterceptor != null) {
            builder.addInterceptor(testsInterceptor);
        }

        return Utils.enableTls12OnPreLollipop(builder);
    }

    public boolean setCurrentOkHttpProxy(@NonNull OkHttpClient.Builder builder) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        ProxySettings proxySettings = ProxySettings.fromPreferences(sharedPref);
        if (proxySettings != null) {
            if (!Utils.setOkHttpProxy(builder, proxySettings)) {
                // proxy settings are not valid
                return false;
            }
        }
        return true;
    }

    private OkHttpClient newHttpClientForPicasso() {
        File cache = new File(getCacheDir(), "picasso-cache");
        if (!cache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new UserAgentInterceptor("KRR/" + BuildConfig.VERSION_NAME))
                .cache(new Cache(cache, Integer.MAX_VALUE));

        if (testsInterceptor != null) {
            builder.addInterceptor(testsInterceptor);
        }

        setCurrentOkHttpProxy(builder);

        return builder.build();
    }
}
