package com.example.contactstest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

public class PhotoHelper {
    
    public static final String TAG = "PhotoHelper";
    private static final String[] PHOTO_ID_PROJECTION = new String[] {
        ContactsContract.Contacts.PHOTO_ID
    };

    private static final String[] PHOTO_BITMAP_PROJECTION = new String[] {
        ContactsContract.CommonDataKinds.Photo.PHOTO
    };
    
    /**
     * 获取缩略图数据：官方
     * @param contactId
     * @return
     */
    public static byte[] openPhoto(Context context, long contactId) {
        assert (context != null);
        byte[] result = null;
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
             new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                result = cursor.getBlob(0);
            }
        } finally {
            cursor.close();
        }
        return result;
    }
    
    /**
     * 获取photo file数据：官方
     * @param context
     * @param contactId
     * @return
     */
    public static byte[] getDisplayPhoto(Context context, long contactId) {
        assert (context != null);
        byte[] result = null; 
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
            is = fd.createInputStream();
            if (is != null) {
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read = 0;
                int totalLength = 0;
                while ((read = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, read);
                    totalLength += read;
                }
                Log.d(TAG, "getDisplayPhoto totalLength=" + totalLength);
                baos.flush();
                result = baos.toByteArray();
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    
    /**
     * 获取photo file数据：官方
     * @param context
     * @param contactId
     * @param preferHighres
     * @return
     */
    public static byte[] getPhotoBytes(Context context, long contactId, boolean preferHighres) {
        assert (context != null);
        byte[] result = null;
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        InputStream input = ContactsContract.Contacts
                .openContactPhotoInputStream(context.getContentResolver(), uri, preferHighres);
        ByteArrayOutputStream baos = null;
        try {
            if (input != null) {
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024 * 4];
                int read = 0;
                while ((read = input.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                baos.flush();
                result = baos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    
    /**
     * 设置联系人头像（文件+缩略图都会生成）:非官方，tsf中使用
     * @param rawContactId
     * @param data
     * @return
     */
    public static boolean setContactThumbnail(Context context, long rawContactId, byte[] data) {
        assert (context != null);
        boolean result = false;
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        int photoRow = -1;
        String where = ContactsContract.Data.RAW_CONTACT_ID + " = "
                + rawContactId + " AND " + ContactsContract.Data.MIMETYPE
                + "=='"
                + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                + "'";
        Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
        if (cursor == null)
            return false;
        
        int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
        if (cursor.moveToFirst()) {
            photoRow = cursor.getInt(idIdx);
        }
        cursor.close();

        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, data);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

        if (photoRow >= 0) {
            if (resolver.update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data._ID + " = " + photoRow, null) > 0) {
                result = true;
            }
        } else {
            if (resolver.insert(ContactsContract.Data.CONTENT_URI, values) != null) {
                result = true;
            }
        }
        return result;
    }
    
    public static boolean setDisplayPhoto(Context context, long rawContactId, File file) {
        final Uri inputUri = Uri.fromFile(file);
        final Uri outputUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
        
        return PhotoHelper.savePhotoFromUriToUri(context, inputUri, outputUri, false);
    }
    
    /**
     * TOS CSP源码
     * @param context
     * @param inputUri
     * @param outputUri
     * @param deleteAfterSave
     * @return
     */
    public static boolean savePhotoFromUriToUri(Context context, Uri inputUri, Uri outputUri,
            boolean deleteAfterSave) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(outputUri, "rw").createOutputStream();
            inputStream = context.getContentResolver().openInputStream(
                    inputUri);

            final byte[] buffer = new byte[16 * 1024];
            int length;
            int totalLength = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalLength += length;
            }
            Log.v(TAG, "Wrote " + totalLength + " bytes for photo " + inputUri.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write photo: " + inputUri.toString() + " because: " + e);
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (deleteAfterSave) {
                context.getContentResolver().delete(inputUri, null, null);
            }
        }
        return true;
    }
    
    /**
     * 获取到的是缩略图：非官方
     * @param context
     * @param rawContactId
     * @return
     */
    @Deprecated
    public static byte[] getRawPhotoBytes(Context context, long rawContactId) {
        assert (context != null);
        byte[] result = null;
        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Photo.PHOTO,
        };
        String where = ContactsContract.Data.RAW_CONTACT_ID + "=" + rawContactId
                + " AND " + ContactsContract.Data.MIMETYPE + "=" + "'" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI, projection, where, null, null);
        if (cursor == null)
            return null;
        
        if (cursor.getCount() == 0) {
            return null;
        }
        
        try {
            while (cursor.moveToNext()) {
                String rawId = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
                result = cursor.getBlob(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
                Log.d(TAG, "" + rawId);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * 获取缩略图Id：非官方, from stackoverflow
     * @param context
     * @param phoneNumber
     * @return
     */
    @Deprecated
    public static Integer fetchThumbnailId(Context context, String phoneNumber) {
        ContentResolver resolver = context.getContentResolver();
        final Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        final Cursor cursor = resolver.query(uri, PHOTO_ID_PROJECTION, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");

        try {
            Integer thumbnailId = null;
            if (cursor.moveToFirst()) {
                thumbnailId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
            }
            return thumbnailId;
        }
        finally {
            cursor.close();
        }

    }

    /**
     * 获取缩略图数据：非官方, from stackoverflow
     * @param context
     * @param thumbnailId
     * @return
     */
    @Deprecated
    public static byte[] fetchThumbnail(Context context, final int thumbnailId) {
        ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, thumbnailId);
        final Cursor cursor = resolver.query(uri, PHOTO_BITMAP_PROJECTION, null, null, null);

        try {
            byte[] data = null;
            if (cursor.moveToFirst()) {
                data = cursor.getBlob(0);
            }
            return data;
        }
        finally {
            cursor.close();
        }
    }
    
    public static boolean saveBitmap(Bitmap bm, String fileName, int rate) {
        if (bm == null || TextUtils.isEmpty(fileName)) {
            return false;
        }
        
        boolean result = false;
        File f = new File(fileName);
        if (f.exists()) {
           f.delete();
        }
        FileOutputStream fos = null;
        try {
           fos = new FileOutputStream(f);
           bm.compress(Bitmap.CompressFormat.JPEG, rate, fos);
           fos.flush();
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    result = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return result;
    }
    
    public static byte[] convertFile2Bytes(File file) {
        byte[] bFile = new byte[(int) file.length()];
        FileInputStream fileInputStream = null;
        try {
            // convert file into array of bytes
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bFile;
    }
    
    public static boolean convertBytes2File(byte[] data, String filePath) {
        boolean result = false;
        FileOutputStream fileOuputStream = null;
        try {
            // convert array of bytes into file
            fileOuputStream = new FileOutputStream(filePath);
            fileOuputStream.write(data);
            fileOuputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOuputStream != null) {
                try {
                    fileOuputStream.close();
                    result = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    
    public static void setUriData(Context context, Uri input, Uri output) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(output, "rw").createOutputStream();
            inputStream = context.getContentResolver().openInputStream(
                    input);

            final byte[] buffer = new byte[16 * 1024];
            int length;
            int totalLength = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalLength += length;
            }
            Log.v(TAG, "Wrote " + totalLength + " bytes for photo " + input.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write photo: " + input.toString() + " because: " + e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static byte[] getUriData(Context context, Uri intput) {
        assert (context != null);
        byte[] result = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(intput, "r");
            is = fd.createInputStream();
            if (is != null) {
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read = 0;
                int totalLength = 0;
                while ((read = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, read);
                    totalLength += read;
                }
                Log.d(TAG, "getDisplayPhoto totalLength=" + totalLength);
                baos.flush();
                result = baos.toByteArray();
            }
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
