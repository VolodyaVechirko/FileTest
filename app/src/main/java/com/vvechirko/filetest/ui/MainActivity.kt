package com.vvechirko.filetest.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.vvechirko.filetest.FilesUtils
import com.vvechirko.filetest.R
import com.vvechirko.filetest.data.Api
import com.vvechirko.filetest.data.ApiService
import com.vvechirko.filetest.data.Photo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), Adapter.Interaction {

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1001
        const val REQUEST_VIDEO_CAPTURE = 1002
        const val REQUEST_PICK_FILE = 1003
        const val WRITE_EXTERNAL_STORAGE = 1004
    }

    val api = ApiService.create(Api::class.java)
    val disposable = CompositeDisposable()
    val adapter = Adapter()
    val contentHolder = ContentHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        adapter.interaction = this
        photoList.adapter = adapter

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        loadPhotos()
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    override fun download(photo: Photo) {
//        DownloadHelper.download(photo)
        api.download(photo.url)
            .subscribeOn(Schedulers.io())
            .map { FilesUtils.savePublicFile(photo, it) }
            .flatMap { FilesUtils.scanUri(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { openUri(it) },
                { toast(it.toString()) }
            ).also { disposable.add(it) }
    }

    override fun open(photo: Photo) {
        api.download(photo.url)
            .subscribeOn(Schedulers.io())
            .map { FilesUtils.savePrivateFile(photo, it) }
            .map { FilesUtils.uriForFile(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { openUri(it) },
                { toast(it.toString()) }
            ).also { disposable.add(it) }
    }

    fun openUri(uri: Uri) {
        try {
            log(uri)
            startActivity(contentHolder.openFileIntent(this, uri))
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    fun takePhoto() {
        try {
            val intent = contentHolder.takePhotoIntent(this)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    fun takeVideo() {
        try {
            val intent = contentHolder.takeVideoIntent(this)
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    fun uploadFiles() {
        try {
            val intent = contentHolder.takeFilesIntent(this)
            startActivityForResult(intent, REQUEST_PICK_FILE)
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE, REQUEST_VIDEO_CAPTURE, REQUEST_PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val uris = contentHolder.getUris(data)
                    log(uris)
//                    contentResolver.openFileDescriptor(uris.first(), "")
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_photo -> {
                takePhoto(); true
            }
            R.id.action_video -> {
                takeVideo(); true
            }
            R.id.action_file -> {
                uploadFiles(); true
            }
            R.id.action_settings -> {
                loadPhotos(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun loadPhotos() {
        api.getPhotos(1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { adapter.setData(it) },
                { toast(it.toString()) }
            ).also { disposable.add(it) }
    }

    fun log(s: Any?) = Log.d("MainActivity", s.toString())

    fun toast(s: Any?) = Toast.makeText(this, s.toString(), Toast.LENGTH_SHORT).show().also { log(s) }
}
