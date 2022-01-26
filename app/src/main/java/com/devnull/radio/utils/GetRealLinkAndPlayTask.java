package com.devnull.radio.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.RemoteException;

import com.devnull.radio.IPlayerService;
import com.devnull.radio.station.DataRadioStation;
import com.devnull.radio.RadioDroidApp;
import com.devnull.radio.Utils;

import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;

public class GetRealLinkAndPlayTask extends AsyncTask<Void, Void, String> {
    private WeakReference<Context> contextRef;
    private DataRadioStation station;
    private WeakReference<IPlayerService> playerServiceRef;

    private OkHttpClient httpClient;

    public GetRealLinkAndPlayTask(Context context, DataRadioStation station, IPlayerService playerService) {
        this.contextRef = new WeakReference<>(context);
        this.station = station;
        this.playerServiceRef = new WeakReference<>(playerService);

        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        httpClient = radioDroidApp.getHttpClient();
    }

    @Override
    protected String doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context != null) {
            return Utils.getRealStationLink(httpClient, context.getApplicationContext(), station.StationUuid);
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        IPlayerService playerService = playerServiceRef.get();
        if (result != null && playerService != null && !isCancelled()) {
            try {
                station.playableUrl = result;
                playerService.SetStation(station);
                playerService.Play(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onPostExecute(result);
    }
}
