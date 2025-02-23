package com.igrik.cpu_reader

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** CpuReaderPlugin */
class CpuReaderPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var cpuProvider: CpuDataProvider
    private lateinit var cache: HashMap<String, Any>

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "cpu_reader")
        channel.setMethodCallHandler(this)
        cpuProvider = CpuDataProvider()
        cache = hashMapOf()
    }

    private fun getCpuInfo(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val abi = cache.getOrPut("abi") { cpuProvider.getAbi() } as String
        val cores = cache.getOrPut("cores") { cpuProvider.getNumberOfCores() } as Int
        val minMaxFrequencies = cache.getOrPut("minMaxFrequencies") {
            val minMax = mutableMapOf<Int, Map<String, Long>>()
            for (i in 0 until cores) {
                val values = cpuProvider.getMinMaxFreq(i)
                minMax[i] = mapOf("min" to values.first, "max" to values.second)
            }
            minMax
        } as MutableMap<Int, Map<String, Long>>

        val cpuTemperature = cpuProvider.getCpuTemperature()
        val currentFrequencies = mutableMapOf<Int, Long>()
        for (i in 0 until cores) {
            currentFrequencies[i] = cpuProvider.getCurrentFreq(i)
        }

        map["abi"] = abi
        map["numberOfCores"] = cores
        map["minMaxFrequencies"] = minMaxFrequencies
        map["currentFrequencies"] = currentFrequencies
        map["cpuTemperature"] = cpuTemperature

        return map
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getAbi" -> result.success(cpuProvider.getAbi())
            "getNumberOfCores" -> result.success(cpuProvider.getNumberOfCores())
            "getCurrentFrequency" -> {
                val coreNumber = call.argument<Int>("coreNumber") ?: 0
                result.success(cpuProvider.getCurrentFreq(coreNumber))
            }
            "getMinMaxFrequencies" -> {
                val coreNumber = call.argument<Int>("coreNumber") ?: 0
                val pair = cpuProvider.getMinMaxFreq(coreNumber)
                result.success(mapOf("min" to pair.first, "max" to pair.second))
            }
            "getCpuTemperature" -> result.success(cpuProvider.getCpuTemperature())
            "getCpuInfo" -> result.success(getCpuInfo())
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
