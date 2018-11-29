package com.vvechirko.filetest

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.support.v4.content.FileProvider
import android.text.format.Formatter
import android.util.Log
import com.vvechirko.filetest.data.Photo
import io.reactivex.Observable
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FilesUtils {

    fun savePublicFile(photo: Photo, body: ResponseBody): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, photo.name)
        body.byteStream().use { input ->
            file.outputStream().use {
                input.copyTo(it)
            }
        }
        return file //Uri.fromFile(file)
    }

    fun savePrivateFile(photo: Photo, body: ResponseBody): File {
        val dir = App.appContext().cacheDir//getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, photo.name)
        body.byteStream().use { input ->
            file.outputStream().use {
                input.copyTo(it)
            }
        }
        return file //Uri.fromFile(file)
    }

    fun cachePhoto(photo: Photo, body: ResponseBody): File {
        val dir = App.appContext().cacheDir
        val file = File(dir, photo.name)
        body.byteStream().use { input ->
            file.outputStream().use {
                input.copyTo(it)
            }
        }
        return file
    }

    fun cacheThumb(photo: Photo, body: ResponseBody): File {
        val dir = App.appContext().cacheDir
        val file = File(dir, photo.thumbName)
        body.byteStream().use { input ->
            file.outputStream().use {
                input.copyTo(it)
            }
        }
        return file
    }

    fun scanUri(file: File): Observable<Uri> {
        return Observable.create<Uri> {
            MediaScannerConnection.scanFile(App.appContext(), arrayOf(file.absolutePath), null) { path, uri ->
                Log.d("MediaScannerConnection", "path $path, uri $uri")
                it.onNext(uri)
                it.onComplete()
            }
        }
    }

    fun getContentUri(file: File): Uri {
        var contentUri = Uri.fromFile(file)
        val externalUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)

        val cursor = App.contentResolver().query(
            externalUri, projection,
            MediaStore.Files.FileColumns.DATA + " LIKE ?", arrayOf<String>(file.path), null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val fileId = cursor.getLong(cursor.getColumnIndex(projection[0]))
            contentUri = MediaStore.Files.getContentUri("external", fileId)
            cursor.close()
        }

        return contentUri
    }

    fun timeStamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    fun uriForFile(file: File) = FileProvider.getUriForFile(
        App.appContext(),
        "com.vvechirko.filetest.fileprovider",
        file
    )

    @Throws(IOException::class)
    fun createImageFile(): File {
        return File.createTempFile(
            "JPEG_${timeStamp()}_",
            ".jpg",
            App.appContext().filesDir
        )
    }

    @Throws(IOException::class)
    fun createVideoFile(): File {
        return File.createTempFile(
            "MPEG_${timeStamp()}_",
            ".mp4",
            App.appContext().filesDir
        )
    }

    fun fileMeta(uri: Uri): FileMeta {
        var size = 0L
        var name = "unknown"

        val projection = arrayOf<String>(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = App.contentResolver()
            .query(uri, projection, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
            name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            cursor.close()
        }

        return FileMeta(uri, name, size)
    }

    class FileMeta(val uri: Uri, val name: String, val size: Long)

    fun formatFileSize(size: Long): String {
        return Formatter.formatFileSize(App.appContext(), size)
    }
}