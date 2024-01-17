package com.switchpayment

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.switchpay.android.SwitchPayMacros
import java.util.HashMap
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


class MainActivityDelegate @VisibleForTesting internal constructor(
    activity: Activity,
    result: MethodChannel.Result?,
) :
    PluginRegistry.ActivityResultListener {
    private val activity: Activity
    private var pendingResult: MethodChannel.Result?
    private var eventSink: EventChannel.EventSink? = null

    private var amount = ""
    private var description = ""
    private var name = ""
    private var mobile = ""
    private var email = ""
    private var user_uuid = ""
    private var bearer_token = ""
    private var base_url = ""
    private var order_id = ""

    constructor(activity: Activity) : this(activity, null)

    fun setEventHandler(eventSink: EventChannel.EventSink?) {
        this.eventSink = eventSink
    }

    init {
        this.activity = activity
        pendingResult = result
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val resultData = HashMap<String,String>()

        if (requestCode === REQUEST_CODE && resultCode === Activity.RESULT_OK)
        {
            dispatchEventStatus(true)

            Thread {
                resultData["status"] = data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
                resultData["transaction_id"] = data.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
                resultData["order_id"] = data.getStringExtra(SwitchPayMacros.ORDER_ID)!!
                //pendingResult.success(resultData)
                finishWithSuccess(resultData)
            }.start()
            return true
        }
        else
        {
            if (null != data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)) {
                resultData["status"] = data.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
            }
            if (null != data.getStringExtra(SwitchPayMacros.UPI_TXNID)) {
                resultData["transaction_id"] = data.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
            }
            if (null != data.getStringExtra(SwitchPayMacros.ORDER_ID)) {
                resultData["order_id"] = data.getStringExtra(SwitchPayMacros.ORDER_ID)!!
            }
            var message = ""
            if (null != data.getStringExtra(SwitchPayMacros.MESSAGE))
            {
                message = data.getStringExtra(SwitchPayMacros.MESSAGE)!!
                if (data.getStringExtra(SwitchPayMacros.UPI_STATUS)!=null)
                {
                    //pendingResult.error( it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!,message,resultData)
                    finishWithError(data.getStringExtra(SwitchPayMacros.UPI_STATUS)!!,message,resultData)
                }
                else
                {
                    //pendingResult.error(message,resultData)
                    finishWithError("",message,resultData)
                }
            }
            else{
                //pendingResult.error("",message,resultData)
                finishWithError("",message,resultData)
            }

            return true
        }

        return false

//        if (requestCode === REQUEST_CODE && resultCode === Activity.RESULT_OK) {
//            dispatchEventStatus(true)
//            Thread(object : Runnable() {
//                @Override
//                fun run() {
//                }
//            }).start()
//            return true
//        } else if (requestCode === REQUEST_CODE && resultCode === Activity.RESULT_CANCELED) {
//            Log.i(TAG, "User cancelled the Switch Payment open request")
//            finishWithSuccess(null)
//            return true
//        } else if (requestCode === REQUEST_CODE) {
//            finishWithError("unknown_activity", "Unknown activity error, please fill an issue.")
//        }
//        return false
    }

    private fun setPendingMethodCallAndResult(result: MethodChannel.Result): Boolean {
        if (pendingResult != null) {
            return false
        }
        pendingResult = result
        return true
    }

    @SuppressWarnings("deprecation")
    private fun checkout() {
        var intent = Intent( this.activity, MainActivityDelegate::class.java)
        intent.putExtra(SwitchPayMacros.AMOUNT, amount)
        intent.putExtra(SwitchPayMacros.DESCRIPTION, description)
        intent.putExtra(SwitchPayMacros.NAME, name)
        intent.putExtra(SwitchPayMacros.MOBILE, mobile)
        intent.putExtra(SwitchPayMacros.EMAIL, email)
        intent.putExtra(SwitchPayMacros.USER_UUID, user_uuid)
        intent.putExtra(SwitchPayMacros.BEARER_TOKEN, bearer_token)
        intent.putExtra(SwitchPayMacros.BASE_URL, base_url)
        intent.putExtra(SwitchPayMacros.ORDER_ID, order_id)
        intent.putExtra(SwitchPayMacros.OTHER_INFO, "")
        //resultLauncher?.launch(intent)

        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, REQUEST_CODE)
        } else {
            Log.e(
                TAG,
                "Can't find a valid activity to handle the request. Make sure you've a file explorer installed."
            )
            finishWithError("invalid_format_type", "Can't handle the provided file type.", null)
        }




//        // Temporary fix, remove this null-check after Flutter Engine 1.14 has landed on stable
//        if (type == null) {
//            return
//        }
//        if (type!!.equals("dir")) {
//            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        } else {
//            if (type!!.equals("image/*")) {
//                intent = Intent(
//                    Intent.ACTION_PICK,
//                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                )
//            } else {
//                intent = Intent(Intent.ACTION_GET_CONTENT)
//                intent.addCategory(Intent.CATEGORY_OPENABLE)
//            }
//            val uri: Uri =
//                Uri.parse(Environment.getExternalStorageDirectory().getPath() + File.separator)
//            Log.d(TAG, "Selected type $type")
//            intent.setDataAndType(uri, type)
//            intent.setType(type)
//            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultipleSelection)
//            intent.putExtra("multi-pick", isMultipleSelection)
//            if (type.contains(",")) {
//                allowedExtensions = type.split(",")
//            }
//            if (allowedExtensions != null) {
//                intent.putExtra(Intent.EXTRA_MIME_TYPES, allowedExtensions)
//            }
//        }
//        if (intent.resolveActivity(activity.getPackageManager()) != null) {
//            activity.startActivityForResult(intent, REQUEST_CODE)
//        } else {
//            Log.e(
//                TAG,
//                "Can't find a valid activity to handle the request. Make sure you've a file explorer installed."
//            )
//            finishWithError("invalid_format_type", "Can't handle the provided file type.")
//        }
    }

    @SuppressWarnings("deprecation")
    fun checkout(
        result: MethodChannel.Result,
        amount: String,
        description: String,
        name: String,
        mobile: String,
        email: String,
        user_uuid: String,
        bearer_token: String,
        base_url: String,
        order_id: String,
    ) {
        if (!setPendingMethodCallAndResult(result)) {
            finishWithAlreadyActiveError(result)
            return
        }

        this.amount = amount
        this.description = description
        this.name = name
        this.mobile = mobile
        this.email = email
        this.user_uuid = user_uuid
        this.bearer_token = bearer_token
        this.base_url = base_url
        this.order_id = order_id

        this.checkout()
    }

    @SuppressWarnings("unchecked")
    private fun finishWithSuccess(data: HashMap<String, String>) {
        dispatchEventStatus(false)

        // Temporary fix, remove this null-check after Flutter Engine 1.14 has landed on stable
        if (pendingResult != null) {
            pendingResult!!.success(data)
            clearPendingResult()
        }
    }

    private fun finishWithError(errorCode: String, errorMessage: String, data: HashMap<String, String>?) {
        if (pendingResult == null) {
            return
        }
        dispatchEventStatus(false)
        pendingResult!!.error(errorCode, errorMessage, data)
        clearPendingResult()
    }

    private fun dispatchEventStatus(status: Boolean) {
        if (eventSink == null) {
            return
        }
        object : Handler(Looper.getMainLooper()) {
            @Override
            fun handleMessage(message: Message?) {
                eventSink!!.success(status)
            }
        }.obtainMessage().sendToTarget()
    }

    private fun clearPendingResult() {
        pendingResult = null
    }

    companion object {
        private const val TAG = "MainActivityDelegate"
        private val REQUEST_CODE: Int = MainActivityDelegate::class.java.hashCode() + 43 and 0x0000ffff
        private fun finishWithAlreadyActiveError(result: MethodChannel.Result) {
            result.error("already_active", "Switch Payment is already active", null)
        }
    }
}
