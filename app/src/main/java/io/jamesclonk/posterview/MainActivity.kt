package io.jamesclonk.posterview

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.Manifest
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import io.jamesclonk.posterview.databinding.PosterviewFullscreenBinding
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: PosterviewFullscreenBinding
    private lateinit var fullscreenContent: ImageView
    private val mp = MediaPlayer()
    private lateinit var currentImage: File

    @SuppressLint("InlinedApi")
    private fun hide() {
        // removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PosterviewFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.hide()

        fullscreenContent = binding.posterImage
        //fullscreenContent.setOnClickListener { playSound() }

        hide()
        backgroundSwitcher()
    }

    override fun onDestroy() {
        mp.release()
        super.onDestroy()
    }

    private fun backgroundSwitcher() {
        switchRandomImage()
        Thread(
            Runnable {
                while(true) {
                    Thread.sleep(1000 * 60 * 10) // switch image every 10 minutes
                    runOnUiThread { switchRandomImage() }
                }
            }
        ).start()
    }

    private fun getRandomImageFile(): File {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)

        //val path = File("/storage/emulated/0/Posters/")
        val path = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Posters")

        val files = path.walkTopDown().filter { file ->
            file.isFile && (file.extension == "png" || file.extension == "jpg")
        }.shuffled().toList()

        val randomIndex = Random.nextInt(files.size);
        return files[randomIndex]
    }

    private fun getSoundFile(imageFile: File): File {
        return File(imageFile.parentFile.absolutePath + File.separator + imageFile.nameWithoutExtension + ".mp3")
    }

    fun switchImage(view: View) {
        // stop music if playing, we've triggered an on-demand image switch
        if(mp.isPlaying) {
            mp.stop()
            mp.reset()
        }

        switchRandomImage()
    }

    private fun switchRandomImage() {
        if(!mp.isPlaying) { // only switch image if there's currently no music playing
            currentImage = getRandomImageFile()
            fullscreenContent.setImageBitmap(BitmapFactory.decodeFile(currentImage.absolutePath))
        }
    }

    fun playSound(view: View) {
        val soundFile = getSoundFile(currentImage)
        if(!soundFile.exists()) {
            return
        }

        if(mp.isPlaying) {
            mp.stop()
            mp.reset()
        } else {
            mp.setDataSource(soundFile.absolutePath);
            mp.prepare()
            mp.start()
        }
    }

    /**
     * Recursively list files from a given directory.
     */
    private fun listFiles(directory: File) {
        Log.d("dir", directory.absolutePath)

        val files = directory.listFiles()
        if (files != null) {
            Log.d("files.size", files.size.toString())

            for (file in files) {
                if (file != null) {
                    if (file.isDirectory) {
                        listFiles(file)
                    } else {
                        Log.d("file", file.absolutePath)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}