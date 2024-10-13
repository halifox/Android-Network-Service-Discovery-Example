package com.example.app

import android.annotation.SuppressLint
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.ActivityMainBinding
import com.example.app.databinding.ItemLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@SuppressLint("HardwareIds")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val nsdManager by lazy { getSystemService<NsdManager>()!! }
    private val deviceAdapter by lazy { DeviceAdapter(nsdManager) }
    private val serviceInfos = mutableListOf<NsdServiceInfo>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.devices.adapter = deviceAdapter
        binding.devices.layoutManager = LinearLayoutManager(this)

        discoverServices()
        repeat(10) {
            registerService()
        }
    }

    class DeviceAdapter(val nsdManager: NsdManager) : ListAdapter<NsdServiceInfo, DeviceAdapter.ViewHolder>(DIFF_CALLBACK) {
        companion object {
            private val TAG = "DeviceAdapter"
            private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NsdServiceInfo>() {
                override fun areItemsTheSame(oldItem: NsdServiceInfo, newItem: NsdServiceInfo) = true
                override fun areContentsTheSame(oldItem: NsdServiceInfo, newItem: NsdServiceInfo) = oldItem.serviceName == newItem.serviceName
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item)
        }

        inner class ViewHolder(private val binding: ItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
            fun toString(serviceInfo: NsdServiceInfo?): String {
                return """
                        name:${serviceInfo?.serviceName}
                        type:${serviceInfo?.serviceType}
                        host:${serviceInfo?.host}
                        port:${serviceInfo?.port}
                    """.trimIndent()
            }

            fun bind(serviceInfo: NsdServiceInfo) {
                binding.content.text = toString(serviceInfo)
                binding.resolve.setOnClickListener {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            Log.d(TAG, "onResolveFailed:${serviceInfo} ${errorCode} ")
                            MainScope().launch(Dispatchers.Main) {
                                binding.content.text = toString(serviceInfo)
                            }
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.d(TAG, "onServiceResolved:${serviceInfo} ")
                            MainScope().launch(Dispatchers.Main) {
                                binding.content.text = toString(serviceInfo)
                            }
                        }
                    })
                }
            }
        }
    }


    private fun discoverServices() {
        val discoveryListener = object : DiscoveryListener {
            private val TAG = "DiscoveryListener"
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(TAG, "onStartDiscoveryFailed:${serviceType} ${errorCode}")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.d(TAG, "onStopDiscoveryFailed:${serviceType} ${errorCode}")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "onDiscoveryStarted:${serviceType} ")

            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "onDiscoveryStopped:${serviceType} ")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "onServiceFound:${serviceInfo} ")
                lifecycleScope.launch(Dispatchers.Main) {
                    if (serviceInfos.none { it.serviceName == serviceInfo.serviceName }) {
                        serviceInfos.add(serviceInfo)
                        deviceAdapter.submitList(serviceInfos)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "onServiceLost:${serviceInfo} ")
                lifecycleScope.launch(Dispatchers.Main) {
                    serviceInfos.removeIf { it.serviceName == serviceInfo.serviceName }
                    deviceAdapter.submitList(serviceInfos)
                }
            }
        }
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                nsdManager.stopServiceDiscovery(discoveryListener)
            }
        })
    }

    private fun registerService() {
        //在 Android 的 NSD（Network Service Discovery）中，`setServiceName`、`setServiceType` 和 `setPort` 方法有以下要求：
        //
        //### 1. `setServiceName(String serviceName)`
        //- **要求**:
        //  - **唯一性**: 服务名称必须在同一网络中的同一服务类型下唯一。
        //  - **字符限制**: 最好使用 0 到 15 个字符（可接受的最大字符数），且不能包含特殊字符（如空格和特殊符号）。
        //  - **示例**: 可以使用字母和数字，如 "MyService"。
        //
        //### 2. `setServiceType(String serviceType)`
        //- **要求**:
        //  - **格式**: 必须以下划线 `_` 开头，后面可以包含字母、数字和下划线，通常由服务的协议和类型组成。
        //  - **注册服务类型**: 使用 `_http._tcp.`、`_ftp._tcp.` 等标准类型，确保使用正确的服务类型。
        //  - **示例**: 常用的服务类型包括：
        //    - HTTP 服务: `_http._tcp.`
        //    - FTP 服务: `_ftp._tcp.`
        //    - 自定义服务: `_mycustomservice._tcp.`
        //
        //### 3. `setPort(int port)`
        //- **要求**:
        //  - **有效端口**: 端口号必须在 1 到 65535 之间。
        //  - **使用中的端口**: 确保所指定的端口未被其他服务占用，避免端口冲突。
        //  - **示例**: 常用的端口如 80（HTTP）或 443（HTTPS），但可以使用任意未被占用的端口。
        //
        //确保遵循这些要求，以便 NSD 能够成功注册和发现服务。
        val serviceInfo = NsdServiceInfo().apply {
//            serviceName = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            serviceName = "NSD_${Random.nextInt(10000, 60000)}"
            serviceType = "_http._tcp."
            port = Random.nextInt(10000, 60000)
        }
        val registrationListener = object : RegistrationListener {
            private val TAG = "RegistrationListener${serviceInfo.serviceName}"
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.d(TAG, "Registration Failed ${serviceInfo} ${errorCode}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.d(TAG, "UnRegistration Failed ${serviceInfo} ${errorCode}")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service Registered ${serviceInfo}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service Unregistered ${serviceInfo}")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                nsdManager.unregisterService(registrationListener)
            }
        })
    }
}
