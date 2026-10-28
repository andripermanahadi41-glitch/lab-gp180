package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Background & Foreground Service for USB MIDI communication with Valeton GP-180 hardware.
 * Maintains reliable hardware parameter bi-directional sync during live performance or background state.
 */
class UsbMidiService : Service() {

    companion object {
        private const val TAG = "UsbMidiService"
        const val CHANNEL_ID = "gp180_usb_midi_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.action.START_USB_SERVICE"
        const val ACTION_STOP = "com.example.service.action.STOP_USB_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, UsbMidiService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, UsbMidiService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val binder = UsbMidiBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var midiManager: MidiManager? = null
    private var usbMidiDevice: MidiDevice? = null
    private var usbInputPort: MidiInputPort? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("USB MIDI Idle")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    // Incoming MIDI stream: Triple<Status, Data1, Data2>
    private val _incomingMidi = MutableSharedFlow<Triple<Int, Int, Int>>(extraBufferCapacity = 64)
    val incomingMidi: SharedFlow<Triple<Int, Int, Int>> = _incomingMidi.asSharedFlow()

    // Raw incoming log events
    private val _incomingRawBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingRawBytes: SharedFlow<ByteArray> = _incomingRawBytes.asSharedFlow()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            val name = device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: ""
            Log.d(TAG, "USB MIDI Device Added: $name")
            if (!_isConnected.value && (name.contains("Valeton", ignoreCase = true) || name.contains("GP", ignoreCase = true))) {
                connectToDevice(device)
            }
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Log.d(TAG, "USB MIDI Device Removed: ${device.id}")
            if (usbMidiDevice?.info?.id == device.id) {
                disconnectDevice()
            }
        }
    }

    inner class UsbMidiBinder : Binder() {
        fun getService(): UsbMidiService = this@UsbMidiService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        midiManager = getSystemService(Context.MIDI_SERVICE) as? MidiManager

        try {
            midiManager?.registerDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.w(TAG, "Could not register MidiManager DeviceCallback: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                disconnectDevice()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification("Menunggu koneksi USB GP-180...")
                autoConnectFirstAvailable()
            }
            else -> {
                startForegroundWithNotification("Layanan USB MIDI aktif")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startForegroundWithNotification(statusText: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Valeton GP-180 USB MIDI")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Valeton GP-180 USB MIDI")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GP-180 USB MIDI Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan latar belakang sinkronisasi parameter MIDI USB pedal GP-180"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun autoConnectFirstAvailable(onFailed: (String) -> Unit = {}) {
        val manager = midiManager
        if (manager == null) {
            onFailed("Sistem Android MIDI tidak didukung")
            return
        }

        val devices = manager.devices
        if (devices.isEmpty()) {
            _connectedDeviceName.value = "Kabel OTG belum terdeteksi"
            updateNotification("Menunggu kabel USB GP-180 tercolok...")
            onFailed("Perangkat USB MIDI belum tercolok")
            return
        }

        val target = devices.firstOrNull { info ->
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: ""
            name.contains("Valeton", ignoreCase = true) || name.contains("GP", ignoreCase = true)
        } ?: devices.first()

        connectToDevice(target, onFailed)
    }

    fun connectToDevice(targetInfo: MidiDeviceInfo, onFailed: (String) -> Unit = {}) {
        val devName = targetInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB MIDI Device"
        Log.d(TAG, "Opening USB MIDI: $devName")

        midiManager?.openDevice(targetInfo, { device ->
            if (device == null) {
                Log.e(TAG, "Failed to open USB device: $devName")
                onFailed("Gagal membuka USB MIDI")
                return@openDevice
            }

            usbMidiDevice?.close()
            usbMidiDevice = device
            usbInputPort = device.openInputPort(0)

            val outputPort = device.openOutputPort(0)
            outputPort?.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    val raw = msg.copyOfRange(offset, offset + count)
                    processIncomingMidiBytes(raw)
                }
            })

            _isConnected.value = true
            _connectedDeviceName.value = devName
            updateNotification("Terhubung ke $devName (Sync Aktif)")
            Log.d(TAG, "Successfully connected to $devName via USB MIDI")
        }, Handler(Looper.getMainLooper()))
    }

    fun sendMidiBytes(bytes: ByteArray): Boolean {
        return try {
            val port = usbInputPort
            if (port != null && _isConnected.value) {
                port.send(bytes, 0, bytes.size)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending USB MIDI bytes: ${e.message}", e)
            false
        }
    }

    private fun processIncomingMidiBytes(raw: ByteArray) {
        if (raw.isEmpty()) return
        scope.launch {
            _incomingRawBytes.emit(raw)
        }

        var i = 0
        while (i < raw.size) {
            val status = raw[i].toInt() and 0xFF
            if (status in 0x80..0xEF) {
                val type = status and 0xF0
                val data1 = if (i + 1 < raw.size) raw[i + 1].toInt() and 0x7F else 0
                val data2 = if (type == 0xC0 || type == 0xD0) 0 else if (i + 2 < raw.size) raw[i + 2].toInt() and 0x7F else 0
                
                scope.launch {
                    _incomingMidi.emit(Triple(status, data1, data2))
                }
                i += if (type == 0xC0 || type == 0xD0) 2 else 3
            } else {
                i++
            }
        }
    }

    fun disconnectDevice() {
        try {
            usbInputPort?.close()
            usbInputPort = null
            usbMidiDevice?.close()
            usbMidiDevice = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing USB MIDI ports: ${e.message}")
        }
        _isConnected.value = false
        _connectedDeviceName.value = "USB MIDI Terputus"
        updateNotification("USB MIDI Terputus")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            // ignore
        }
        disconnectDevice()
        scope.cancel()
    }
}
