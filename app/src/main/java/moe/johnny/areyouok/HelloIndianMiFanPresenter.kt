package moe.johnny.areyouok

import android.annotation.SuppressLint
import android.util.Log
import java.io.*
import java.math.BigDecimal
import java.security.AccessControlException
import java.security.AccessController

/**
 * Created by bmy001 on 2/26/2018 AD.
 */
class HelloIndianMiFanPresenter(private val mView: HelloIndianMiFanAct) {

    private val bmsProvider = BmsDataProvider()
    val BATTERY_LOSS_UNKNOWN : Float = -999999f
    val BATTERY_ONLY_SUPPORT_FULL_MODE : Float = -999998f

    fun checkPermission() : Boolean {
        val ueventFd = File("/sys/class/power_supply")
        val bmsFd = File("/sys/class/power_supply/bms")
        val bmsUeventFd = File("/sys/class/power_supply/bms/uevent")

        if (ueventFd.canRead() && bmsFd.canRead() && bmsUeventFd.canRead()) {
            bmsProvider.needRoot = false
            return true
        } else {
            bmsProvider.needRoot = true
        }

        try {
            val p = Runtime.getRuntime().exec("su")
            val dos = DataOutputStream(p.outputStream)
            dos.writeBytes("exit\n")
            dos.flush()
            p.waitFor()
            if (p.exitValue() == 0) {
                return true
            }
        } catch (e: InterruptedException) {

        } catch (e: IOException) {

        }
        return false
    }

    fun refreshBmsData() : Boolean {
        return bmsProvider.getData()
    }

    fun getCapacity() : Int {
        return bmsProvider.getIntData("POWER_SUPPLY_CAPACITY")
    }

    //获得真正的百分比
    @SuppressLint("SetTextI18n")
    private fun getRealPercent() : Float {
        val design = bmsProvider.getIntData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
        val currentRow = bmsProvider.getIntData("POWER_SUPPLY_CHARGE_NOW_RAW")
        var bd = BigDecimal((currentRow/design.toDouble()) * 100)
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
        return bd.toFloat()
    }

    fun getBatteryLoss() : Float {
        if (getCapacity() != 100) {
            if (bmsProvider.isSupportStanderMode()) {
                val chargeFullDesign = bmsProvider.getIntData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
                val chargeFull = bmsProvider.getIntData("POWER_SUPPLY_CHARGE_FULL")
                var bd = BigDecimal((1 - (chargeFull / chargeFullDesign)) * 100)
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
                return bd.toFloat()
            } else if (bmsProvider.isSupportCapcityMode()) {
                val capacityRaw = bmsProvider.getIntData("POWER_SUPPLY_CAPACITY_RAW")
                val capacityStand = bmsProvider.getIntData("POWER_SUPPLY_CAPACITY") * 100
                var bd = BigDecimal((1 - (capacityRaw / capacityStand.toDouble())) * 100)
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
                return bd.toFloat()
            }
        } else if (bmsProvider.isSupportFullChageMode()){
            var bd = BigDecimal(100 - (getRealPercent() / bmsProvider.getIntData("POWER_SUPPLY_CAPACITY").toDouble()) * 100)
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP)
            return bd.toFloat()
        }

        if (bmsProvider.isSupportFullChageMode()) {
            return BATTERY_ONLY_SUPPORT_FULL_MODE
        }
        return BATTERY_LOSS_UNKNOWN
    }
}