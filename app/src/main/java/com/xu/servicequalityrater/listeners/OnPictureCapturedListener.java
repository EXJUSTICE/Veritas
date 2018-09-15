package com.xu.servicequalityrater.listeners;

/**
 * Created by Omistaja on 14/04/2017.
 */


        import java.util.TreeMap;

public interface OnPictureCapturedListener {
    void onCaptureDone(String str, byte[] bArr);

    void onDoneCapturingAllPhotos(TreeMap<String, byte[]> treeMap);
}
