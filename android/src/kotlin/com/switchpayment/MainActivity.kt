package com.switchpayement

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.switchpay.android.SwitchPayActivity
import com.switchpay.android.SwitchPayMacros
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import java.lang.ref.WeakReference
class MainActivity : FlutterFragmentActivity() {
    private lateinit var channel: MethodChannel
    private var resultLauncher: ActivityResultLauncher<Intent>? = null
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
    private lateinit var result: MethodChannel.Result
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher(WeakReference(this))
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, UPI_CHANNEL)
        channel.setMethodCallHandler { call, result ->
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
                this.result = result
                checkout()
            } }
    }
    private fun registerLauncher(activity: WeakReference<Activity>) {
        try {
            resultLauncher = (activity.get() as ComponentActivity).registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                val resultData = HashMap<String,String>()
                if (Activity.RESULT_OK == it.resultCode)
                {
                    resultData["status"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
                    resultData["transaction_id"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
                    resultData["order_id"] = it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)!!
                    result.success(resultData)
                }
                else
                {
                    if (null != it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)) {
                        resultData["status"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!
                    }
                    if (null != it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)) {
                        resultData["transaction_id"] = it.data!!.getStringExtra(SwitchPayMacros.UPI_TXNID)!!
                    }
                    if (null != it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)) {
                        resultData["order_id"] = it.data!!.getStringExtra(SwitchPayMacros.ORDER_ID)!!
                    }
                    var message = ""
                    if (null != it.data!!.getStringExtra(SwitchPayMacros.MESSAGE))
                    {
                        message = it.data!!.getStringExtra(SwitchPayMacros.MESSAGE)!!
                        if (it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!=null)
                        {
                            result.error( it.data!!.getStringExtra(SwitchPayMacros.UPI_STATUS)!!,message,resultData)
                        }
                        else
                        {
                            result.error("",message,resultData)
                        }
                    }
                    else{
                        result.error("",message,resultData)
                    }
                }
            }
        }
        catch (ex: Exception) {
            Log.i("Test_Switch_Pay", "registerForActivityResult.$ex")
        }
    }

    private fun checkout() {
        try {
            val intent = Intent( this@MainActivity, SwitchPayActivity::class.java)
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
            resultLauncher?.launch(intent)
        }
        catch (e: ActivityNotFoundException) {
            Log.i("Test_Switch_Pay", "ActivityNotFoundException.$e")
        }
    }
}
