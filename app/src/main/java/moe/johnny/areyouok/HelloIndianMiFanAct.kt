package moe.johnny.areyouok

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.activity_hello_indian_mi_fan.*


class HelloIndianMiFanAct : AppCompatActivity() {

    private val mPresenter: HelloIndianMiFanPresenter = HelloIndianMiFanPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hello_indian_mi_fan)
        refreshBtn.setOnClickListener { loadData() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        if (!mPresenter.checkPermission()) {
            permissionDenied()
            return
        }
        if (mPresenter.refreshBmsData()) {
            val loss = mPresenter.getBatteryLoss()
            if (loss == mPresenter.BATTERY_LOSS_UNKNOWN) {
                unsupportDevice()
            }
            selectBackground(loss)
            setBatteryLossProgress(loss)
            setTips(loss, mPresenter.getCapacity())
        } else {
            unsupportDevice()
        }
    }

    private fun setTips(loss: Float, curPercent: Int) {
        var tips = ""
        if (curPercent != 100) {
            if (loss == mPresenter.BATTERY_ONLY_SUPPORT_FULL_MODE) {
                tips = "当前系统不支持在电池充满前获取电池损耗，请将电池饱和充电后测试以获取最准确的损耗信息"
                tipsTv.text = tips
                return
            } else {
                tips = "当前损耗为估算数据，可能存在较大误差，请将电池饱和充电后，在连接充电器的情况下再次测试损耗，将会得到最准切的损耗数据\n"
            }
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
            mPresenter.BATTERY_ONLY_SUPPORT_FULL_MODE -> R.drawable.bg_battery_very_good
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
        batteryLostProgress.setAnimDuration(400)
        if (loss == mPresenter.BATTERY_ONLY_SUPPORT_FULL_MODE) {
            batteryLostProgress.setCustomText(arrayOf("等待充满", "已充满"))
            batteryLostProgress.showValue(mPresenter.getCapacity().toFloat(), 100f, false)
        } else {
            batteryLostProgress.setCustomText(arrayOf(loss.toString() + "%", loss.toString() + "%"))
            batteryLostProgress.showValue(100 - loss, 100f, true)
        }

    }

    fun permissionDenied() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.dialog_permission_denied)
                .setPositiveButton("OK", { _, _ ->
                    this@HelloIndianMiFanAct.finish()
                })
        builder.create().show()
    }

    fun unsupportDevice() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.dialog_unsupport_device)
                .setPositiveButton("OK", { _, _ ->
                    this@HelloIndianMiFanAct.finish()
                })
        builder.create().show()
    }

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
