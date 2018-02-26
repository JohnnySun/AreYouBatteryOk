package moe.johnny.areyouok

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by bmy001 on 2/26/2018 AD.
 */
class BmsDataProvider {

    private var bmsRawData : HashMap<String, String> = hashMapOf()
    var needRoot = true

    fun getData() : Boolean{
        try {
            val commandLine = "cat /sys/class/power_supply/bms/uevent"
            var p: Process? = null

            if (needRoot) {
                p = Runtime.getRuntime().exec("su")
                val dos = DataOutputStream(p.outputStream)
                dos.writeBytes(commandLine + "\n")
                dos.writeBytes("exit\n")
                dos.flush()
            } else {
                p = Runtime.getRuntime().exec(commandLine)
            }
            p.waitFor()
            val exitValue = p.exitValue()
            return when (exitValue) {
                0 -> {
                    val reader = BufferedReader(InputStreamReader(p.inputStream))
                    saveData(reader)
                    true
                }
                else -> false
            }
        } catch (e: InterruptedException) {

        } catch (e: IOException) {

        }
        return false
    }

    fun saveData(reader: BufferedReader) {
        var metaData : List<String>
        reader.forEachLine {
            metaData = it.split("=")
            bmsRawData[metaData[0]] = metaData[1]
        }
    }

    fun getIntData(key: String) : Int {
        if (bmsRawData.isEmpty() || bmsRawData[key] == null) {
            return 0
        }
        return bmsRawData[key]!!.toInt()
    }

    fun getStrData(key: String) : String? {
        if (bmsRawData.isEmpty() || bmsRawData[key] == null) {
            return null
        }
        return bmsRawData[key]
    }

    fun isSupportCapcityMode() : Boolean {
        val capaityRaw = getStrData("POWER_SUPPLY_CAPACITY_RAW")
        val capaity = getStrData("POWER_SUPPLY_CAPACITY")
        return !(capaityRaw.isNullOrEmpty() || capaity.isNullOrEmpty())
    }

    fun isSupportFullChageMode() : Boolean {
        val chargeFullDesign = getStrData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
        val chargeNowRaw = getStrData("POWER_SUPPLY_CHARGE_NOW_RAW")
        return !(chargeFullDesign.isNullOrEmpty() || chargeNowRaw.isNullOrEmpty())
    }

    fun isSupportStanderMode() : Boolean {
        val chargeFullDesign = getStrData("POWER_SUPPLY_CHARGE_FULL_DESIGN")
        val chargeFull = getStrData("POWER_SUPPLY_CHARGE_FULL")
        return (!(chargeFullDesign.isNullOrEmpty() || chargeFull.isNullOrEmpty()) &&
                chargeFullDesign != chargeFull)
    }

}