package com.example.iot.ui.provision

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.iot.R
import com.example.iot.databinding.FragmentBleProvisionBinding
import com.espressif.provisioning.DeviceConnectionEvent
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import com.espressif.provisioning.listeners.ProvisionListener
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class BleProvisionFragment : Fragment() {

    private var _b: FragmentBleProvisionBinding? = null
    private val b get() = _b!!

    private lateinit var provisionManager: ESPProvisionManager
    private var espDevice: ESPDevice? = null

    private val devices = mutableListOf<FoundDevice>()
    private lateinit var adapter: ArrayAdapter<String>
    private var selected: FoundDevice? = null

    private var pendingAction: (() -> Unit)? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val ok = requiredPermissions().all { grants[it] == true }
            if (ok) {
                pendingAction?.invoke()
            } else {
                setStatus("Thiếu quyền Bluetooth/Location, không thể quét BLE.")
            }
            pendingAction = null
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isBluetoothEnabled()) {
                pendingAction?.invoke()
            } else {
                setStatus("Bluetooth chưa bật.")
            }
            pendingAction = null
        }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBleProvisionBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val toolbar = b.topBar.root
        toolbar.title = "Provision Wi‑Fi"
        toolbar.subtitle = "BLE provisioning"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        provisionManager = ESPProvisionManager.getInstance(requireContext())

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_single_choice)
        b.listDevices.adapter = adapter

        b.listDevices.setOnItemClickListener { _, _, position, _ ->
            val found = devices.getOrNull(position) ?: return@setOnItemClickListener
            connect(found)
        }

        b.btnScan.setOnClickListener {
            ensureBleReady { startScan() }
        }

        b.btnProvision.setOnClickListener {
            ensureBleReady { provision() }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        stopScan()
        espDevice?.disconnectDevice()
        espDevice = null
        selected = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDeviceConnectionEvent(event: DeviceConnectionEvent) {
        when (event.eventType) {
            ESPConstants.EVENT_DEVICE_CONNECTED -> {
                setStatus("Đã kết nối BLE. Có thể gửi Wi‑Fi.")
            }
            ESPConstants.EVENT_DEVICE_DISCONNECTED -> {
                setStatus("Thiết bị đã ngắt kết nối.")
            }
            ESPConstants.EVENT_DEVICE_CONNECTION_FAILED -> {
                setStatus("Kết nối thất bại.")
            }
        }
    }

    private fun startScan() {
        stopScan()
        devices.clear()
        adapter.clear()
        selected = null
        espDevice?.disconnectDevice()
        espDevice = null

        setSelected(null)
        setStatus("Đang quét BLE…")

        try {
            provisionManager.searchBleEspDevices(DEVICE_NAME_PREFIX, object : BleScanListener {
                override fun scanStartFailed() {
                    setStatus("Không thể bắt đầu quét. Hãy bật Bluetooth.")
                }

                override fun onPeripheralFound(device: android.bluetooth.BluetoothDevice, scanResult: android.bluetooth.le.ScanResult) {
                    val serviceUuid = scanResult.scanRecord?.serviceUuids?.firstOrNull()?.toString().orEmpty()
                    val name = scanResult.scanRecord?.deviceName ?: device.name ?: "PROV"

                    if (devices.any { it.address == device.address }) return
                    devices.add(
                        FoundDevice(
                            name = name,
                            address = device.address,
                            device = device,
                            primaryServiceUuid = serviceUuid
                        )
                    )
                    adapter.add("$name (${device.address})")
                    adapter.notifyDataSetChanged()
                }

                override fun scanCompleted() {
                    if (devices.isEmpty()) {
                        setStatus("Không tìm thấy thiết bị PROV_.")
                    } else {
                        setStatus("Đã tìm thấy ${devices.size} thiết bị. Chọn 1 thiết bị để kết nối.")
                    }
                }

                override fun onFailure(e: Exception) {
                    setStatus("Quét thất bại: ${e.message ?: e.javaClass.simpleName}")
                }
            })
        } catch (t: Throwable) {
            setStatus("Quét thất bại: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun stopScan() {
        try {
            provisionManager.stopBleScan()
        } catch (_: Throwable) {
        }
    }

    private fun connect(found: FoundDevice) {
        if (found.primaryServiceUuid.isBlank()) {
            Toast.makeText(requireContext(), "Thiếu Service UUID. Hãy quét lại.", Toast.LENGTH_SHORT).show()
            return
        }

        stopScan()

        selected = found
        setSelected(found)
        setStatus("Đang kết nối BLE…")

        espDevice?.disconnectDevice()
        espDevice = provisionManager.createESPDevice(
            ESPConstants.TransportType.TRANSPORT_BLE,
            ESPConstants.SecurityType.SECURITY_0
        ).apply {
            setDeviceName(found.name)
        }

        espDevice?.connectBLEDevice(found.device, found.primaryServiceUuid)
    }

    private fun provision() {
        val found = selected
        val device = espDevice
        if (found == null || device == null) {
            Toast.makeText(requireContext(), "Chọn thiết bị trước.", Toast.LENGTH_SHORT).show()
            return
        }

        val ssid = b.edtSsid.text?.toString().orEmpty().trim()
        val pass = b.edtPassword.text?.toString().orEmpty()
        if (ssid.isBlank()) {
            Toast.makeText(requireContext(), "Nhập SSID.", Toast.LENGTH_SHORT).show()
            return
        }

        setStatus("Đang gửi Wi‑Fi…")

        device.provision(ssid, pass, object : ProvisionListener {
            override fun createSessionFailed(e: Exception) {
                setStatus("Tạo session thất bại: ${e.message ?: e.javaClass.simpleName}")
            }

            override fun wifiConfigSent() {
                setStatus("Đã gửi Wi‑Fi credentials.")
            }

            override fun wifiConfigFailed(e: Exception) {
                setStatus("Gửi Wi‑Fi thất bại: ${e.message ?: e.javaClass.simpleName}")
            }

            override fun wifiConfigApplied() {
                setStatus("Thiết bị đã áp dụng Wi‑Fi, đang kết nối mạng…")
            }

            override fun wifiConfigApplyFailed(e: Exception) {
                setStatus("Áp dụng Wi‑Fi thất bại: ${e.message ?: e.javaClass.simpleName}")
            }

            override fun provisioningFailedFromDevice(failureReason: ESPConstants.ProvisionFailureReason) {
                setStatus("Provision thất bại từ thiết bị: $failureReason")
            }

            override fun deviceProvisioningSuccess() {
                setStatus("Provision thành công. ESP32 sẽ lưu Wi‑Fi vào NVS.")
            }

            override fun onProvisioningFailed(e: Exception) {
                setStatus("Provision thất bại: ${e.message ?: e.javaClass.simpleName}")
            }
        })
    }

    private fun ensureBleReady(action: () -> Unit) {
        if (!hasPermissions()) {
            pendingAction = action
            permissionLauncher.launch(requiredPermissions())
            return
        }
        if (!isBluetoothEnabled()) {
            pendingAction = action
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        action()
    }

    private fun hasPermissions(): Boolean =
        requiredPermissions().all { p ->
            ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isBluetoothEnabled(): Boolean =
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    private fun setSelected(found: FoundDevice?) {
        b.txtSelected.text = when (found) {
            null -> "Chưa chọn thiết bị"
            else -> "Đã chọn: ${found.name} (${found.address})"
        }
    }

    private fun setStatus(msg: String) {
        b.txtStatus.text = "Trạng thái: $msg"
    }

    private data class FoundDevice(
        val name: String,
        val address: String,
        val device: android.bluetooth.BluetoothDevice,
        val primaryServiceUuid: String
    )

    private companion object {
        private const val DEVICE_NAME_PREFIX = "PROV_"
    }
}

