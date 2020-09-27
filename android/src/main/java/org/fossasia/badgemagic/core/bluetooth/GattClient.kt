package org.fossasia.badgemagic.core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import java.util.LinkedList
import org.fossasia.badgemagic.core.android.log.Timber
import org.fossasia.badgemagic.data.DataToSend
import org.fossasia.badgemagic.util.BadgeUtils
import org.fossasia.badgemagic.utils.ByteArrayUtils
import org.koin.core.KoinComponent
import org.koin.core.inject

class GattClient : KoinComponent {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var onConnectedListener: ((Boolean) -> Unit)? = null
    private var onFinishWritingDataListener: (() -> Unit)? = null

    private val messagesToSend = LinkedList<ByteArray>()

    private val badgeUtils: BadgeUtils by inject()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.i { "Connected to GATT client. Attempting to start service discovery" }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.i { "Disconnected from GATT client" }
                stopClient()
                onConnectedListener?.invoke(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onConnectedListener?.invoke(true)
            } else {
                Timber.w { "onServicesDiscovered received: $status" }
                stopClient()
                onConnectedListener?.invoke(false)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Timber.i { "onCharacteristicWrite" }
            Thread.sleep(100)
            writeNextData()
        }
    }

    fun convert(data: DataToSend): List<ByteArray> {
        return badgeUtils.currentDevice.convert(data)
    }

    fun writeDataStart(byteData: List<ByteArray>, onFinishWritingDataListener: () -> Unit) {
        this.onFinishWritingDataListener = onFinishWritingDataListener
        messagesToSend.addAll(byteData)
        writeNextData()
    }

    fun writeNextData() {
        if (messagesToSend.isEmpty()) {
            onFinishWritingDataListener?.invoke()
        } else {
            val data = messagesToSend.pop()
            Timber.e { "Writing: ${ByteArrayUtils.byteArrayToHexString(data)}" }

            val characteristic = bluetoothGatt
                ?.getService(badgeUtils.currentDevice.serviceID)
                ?.getCharacteristic(badgeUtils.currentDevice.characteristicsID)
            characteristic?.value = data
            if (characteristic != null)
                bluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    fun startClient(context: Context, deviceAddress: String, onConnectedListener: (Boolean) -> Unit) {
        this.onConnectedListener = onConnectedListener
        bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)

        if (bluetoothGatt == null) {
            Timber.w { "Unable to create GATT client" }
            return
        }
    }

    fun stopClient() {
        if (bluetoothGatt != null) {
            bluetoothGatt?.close()
            bluetoothGatt = null
        }

        if (bluetoothAdapter != null) {
            bluetoothAdapter = null
        }
    }
}
