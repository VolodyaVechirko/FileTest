package com.vvechirko.filetest.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.vvechirko.filetest.FilesUtils
import java.io.File
import java.io.IOException

class ContentHolder {

    private lateinit var fileUri: Uri

    @Throws(IllegalAccessException::class)
    fun takePhotoIntent(context: Context): Intent {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                val file: File = FilesUtils.createImageFile()
                fileUri = FilesUtils.uriForFile(file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                return intent
            } catch (ex: IOException) {
                throw IllegalAccessException("Error occurred while creating the File")
            }
        } else {
            throw IllegalAccessException("No camera app")
        }
    }

    @Throws(IllegalAccessException::class)
    fun takeVideoIntent(context: Context): Intent {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                val file: File = FilesUtils.createVideoFile()
                fileUri = FilesUtils.uriForFile(file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                return intent
            } catch (ex: IOException) {
                throw IllegalAccessException("Error occurred while creating the File")
            }
        } else {
            throw IllegalAccessException("No camera app")
        }
    }

    @Throws(IllegalAccessException::class)
    fun takeFilesIntent(context: Context): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = "*/*"
        if (intent.resolveActivity(context.packageManager) != null) {
            return intent
        } else {
            throw IllegalAccessException("No file explorer app")
        }
    }

    @Throws(IllegalAccessException::class)
    fun openFileIntent(context: Context, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_VIEW, uri)
//        intent.type = "*/*"
        if (intent.resolveActivity(context.packageManager) != null) {
            return intent
        } else {
            throw IllegalAccessException("No any app")
        }
    }

    fun getUris(intent: Intent?): List<Uri> {
        intent?.clipData?.let {
            val list = mutableListOf<Uri>()
            for (i in 0 until it.itemCount) {
                list.add(it.getItemAt(i).uri)
            }
            return list
        }
        val data: Uri? = intent?.data
        return listOf(data ?: fileUri)
    }
}