package moe.johnny.areyouok

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.activity_hello_indian_mi_fan.*
import java.io.*
import java.math.BigDecimal


class HelloIndianMiFanAct : AppCompatActivity() {

    private var rawData : HashMap<String, String> = hashMapOf()
    private var currentRow = 0
    private var design = 0

    private var loss = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hello_indian_mi_fan)
        loadData()
        refreshBtn.setOnClickListener { loadData() }
    }

    private fun loadData() {
        try {
            val p = Runtime.getRuntime().exec("su")
            val dos = DataOutputStream(p.outputStream)
            dos.writeBytes("cat /sys/class/power_supply/bms/uevent\n")
            dos.writeBytes("exit\n")
            dos.flush()
            try {
                p.waitFor()
                if (p.exitValue() == 0) {
                    val reader = BufferedReader(InputStreamReader(p.inputStream))
                    saveData(reader)
                    if (checkDeviceSupport()) {
                        loss = getBatteryLoss()
                        selectBackground(loss)
                        setBatteryLossProgress(loss)
                        setTips(loss, getIntData("POWER_SUPPLY_CAPACITY"))
                    } else {
                        unsupportDevice()
                    }
                } else {
                    permissionDenied()
                }
            } catch (e: InterruptedException) {
                unsupportDevice()
            }
        } catch (e: IOException) {
            permissionDenied()
        }
    }

    private fun setTips(loss: Float, curPercent: Int) {
        var tips = ""
        if (curPercent != 100) {
            tips = "当前损耗为估算数据，可能存在较大误差，请将电池饱和充电后，在连接充电器的情况下再次测试损耗，将会得到最准切的损耗数据\n"
        }
        tips += when (loss) {
            in -999..10 -> "您的电池当前状态良好，无需更换电池"
            in 11..20 -> "您的电池状态一般， 暂时还不需要更换电池"
            in 21..40 -> "您的电池损耗损耗已经超过20Pa，建议您去守候更换电池，以保证手机的续航"
            else -> "您的电池严重损耗，建议您立即更换电池来保证设备的正常运转"
        }
        tipsTv.text = tips
    }

    private fun selectBackground(loss: Float) {
        val resId: Int = when (loss) {
            in -999..10 -> R.drawable.bg_battery_very_good
            in 11..20 -> R.drawable.bg_battery_normal
            in 21..40 -> R.drawable.bg_battery_suggest_replace
            else -> R.drawable.bg_battery_almost_borken
        }
        setBackground(resId)
    }

    private fun setBackground(@DrawableRes resId: Int) {
        contentView.background = ContextCompat.getDrawable(this, resId)
    }

    private fun saveData(reader: BufferedReader) {
        var metaData : List<String>
        reader.forEachLine {
            metaData = it.split("=")
            rawData[metaData[0]] = metaData[1]
        }
    }

    private fun getIntData(key: String) : Int {
        if (rawData.isEmpty() || rawData[key] == null) {
            return 0
        }
        return rawData[key]!!.toInt()
    }

    private fun getStrData(key: String) : String? {
        if (rawData.isEmpty() || rawData[key] == null) {
            return null
        }
        return rawData[key]
    }

    private fun checkDeviceSupport() : Boolean {
        val chargeFullDesign = getStrData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
        val chargeNowRaw = getStrData("POWER_SUPPLY_CHARGE_NOW_RAW")
        val capaityRaw = getStrData("POWER_SUPPLY_CAPACITY_RAW")
        val capaity = getStrData("POWER_SUPPLY_CAPACITY")

        return !(chargeFullDesign.isNullOrEmpty() || chargeNowRaw.isNullOrEmpty() ||
                capaityRaw.isNullOrEmpty() || capaity.isNullOrEmpty())
    }


    //获得真正的百分比
    @SuppressLint("SetTextI18n")
    private fun getRealPercent() : Float {
        design = getIntData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
        currentRow = getIntData("POWER_SUPPLY_CHARGE_NOW_RAW")
        var bd = BigDecimal((currentRow/design.toDouble()) * 100)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toFloat()
    }

    private fun getBatteryLoss() : Float {
        return if (getIntData("POWER_SUPPLY_CAPACITY") != 100) {
            val capacityRaw = getIntData("POWER_SUPPLY_CAPACITY_RAW")
            val capacityStand = getIntData("POWER_SUPPLY_CAPACITY") * 100
            var bd = BigDecimal((1 - (capacityRaw/capacityStand.toDouble())) * 100)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
            bd.toFloat()
        } else {
            var bd = BigDecimal(100 - (getRealPercent() / getIntData("POWER_SUPPLY_CAPACITY").toDouble()) * 100)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
            bd.toFloat()
        }
    }

    private fun setBatteryLossProgress(loss: Float) {
        batteryLostProgress.setColor(ContextCompat.getColor(this, R.color.batteryProgressColor))
        batteryLostProgress.alpha = 0.3f
        batteryLostProgress.setDimAlpha(0)
        batteryLostProgress.setFormatDigits(1)
        batteryLostProgress.stepSize = 100f
        batteryLostProgress.isTouchEnabled = false
        batteryLostProgress.setUnit("%")
        batteryLostProgress.setDrawText(true)
        batteryLostProgress.setDrawInnerCircle(true)
        batteryLostProgress.setCustomText(arrayOf(loss.toString() + "%"))
        batteryLostProgress.setAnimDuration(400)
        batteryLostProgress.showValue(100 - loss, 100f, true)
    }

    private fun permissionDenied() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.dialog_permission_denied)
                .setPositiveButton("OK", { _, _ ->
                    this@HelloIndianMiFanAct.finish()
                })
        builder.create().show()
    }

    private fun unsupportDevice() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.dialog_unsupport_device)
                .setPositiveButton("OK", { _, _ ->
                    this@HelloIndianMiFanAct.finish()
                })
        builder.create().show()
    }

    /*private fun checkDeviceSupport() : Boolean {
        val codeName = getSystemProperty("ro.product.device")
        if (codeName == "gemini" || codeName == "capricorn" || codeName == "lithium") {
            return true
        }
        return false
    }*/

    fun getSystemProperty(key: String): String? {
        var value: String? = null

        try {
            value = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java).invoke(null, key) as String
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return value
    }

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
