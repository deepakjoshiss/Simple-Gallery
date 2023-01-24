package com.simplemobiletools.gallery.pro.aes;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

public class AESencryptionTask extends AsyncTask<Void, Void, Void> {

    private File mFrom;
    private File mTo;
    private Cipher mCipher;
    private Context mContext;
    private final AESFileUtils fileUtils = AESFileUtils.INSTANCE;

    public AESencryptionTask(Context context, File fromFile, File toFile) {

        mFrom = fromFile;
        mTo = toFile;
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

    private void createFileMeta(String fileParentPath, String nameWE) {
        try {
            String fromPath = mFrom.getAbsolutePath();
            byte[] thumb = mCipher.doFinal(fileUtils.getVideoThumbnail(fromPath));
            byte[] dur = mCipher.doFinal(fileUtils.getDuration(fromPath));
            fileUtils.writeByteArrayToFile(mContext, new File(fileParentPath, nameWE.concat(AESFileUtils.AES_THUMB_EXT)), thumb);
            fileUtils.writeByteArrayToFile(mContext, new File(fileParentPath, nameWE.concat(AESFileUtils.AES_META_EXT)), dur);
        } catch (Exception e) {
            System.out.println(">>>> create meta error " + fileParentPath + " " + nameWE);
            e.printStackTrace();
        }
    }

    private void encryptFile() throws Exception {
        byte[] encName = mCipher.doFinal(mTo.getName().getBytes(StandardCharsets.UTF_8));
        String b64 = fileUtils.encodeBase64Name(encName);
        byte[] dec = fileUtils.decodeBase64Name(b64);
        System.out.println(">>>> file " + dec.length + "  " + b64.length() + "  " + AESHelper.INSTANCE.decryptText(dec));
        File toFile = new File(mTo.getParent(), b64.concat(AESFileUtils.AES_VIDEO_EXT));
        createFileMeta(mTo.getParent(), b64);
        InputStream inputStream = mContext.getContentResolver().openInputStream(Uri.fromFile(mFrom));
        FileOutputStream fileOutputStream = new FileOutputStream(toFile);
        CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, mCipher);
        byte[] buffer = new byte[1024 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            Log.d(getClass().getCanonicalName(), "reading from file...");
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
