package com.devnull.radio.players.mediaplayer;

import com.devnull.radio.station.live.StreamLiveInfo;
import com.devnull.radio.station.live.ShoutcastInfo;

interface StreamProxyListener {
    void onFoundShoutcastStream(ShoutcastInfo bitrate, boolean isHls);
    void onFoundLiveStreamInfo(StreamLiveInfo liveInfo);
    void onStreamCreated(String proxyConnection);
    void onStreamStopped();
    void onBytesRead(byte[] buffer, int offset, int length);
}
