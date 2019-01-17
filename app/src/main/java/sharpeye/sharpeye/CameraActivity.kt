package sharpeye.sharpeye

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.*
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.app_bar_camera.*
import kotlinx.android.synthetic.main.content_camera.*
import java.util.*

class CameraActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val CAMERA_PERMISSIONS = 42

    private var cameraDevice: CameraDevice? = null

    private var previewSize: Size? = null

    private var previewBuilder: CaptureRequest.Builder? = null
    private var previewSession: CameraCaptureSession? = null


    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e("CameraError", "error code $error")
            camera.close()
        }

        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startPreview()
        }
    }

    private fun hasPermissionsGranted(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setMessage(R.string.camera_permission)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSIONS)
                }
                .setNegativeButton(android.R.string.cancel) { _,_ ->
                    finish()
                }
                .create().show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSIONS)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (!hasPermissionsGranted(android.Manifest.permission.CAMERA)) {
            requestCameraPermission()
            return
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            var cameraId = manager.cameraIdList[0]
            for (id in manager.cameraIdList) {
                val chars: CameraCharacteristics = manager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            manager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: CameraAccessException) {
            // TODO Do something dude !
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        /** Set the status bar transparent */
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (cameraView.isAvailable) {
            openCamera()
        } else {
            cameraView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        super.onPause()
        cameraDevice?.close()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.camera, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_danger -> {
                // Handle the camera action
            }
            R.id.nav_signs -> {

            }
            R.id.nav_assist -> {

            }
            R.id.nav_dashcam -> {

            }
            R.id.nav_about -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    internal fun startPreview() {
        if (cameraDevice == null || !cameraView.isAvailable || previewSize == null) {
            return
        }
        val texture = cameraView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        val surface = Surface(texture)
        try {
            previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: Exception) {
            // TODO Do something for this exception
        }

        previewBuilder?.addTarget(surface)
        try {
            cameraDevice?.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    previewSession = session
                    getChangedPreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        } catch (e: Exception) {
            // TODO Do something for this exception
        }
    }

    internal fun getChangedPreview() {
        if (cameraDevice == null) {
            return
        }
        previewBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val thread = HandlerThread("changed Preview")
        thread.start()
        val handler = Handler(thread.looper)
        try {
            previewSession?.setRepeatingRequest(previewBuilder!!.build(), null, handler)
        } catch (e: Exception) {
            // TODO Do something for this exception
        }
    }

}
