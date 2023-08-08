package com.example.beaconDemo

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

import com.example.beaconDemo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var beaconListView: ListView
    lateinit var beaconApplication: BeaconApplication
    lateinit var dexter : DexterBuilder
    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        beaconApplication = application as BeaconApplication


        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(beaconApplication.region)

        getPermission()

        regionViewModel.regionState.observe(this, monitoringObserver)

        regionViewModel.rangedBeacons.observe(this, rangingObserver)


        beaconListView = findViewById<ListView>(R.id.beaconList)

        beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))

    }


    val monitoringObserver = Observer<Int> { state ->
        var dialogTitle = "Beacons detected"
        if (state == MonitorNotifier.OUTSIDE) {
            dialogTitle = "No beacons detected"

            beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
        }
        else {

        }

        Toast.makeText(this,dialogTitle,Toast.LENGTH_LONG).show()
    }

    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.size > 0) {


            if(beacons.count() == 0){
                binding.beaconList.visibility = View.GONE
                binding.txtNoBeacons.visibility = View.VISIBLE
            }else{
                binding.beaconList.visibility = View.VISIBLE
                binding.txtNoBeacons.visibility = View.GONE
            }

            beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                beacons
                    .sortedBy { it.distance }
                    .map { "UUID: ${it.id1}\nMajor: ${it.id2}\nMinor ${it.id3}\n" }.toTypedArray())
        }
    }



    private fun getPermission() {
        dexter = Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    report.let {

                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(this@MainActivity, "Permissions Granted", Toast.LENGTH_SHORT).show()
                        } else {
                            AlertDialog.Builder(this@MainActivity, R.style.Theme_AppCompat_Dialog).apply {
                                setMessage("please allow the required permissions")
                                    .setCancelable(false)
                                    .setPositiveButton("Settings") { _, _ ->
                                        val reqIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .apply {
                                                val uri = Uri.fromParts("package", packageName, null)
                                                data = uri
                                            }
                                        resultLauncher.launch(reqIntent)
                                    }
                                val alert = this.create()
                                alert.show()
                            }
                        }
                    }
                }


                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {

                    token?.continuePermissionRequest()
                }
            }).withErrorListener{
                Toast.makeText(this, it.name, Toast.LENGTH_SHORT).show()
            }
        dexter.check()
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> dexter.check()
    }

}
