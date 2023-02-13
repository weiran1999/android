package com.thoughtworks.android.ui.jsbridge

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebChromeClient
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.thoughtworks.android.R
import kotlin.system.exitProcess

/**
 * js桥的一个实践
 * [参考项目，对应的JS-react项目也在其中](https://github.com/beichensky/jsbridge-example)
 */
class JsBridgeActivity : AppCompatActivity(), View.OnClickListener {

    private var mWebView: BridgeWebView? = null

    companion object {
        // URL 网络请求地址 (用局域网IP加对应项目的端口)
        const val URL = "http://192.168.31.202:8000"
    }

    private var exitTime: Long = 0
    private var mTvUser: TextView? = null
    private var mEditName: EditText? = null
    private var mEditCookie: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bridge)
        initWebView()
        registerHandlers()
        initViews()
    }

    override fun onResume() {
        super.onResume()
        mWebView!!.reload()
    }

    /**
     * 初始化 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        mWebView = findViewById(R.id.main_wv)

        mWebView!!.settings.allowFileAccess = true
        mWebView!!.settings.setAppCacheEnabled(true)
        mWebView!!.settings.databaseEnabled = true
        // 开启 localStorage
        mWebView!!.settings.domStorageEnabled = true
        // 设置支持javascript
        mWebView!!.settings.javaScriptEnabled = true
        // 进行缩放
        mWebView!!.settings.builtInZoomControls = true
        // 设置UserAgent
        mWebView!!.settings.userAgentString = mWebView!!.settings.userAgentString + "app"
        // 设置不用系统浏览器打开,直接显示在当前WebView
        mWebView!!.webChromeClient = WebChromeClient()
        mWebView!!.webViewClient = MyWebViewClient(mWebView!!)

        mWebView!!.loadUrl(URL)
    }

    /**
     * 注册与 H5 交互的事件函数
     */
    private fun registerHandlers() {
        // 设置默认接收函数
        mWebView!!.setDefaultHandler { data, function ->
            Toast.makeText(this@JsBridgeActivity, data, Toast.LENGTH_LONG).show()
            function.onCallBack("安卓返回给 JS 的消息内容")
        }
        // 注册刷新页面的 reloadUrl 函数
        mWebView!!.registerHandler("reloadUrl") { _, function ->
            mWebView!!.reload()
            Toast.makeText(this@JsBridgeActivity, "刷新成功~", Toast.LENGTH_SHORT).show()
            function.onCallBack("")
        }
        // 注册修改 User 名称的 changeUser 函数
        mWebView!!.registerHandler("changeUser") { user, function ->
            mTvUser!!.text = user
            function.onCallBack("")
        }
    }

    /**
     * 初始化其他 View 组件
     */
    private fun initViews() {
        findViewById<View>(R.id.btn_cookie).setOnClickListener(this)
        findViewById<View>(R.id.btn_name).setOnClickListener(this)
        findViewById<View>(R.id.btn_init).setOnClickListener(this)
        mTvUser = findViewById(R.id.tv_user)
        mEditCookie = findViewById(R.id.edit_cookie)
        mEditName = findViewById(R.id.edit_name)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_init ->
                // 调用 H5 界面的默认接收函数
                mWebView!!.send(
                    "安卓传递给 JS 的消息"
                ) { data -> Toast.makeText(this@JsBridgeActivity, data, Toast.LENGTH_LONG).show() }

            R.id.btn_name ->
                // 调用 H5 界面的 changeName 事件函数
                mWebView!!.callHandler("changeName", mEditName!!.text.toString()) {
                    Toast.makeText(this@JsBridgeActivity, "name 修改成功", Toast.LENGTH_SHORT)
                        .show()
                    mEditName!!.setText("")
                }

            R.id.btn_cookie -> {
                syncCookie(this, "token=" + mEditCookie!!.text.toString())
                // 调用 H5 界面的 syncCookie 事件函数
                mWebView!!.callHandler("syncCookie", "") {
                    Toast.makeText(this@JsBridgeActivity, "Cookie 同步成功", Toast.LENGTH_SHORT)
                        .show()
                    mEditCookie!!.setText("")
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView!!.canGoBack()) {
            // 返回前一个页面
            mWebView!!.goBack()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 退出应用
     */
    private fun exit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            Toast.makeText(
                applicationContext, "再按一次退出程序",
                Toast.LENGTH_SHORT
            ).show()
            exitTime = System.currentTimeMillis()
        } else {
            finish()
            exitProcess(0)
        }
    }

    /**
     * 这只并同步 Cookie 的工具函数
     * @param context   上下文对象
     * @param cookie    需要设置的 cookie 值，例如："token=azhd57hkslz"
     */
    @Suppress("DEPRECATION")
    private fun syncCookie(context: Context, cookie: String) {
        CookieSyncManager.createInstance(context)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeSessionCookie()// 移除
        cookieManager.removeAllCookie()
        cookieManager.setCookie(URL, cookie)
        val newCookie = cookieManager.getCookie(URL)
        Log.i("tag ", "newCookie == $newCookie")
        CookieSyncManager.getInstance().sync()
    }
}