package com.vvechirko.filetest

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.vvechirko.filetest.data.Photo

object DownloadHelper {

    fun download(photo: Photo) {
        val request = DownloadManager.Request(Uri.parse(photo.url))
//            .setMimeType(photo.contentType)
//            .addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            .addRequestHeader("User-Agent", System.getProperty("http.agent"))
            .setTitle(photo.title)
            .setDescription("Downloading file...")

            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/")

        request.allowScanningByMediaScanner()
        val dm = App.appContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }
}