package hu.petrik.nextgenqrbarat

import android.Manifest
import android.content.ContentValues
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.zxing.integration.android.IntentIntegrator
import hu.petrik.nextgenqrbarat.databinding.ActivityMainBinding
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var bind: ActivityMainBinding
    //SRY FOR THESE UNINTENTIONAL COMMENTS, but File writing is working on API level 30+
    //The catch was that accessing the file tree is different now, so is the app
    //It was not a bonus exercise, I just hate to work blindfolded, and now you're amazed aren't you?
    //Fun fact, if API level 30+, it'll be in the downloads folder, for no actual reason

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        bind.scanButton.setOnClickListener {
            integrateIntent()
        }

        bind.writeButton.setOnClickListener {
            tryToWrite()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString(this.getString(R.string.result_key), bind.textResult.text.toString())
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.containsKey(this.getString(R.string.result_key))) {
            bind.textResult.text = savedInstanceState.getString(this.getString(R.string.result_key))
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, this.getString(R.string.exit_Scan), Toast.LENGTH_SHORT).show()
            } else {
                bind.textResult.text = result.contents
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun tryToWrite() {
        val text = bind.textResult.text
        if (text === null || text === "") {
            Toast.makeText(this, this.getString(R.string.no_data), Toast.LENGTH_SHORT).show()
            return
        }
        if (hasPermissions()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, 0)
            if (this.hasPermissions()) {
                fileWrite(text as String)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileWriteForGodsSake(text as String)
            }
            else{
                Toast.makeText(this, this.getString(R.string.inactive), Toast.LENGTH_SHORT).show()
                requestNeededPermissions()
            }
        }
    }

    fun integrateIntent() {
        val intentIntegrator = IntentIntegrator(this@MainActivity)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.setPrompt(this.getString(R.string.QR_Tipp))
        intentIntegrator.setCameraId(0)
        intentIntegrator.setBeepEnabled(true) //Beeping makes it more "elegant" 😎
        intentIntegrator.setBarcodeImageEnabled(true)
        intentIntegrator.initiateScan()
    }

    fun fileWrite(content: String) {
        val file =
            File(Environment.getExternalStorageDirectory(), this.getString(R.string.file_name))
        val bf = BufferedWriter(FileWriter(file, true))
        val date = Calendar.getInstance().time as Date
        val dateformat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateformat.format(date)
        val text = "$content,$formattedDate;"
        bf.append(text)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        bf.append(System.lineSeparator())
        bf.close()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun fileWriteForGodsSake(content:String){
        val resolver = applicationContext.contentResolver
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, this.getString(R.string.file_name))
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS + "/" + this.getString(R.string.app_name)
        )
        val outPutStream = resolver.openOutputStream(resolver.insert(MediaStore.Files.getContentUri("external"),values)!!)!!
        val bf = outPutStream.bufferedWriter()
        val date = Calendar.getInstance().time as Date
        val dateformat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateformat.format(date)
        val text = "$content,$formattedDate;"
        bf.append(text)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        bf.append(System.lineSeparator())
        bf.close()
    }

    fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNeededPermissions() {
        val request = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, request, 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            if (!this.hasPermissions()) {
                Toast.makeText(this, this.getString(R.string.inactive), Toast.LENGTH_SHORT).show()
                requestNeededPermissions()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    //PS, don't open that QR, or face the consequences 👀
}