package com.switchpayment


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


class MainActivityPlugin : MethodChannel.MethodCallHandler, FlutterPlugin, ActivityAware {

    private class LifeCycleObserver(activity: Activity) :
        Application.ActivityLifecycleCallbacks,
        DefaultLifecycleObserver {
        private val thisActivity: Activity

        init {
            thisActivity = activity
        }

        @Override
        override fun onStop(owner: LifecycleOwner) {
            onActivityStopped(thisActivity)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onActivityDestroyed(thisActivity)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        }

        override fun onActivityStarted(activity: Activity) {

        }

        override fun onActivityResumed(activity: Activity) {

        }

        override fun onActivityPaused(activity: Activity) {

        }

        override fun onActivityStopped(activity: Activity) {

        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

        }

        @Override
        override fun onActivityDestroyed(activity: Activity) {
            if (thisActivity === activity && activity.applicationContext != null) {
                (activity.applicationContext as Application).unregisterActivityLifecycleCallbacks(
                    this
                ) // Use getApplicationContext() to avoid casting failures
            }
        }

        //==================


    }

    private var activityBinding: ActivityPluginBinding? = null
    private var delegate: MainActivityDelegate? = null
    private var application: Application? = null
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    // This is null when not using v2 embedding;
    private var lifecycle: Lifecycle? = null
    private var observer: LifeCycleObserver? = null
    private var activity: Activity? = null
    private var channel: MethodChannel? = null

    private fun setup(
        messenger: BinaryMessenger,
        application: Application,
        activity: Activity,
        registrar: PluginRegistry.Registrar?,
        activityBinding: ActivityPluginBinding?
    ) {
        this.activity = activity
        this.application = application
        this.delegate = MainActivityDelegate(activity)
        this.channel = MethodChannel(messenger, UPI_CHANNEL)
        this.channel!!.setMethodCallHandler(this)
        EventChannel(messenger, UPI_CHANNEL).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                delegate!!.setEventHandler(events)
            }

            override fun onCancel(arguments: Any?) {
                delegate!!.setEventHandler(null)
            }
        })
        observer = LifeCycleObserver(activity)
        if (registrar != null) {
            // V1 embedding setup for activity listeners.
            application.registerActivityLifecycleCallbacks(observer)
            registrar.addActivityResultListener(this.delegate!!)
            //@@@registrar.addRequestPermissionsResultListener(this.delegate)
        } else {
            // V2 embedding setup for activity listeners.
            activityBinding!!.addActivityResultListener(this.delegate!!)
            //@@@activityBinding.addRequestPermissionsResultListener(this.delegate)
            lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding)
            lifecycle!!.addObserver(observer!!)
        }
    }

    fun registerWith(registrar: io.flutter.plugin.common.PluginRegistry.Registrar) {
        if (registrar.activity() == null) {
            // If a background flutter view tries to register the plugin, there will be no activity from the registrar,
            // we stop the registering process immediately because the ImagePicker requires an activity.
            return
        }
        val activity: Activity = registrar.activity()!!
        var application: Application? = null
        if (registrar.context() != null) {
            application = registrar.context().getApplicationContext() as Application
        }
        val plugin = MainActivityPlugin()
        if (application != null) {
            plugin.setup(registrar.messenger(), application, activity, registrar, null)
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    override fun onMethodCall(call: MethodCall, rawResult: MethodChannel.Result) {
        if (activity == null) {
            rawResult.error(
                "no_activity",
                "switch payment plugin requires a foreground activity",
                null
            )
            return
        }
        val result: MethodChannel.Result = MethodResultWrapper(rawResult)
        //val arguments = call.arguments as HashMap<*, *>

        if(call.method == null) {
            result.notImplemented()
            return
        }

        if (call.method == "upi") {
            val argument = call.arguments<Map<String, String>>() as Map<String, String>
            this.amount = argument["amount"]!!
            this.description = argument["description"]!!
            this.name = argument["name"]!!
            this.mobile = argument["mobile"]!!
            this.email = argument["email"]!!
            this.user_uuid = argument["user_uuid"]!!
            this.bearer_token = argument["bearer_token"]!!
            this.base_url = argument["base_url"]!!
            this.order_id = argument["order_id"]!!
            // this.result = result
            //checkout()

            this.delegate?.checkout(result, amount, description, name, mobile, email, user_uuid, bearer_token, base_url, order_id)
        }



    }

    // MethodChannel.Result wrapper that responds on the platform thread.
    private class MethodResultWrapper internal constructor(result: MethodChannel.Result) :
        MethodChannel.Result {
        private val methodResult: MethodChannel.Result
        private val handler: Handler

        init {
            methodResult = result
            handler = Handler(Looper.getMainLooper())
        }

        @Override
        override fun success(result: Any?) {
            handler.post { methodResult.success(result) }
        }

        @Override
        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
            handler.post { methodResult.error(errorCode!!, errorMessage, errorDetails) }
        }

        @Override
        override fun notImplemented() {
            handler.post {
                methodResult.notImplemented()
            }
        }
    }

    private fun tearDown() {
        activityBinding!!.removeActivityResultListener(this.delegate!!)
        //@@@activityBinding!!.removeRequestPermissionsResultListener(this.delegate)
        activityBinding = null
        if (observer != null) {
            lifecycle!!.removeObserver(observer!!)
            application!!.unregisterActivityLifecycleCallbacks(observer)
        }
        lifecycle = null
        this.delegate!!.setEventHandler(null)
        this.delegate = null
        this.channel!!.setMethodCallHandler(null)
        this.channel = null
        application = null
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        pluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        pluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        setup(
            pluginBinding!!.binaryMessenger,
            pluginBinding!!.applicationContext as Application,
            activityBinding!!.activity,
            null,
            activityBinding!!
        )
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        tearDown()
    }












    //=======================
    //=======================
    //=======================


    //private lateinit var channel: MethodChannel
    //private var resultLauncher: ActivityResultLauncher<Intent>? = null
    private val UPI_CHANNEL = "upi_channel"
    private var amount = ""
    private var description = ""
    private var name = ""
    private var mobile = ""
    private var email = ""
    private var user_uuid = ""
    private var bearer_token = ""
    private var base_url = ""
    private var order_id = ""
    //private lateinit var result: MethodChannel.Result


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        registerLauncher(WeakReference(this))
//    }
//    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
//        GeneratedPluginRegistrant.registerWith(flutterEngine)
//        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, UPI_CHANNEL)
//        channel.setMethodCallHandler { call, result ->
//            if (call.method == "upi") {
//                val argument = call.arguments<Map<String, String>>() as Map<String, String>
//                this.amount = argument["amount"]!!
//                this.description = argument["description"]!!
//                this.name = argument["name"]!!
//                this.mobile = argument["mobile"]!!
//                this.email = argument["email"]!!
//                this.user_uuid = argument["user_uuid"]!!
//                this.bearer_token = argument["bearer_token"]!!
//                this.base_url = argument["base_url"]!!
//                this.order_id = argument["order_id"]!!
//                this.result = result
//                checkout()
//            }
//        }
//    }
//    private fun registerLauncher(activity: WeakReference<Activity>) {
//        try {
//            resultLauncher = (activity.get() as ComponentActivity).registerForActivityResult(
//                ActivityResultContracts.StartActivityForResult()
//            ) {
//                val resultData = HashMap<String,String>()
//                if (Activity.RESULT_OK == it.resultCode)
//                {
//                    resultData["status"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
//                    resultData["transaction_id"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
//                    resultData["order_id"] = it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)!!
//                    result.success(resultData)
//                }
//                else
//                {
//                    if (null != it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)) {
//                        resultData["status"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
//                    }
//                    if (null != it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)) {
//                        resultData["transaction_id"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
//                    }
//                    if (null != it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)) {
//                        resultData["order_id"] = it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)!!
//                    }
//                    var message = ""
//                    if (null != it.data!!.getStringExtra(SwitchPayMacros.MESSAGE))
//                    {
//                        message = it.data!!.getStringExtra(SwitchPayMacros.MESSAGE)!!
//                        if (it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!=null)
//                        {
//                            result.error( it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!,message,resultData)
//                        }
//                        else
//                        {
//                            result.error("",message,resultData)
//                        }
//                    }
//                    else{
//                        result.error("",message,resultData)
//                    }
//                }
//            }
//        }
//        catch (ex: Exception) {
//            Log.i("Test_Switch_Pay", "registerForActivityResult.$ex")
//        }
//    }
//
//    private fun checkout() {
//        try {
//            val intent = Intent( this@MainActivity, SwitchPayActivity::class.java)
//            intent.putExtra(SwitchPayMacros.AMOUNT, amount)
//            intent.putExtra(SwitchPayMacros.DESCRIPTION, description)
//            intent.putExtra(SwitchPayMacros.NAME, name)
//            intent.putExtra(SwitchPayMacros.MOBILE, mobile)
//            intent.putExtra(SwitchPayMacros.EMAIL, email)
//            intent.putExtra(SwitchPayMacros.USER_UUID, user_uuid)
//            intent.putExtra(SwitchPayMacros.BEARER_TOKEN, bearer_token)
//            intent.putExtra(SwitchPayMacros.BASE_URL, base_url)
//            intent.putExtra(SwitchPayMacros.ORDER_ID, order_id)
//            intent.putExtra(SwitchPayMacros.OTHER_INFO, "")
//            resultLauncher?.launch(intent)
//        }
//        catch (e: ActivityNotFoundException) {
//            Log.i("Test_Switch_Pay", "ActivityNotFoundException.$e")
//        }
//    }
}
