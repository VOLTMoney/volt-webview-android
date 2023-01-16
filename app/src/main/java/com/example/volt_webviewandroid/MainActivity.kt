package com.example.volt_webviewandroid

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var webSettings: WebSettings? = null
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private var size: Long = 0

    var appURL = Uri.parse("http://app.staging.voltmoney.in/partnerplatform?platform=SDK_INVESTWELL")


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mUploadMessage == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        try {
            val file_path = mCameraPhotoPath!!.replace("file:", "")
            val file = File(file_path)
            size = file.length()
        } catch (e: Exception) {
            Log.e("Error!", "Error while opening image file" + e.localizedMessage)
        }
        if (data != null || mCameraPhotoPath != null) {
            var count: Int? = 0 //fix fby https://github.com/nnian
            var images: ClipData? = null
            try {
                images = data?.clipData
            } catch (e: Exception) {
                Log.e("Error!", e.localizedMessage)
            }
            if (images == null && data != null && data.dataString != null) {
                count = data.dataString!!.length
            } else if (images != null) {
                count = images.itemCount
            }
            var results: Array<Uri?> = arrayOfNulls<Uri>(count!!)
            // Check that the response is a good one
            if (resultCode == RESULT_OK) {
                if (size != 0L) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = arrayOf(Uri.parse(mCameraPhotoPath))
                    }
                } else if (data?.clipData == null) {
                    results = arrayOf(Uri.parse(data?.dataString))
                } else {
                    for (i in 0 until images!!.itemCount) {
                        results[i] = images.getItemAt(i).uri
                    }
                }
            }
            mUploadMessage!!.onReceiveValue(results as Array<Uri>)
            mUploadMessage = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyStoragePermissions(this)
        webView = findViewById<View>(R.id.webView) as WebView
        webSettings = webView!!.settings
        webSettings!!.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings!!.javaScriptEnabled = true
        webSettings!!.loadWithOverviewMode = true
        webSettings!!.allowFileAccess = true
        webSettings!!.domStorageEnabled = true
        webSettings!!.javaScriptCanOpenWindowsAutomatically = true
        webSettings!!.allowFileAccessFromFileURLs = true
        webSettings!!.setEnableSmoothTransition(true)
        webView!!.webViewClient = PQClient()
        webView!!.webChromeClient = PQChromeClient()
        //if SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
        if (Build.VERSION.SDK_INT >= 19) {
            webView!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        } else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 19) {
            webView!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        webView!!.loadUrl(appURL.toString())
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    inner class PQChromeClient : WebChromeClient() {
        // For Android 5.0+
        override fun onShowFileChooser(
            view: WebView,
            filePath: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            // Double check that we don't have any existing callbacks
            if (mUploadMessage != null) {
                mUploadMessage!!.onReceiveValue(null)
            }
            mUploadMessage = filePath
            Log.e("FileCooserParams => ", filePath.toString())
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                } else {
                    takePictureIntent = null
                }
            }
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            contentSelectionIntent.type = "image/*"
            val intentArray: Array<Intent?>
            intentArray = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(2)
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            startActivityForResult(Intent.createChooser(chooserIntent, "Select images"), 1)
            return true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && webView!!.canGoBack()) {
            webView!!.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    inner class PQClient : WebViewClient() {
        var progressDialog: ProgressDialog? = null


        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

            // main url window
            if (url.contains(appURL.host!!)) {
                webView.loadUrl(url)
            }
            // redirect url window
            else if (url.contains("alpha-") || url.contains("bfin.in") || url.contains("docapp.bajajfinserv.in")) {
                val customIntent = CustomTabsIntent.Builder()
                customIntent.setUrlBarHidingEnabled(true)
                customIntent.setShowTitle(false);
                customIntent.setStartAnimations(
                    this@MainActivity,
                    R.anim.slide_in_right,
                    R.anim.slide_out_right
                )
                customIntent.setExitAnimations(
                    this@MainActivity,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right
                )
                customIntent.setToolbarColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.purple_200
                    )
                );
                openCustomTab(this@MainActivity, customIntent.build(), Uri.parse(url));

            }
            // open camera/document picker
            else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                webView.context.startActivity(intent)
            }
            return true
        }

        //Show loader on url load
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {

            // Then show progress  Dialog
            // in standard case YourActivity.this
            if (progressDialog == null) {
                progressDialog = ProgressDialog(this@MainActivity)
                progressDialog!!.setMessage("Loading...")
                progressDialog!!.hide()
            }
        }

        // Called when all page resources loaded
        override fun onPageFinished(view: WebView, url: String) {
            webView!!.loadUrl(
                "javascript:(function(){ " +
                        "document.getElementById('android-app').style.display='none';})()"
            )
            try {
                // Close progressDialog
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                    progressDialog = null
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private val TAG = MainActivity::class.java.simpleName

        // Storage Permissions variables
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have read or write permission
            val writePermission = ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readPermission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val cameraPermission =
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }

    fun openCustomTab(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri?) {
        // package name is the default package
        // for our custom chrome tab
        val packageName = "com.android.chrome"
        if (packageName != null) {

            // we are checking if the package name is not null
            // if package name is not null then we are calling
            // that custom chrome tab with intent by passing its
            // package name.
            customTabsIntent.intent.setPackage(packageName)

            // in that custom tab intent we are passing
            // our url which we have to browse.
            customTabsIntent.launchUrl(activity, uri!!)
        } else {
            // if the custom tabs fails to load then we are simply
            // redirecting our user to users device default browser.
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}


