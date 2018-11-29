package com.vvechirko.filetest

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import com.vvechirko.filetest.data.Album
import com.vvechirko.filetest.data.Api
import com.vvechirko.filetest.data.ApiService
import com.vvechirko.filetest.data.Photo

class CloudProvider : DocumentsProvider() {

    companion object {
        const val TAG = "CloudProvider"

        // Use these as the default columns to return information about a root if no specific
        // columns are requested in a query.
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        // Use these as the default columns to return information about a document if no specific
        // columns are requested in a query.
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        // No official policy on how many to return, but make sure you do limit the number of recent
        // and search results.
        private val MAX_SEARCH_RESULTS = 20
        private val MAX_LAST_MODIFIED = 5

        private val ROOT = "cloud_root"
        private val ROOT_ID = "root_id"
        private val INIT_DIR = "init_dir"
    }

    private val api = ApiService.create(Api::class.java)

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")

        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        // Use a MatrixCursor to build a cursor
        // with either the requested fields, or the default
        // projection if "projection" is null.
        val result = MatrixCursor(resolveRootProjection(projection))

        // If user is not logged in, return an empty root cursor.  This removes our
        // provider from the list entirely.
        if (!isUserLoggedIn()) {
            return result
        }

        // It's possible to have multiple roots (e.g. for multiple accounts in the
        // same app) -- just add multiple cursor rows.
        result.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT)

            // You can provide an optional summary, which helps distinguish roots
            // with the same title. You can also use this field for displaying an
            // user account name.
            add(DocumentsContract.Root.COLUMN_SUMMARY, "@vvechirko")

            // FLAG_SUPPORTS_CREATE means at least one directory under the root supports
            // creating documents. FLAG_SUPPORTS_RECENTS means your application's most
            // recently used documents will show up in the "Recents" category.
            // FLAG_SUPPORTS_SEARCH allows users to search all documents the application
            // shares.
            add(
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                        DocumentsContract.Root.FLAG_SUPPORTS_RECENTS or
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
            )

            // COLUMN_TITLE is the root title (e.g. Gallery, Drive).
            add(DocumentsContract.Root.COLUMN_TITLE, context.getString(R.string.app_name))

            // This document id cannot change after it's shared.
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_ID)

            // The child MIME types are used to filter the roots and only present to the
            // user those roots that contain the desired type somewhere in their file hierarchy.
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "image/*")
//            add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, freeSpace)
//            add(DocumentsContract.Root.COLUMN_CAPACITY_BYTES, capacity)
            add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }

        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        Log.d(TAG, "queryDocument, documentId: $documentId")

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        if (documentId == ROOT_ID) {
            result.newRow()
                .add(Document.COLUMN_DOCUMENT_ID, INIT_DIR)
                .add(Document.COLUMN_DISPLAY_NAME, "Albums")
                .add(Document.COLUMN_SIZE, 0)
                .add(Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
                .add(Document.COLUMN_LAST_MODIFIED, null)
                .add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
        } else {
            val id = getRealId(documentId)

            if (documentId.contains("album")) {
                val album = api.getAlbum(id).blockingFirst()
                albumToRow(result, album)
            } else {
                val photo = api.getPhoto(id).blockingFirst()
                photoToRow(result, photo)
            }
        }

        return result
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        Log.d(TAG, "queryChildDocuments, parentDocumentId: $parentDocumentId, sortOrder: $sortOrder")

        val result = MatrixCursor(resolveDocumentProjection(projection))

        if (parentDocumentId == INIT_DIR) {
            val albums = api.getAlbums(1).blockingFirst()

            albums.forEach {
                albumToRow(result, it)
            }
        } else {
            val photos = api.getPhotos(getRealId(parentDocumentId)).blockingFirst()

            photos.forEach {
                photoToRow(result, it)
            }
        }
        return result
    }

    private fun albumToRow(result: MatrixCursor, it: Album) {
        result.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, getAlbumId(it.id))
            .add(Document.COLUMN_DISPLAY_NAME, it.title)
            .add(Document.COLUMN_SIZE, 0)
            .add(Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            .add(Document.COLUMN_LAST_MODIFIED, null)
            .add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
    }

    private fun photoToRow(result: MatrixCursor, it: Photo) {
        result.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, getPhotoId(it.id))
            .add(Document.COLUMN_DISPLAY_NAME, it.name)
            .add(Document.COLUMN_SIZE, 1000000)
            .add(Document.COLUMN_MIME_TYPE, getTypeForName(it.name))
            .add(Document.COLUMN_LAST_MODIFIED, null)
            .add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_THUMBNAIL)
    }

    override fun openDocumentThumbnail(documentId: String, sizeHint: Point, signal: CancellationSignal): AssetFileDescriptor {
        Log.d(TAG, "openDocumentThumbnail documentId: $documentId")

        val file = api.getPhoto(getRealId(documentId))
            .flatMap { photo ->
                api.download(photo.thumbnailUrl)
                    .map { FilesUtils.cacheThumb(photo, it) }
            }
            .blockingFirst()

        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        Log.d(TAG, "openDocument, documentId: $documentId, mode: $mode")
        // It's OK to do network operations in this method to download the document, as long as you
        // periodically check the CancellationSignal.  If you have an extremely large file to
        // transfer from the network, a better solution may be pipes or sockets
        // (see ParcelFileDescriptor for helper methods).

        val file = api.getPhoto(getRealId(documentId))
            .flatMap { photo ->
                api.download(photo.url)
                    .map { FilesUtils.cachePhoto(photo, it) }
            }
            .blockingFirst()

        val accessMode = ParcelFileDescriptor.parseMode(mode)

        val isWrite = mode.indexOf('w') != -1
        return if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            val handler = Handler(context!!.mainLooper)
            ParcelFileDescriptor.open(file, accessMode, handler) {
                // Update the file with the cloud server.  The client is done writing.
                Log.i(
                    TAG, ("A file with id " + documentId + " has been closed!  Time to " +
                            "update the server.")
                )
            }

        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }

    /**
     * @param projection the requested root column projection
     * @return either the requested root column projection, or the default projection if the
     * requested projection is null.
     */
    private fun resolveRootProjection(projection: Array<out String>?): Array<out String> {
        return projection ?: DEFAULT_ROOT_PROJECTION
    }

    private fun resolveDocumentProjection(projection: Array<out String>?): Array<out String> {
        return projection ?: DEFAULT_DOCUMENT_PROJECTION
    }

    /**
     * Get the MIME data type of a document, given its filename.
     *
     * @param name the filename of the document
     * @return the MIME data type of a document
     */
    private fun getTypeForName(name: String): String {
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return mime
            }
        }
        return "application/octet-stream"
    }

    private fun getAlbumId(id: Int): String = "album/$id"

    private fun getPhotoId(id: Int): String = "photo/$id"

    private fun getRealId(id: String): Int {
        return id.split("/").last().toInt()
    }


    /**
     * Dummy function to determine whether the user is logged in.
     */
    private fun isUserLoggedIn(): Boolean {
        return true
    }
}