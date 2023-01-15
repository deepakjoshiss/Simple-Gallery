package com.simplemobiletools.gallery.pro.aes;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

public class AESencryptionTask  extends AsyncTask<Void, Void, Void> {

    private Uri mUri;
    private File mFile;
    private Cipher mCipher;
    private Context mContext;

    public AESencryptionTask(Context context, Uri uri, File file) {
        if (uri == null) {
            throw new IllegalArgumentException("You need to supply a url to a clear MP4 file to download and encrypt, or modify the code to use a local encrypted mp4");
        }
        mUri = uri;
        mFile = file;
        mContext = context;
    }

    private void downloadAndEncrypt() throws Exception {

//        URL url = new URL(mUrl);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.connect();
//
//        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
//            throw new IOException("server error: " + connection.getResponseCode() + ", " + connection.getResponseMessage());
//        }
//
//        InputStream inputStream = connection.getInputStream();
//        FileOutputStream fileOutputStream = new FileOutputStream(mFile);
//        CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, mCipher);
//
//        byte buffer[] = new byte[1024 * 1024];
//        int bytesRead;
//        while ((bytesRead = inputStream.read(buffer)) != -1) {
//            Log.d(getClass().getCanonicalName(), "reading from http...");
//            cipherOutputStream.write(buffer, 0, bytesRead);
//        }
//
//        inputStream.close();
//        cipherOutputStream.close();
//        connection.disconnect();
    }

    private void encryptFile() throws Exception {
        InputStream inputStream = mContext.getContentResolver().openInputStream(mUri);
        FileOutputStream fileOutputStream = new FileOutputStream(mFile);
        CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, mCipher);

        byte[] buffer = new byte[1024 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            Log.d(getClass().getCanonicalName(), "reading from http...");
            cipherOutputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        cipherOutputStream.close();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mCipher = AESHelper.INSTANCE.getEncryptionCypher();
            encryptFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(getClass().getCanonicalName(), "done");
    }
}
