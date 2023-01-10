package com.example.volt_webviewandroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        webView = findViewById(R.id.webView)

        /*** Customise setting ***/
        webView.settings.setJavaScriptEnabled(true);
        webView.settings.setDomStorageEnabled(true);
        webView.settings.setAllowFileAccess(true);
        webView.settings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.settings.setAllowFileAccessFromFileURLs(true);
        webView.settings.setEnableSmoothTransition(true);

        /*** End of customise setting ***/

        /*** Hardware acceleration ***/
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT in 11..18) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                ///allowing only volt app to run on this window
                if (url != null && url.contains("http://app.stagging.voltmoney.in")) {
                    return false
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view?.context?.startActivity(intent)
                return true
            }
        }


        webView.loadUrl("http://192.168.0.110:3000/")


        /***** Custom Tab ********/
        // initializing object for custom chrome tabs.
        val customIntent: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        // setting our toolbar color.
        openCustomTab(
            this@MainActivity,
            customIntent.build(),
            Uri.parse("http://192.168.0.110:3000/")
        )
        /***** End of Custom Tab ********/


    }

    override fun onBackPressed() {
        // if your webview can go back it will go back
        if (webView.canGoBack()) webView.goBack()
        // if your webview cannot go back
        // it will exit the application
        else super.onBackPressed()
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

//account !=partnerplatform -> dont show google login then deploy staging.
// "linkedPlatformAccounts": [
//        {
//            "@type": "Platform",
//            "accountId": "90f48862-d7d1-47b2-aa3e-76613da4a3f6",
//            "accountState": "ACTIVE",
//            "isInternal": false,
//            "accountTier": "BASIC",
//            "addedOnTimeStamp": 1669179582866,
//            "lastUpdatedTimeStamp": 1669179582866,
//            "platformName": "Platform 2",
//            "platformLogoImgSrc": "wGTIRwOTOncrw2GIstss",
//            "platformCode": "VOLT_MOBILE_APP", if contains VOLT
//            "platformAgreementIdUri": null,
//            "address": "UFdIMRVvZaJkUamfyCYoiGHaHcmSdA"
//        }
//    ],
// [Share Anuj, that we 2 issue, file upload we are working ]
// Java or Kotlin - resolving.
// Different SDK
// Ask Bharat for MI device
