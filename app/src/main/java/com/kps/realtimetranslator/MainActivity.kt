package com.kps.realtimetranslator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Pair
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.kps.realtimetranslator.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var mBinding: ActivityMainBinding
    private var mSelectedImage: Bitmap? = null

    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null

    /**
     * Number of results to show in the UI.
     */
    private val RESULTS_TO_SHOW = 3

    /**
     * Dimensions of inputs.
     */
    private val DIM_BATCH_SIZE = 1
    private val DIM_PIXEL_SIZE = 3
    private val DIM_IMG_SIZE_X = 224
    private val DIM_IMG_SIZE_Y = 224
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.buttonText.setOnClickListener(View.OnClickListener { runTextRecognition() })
        val dropdown = findViewById<Spinner>(R.id.spinner)
        val items = arrayOf("Test Image 1 (Text)", "Test Image 2 (Face)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        dropdown.setOnItemSelectedListener(this)
    }

    private fun runTextRecognition() {
        val image = InputImage.fromBitmap(mSelectedImage!!, 0)
        val recognizer = TextRecognition.getClient()
        mBinding.buttonText.setEnabled(false)
        recognizer.process(image)
            .addOnSuccessListener { texts ->
                mBinding.buttonText.setEnabled(true)
                processTextRecognitionResult(texts)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                mBinding.buttonText.setEnabled(true)
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(texts: Text) {
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            showToast("No text found")
            return
        }
        mBinding.graphicOverlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic: GraphicOverlay.Graphic = TextGraphic(mBinding.graphicOverlay, elements[k])
                    mBinding.graphicOverlay.add(textGraphic)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        mBinding.graphicOverlay.clear()
        mSelectedImage = when (position) {
            0 -> getBitmapFromAsset(this, "Please_walk_on_the_grass.jpg")
            1 -> getBitmapFromAsset(this, "grace_hopper.jpg")
            // Whatever you want to happen when the thrid item gets selected
            else -> null
        }
        if (mSelectedImage != null) {
            // Get the dimensions of the View
            val targetedSize: Pair<Int, Int> = getTargetedWidthHeight()
            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            mSelectedImage?.let {
                // Determine how much to scale down the image
                val scaleFactor = Math.max(
                    it.width.toFloat() / targetWidth.toFloat(),
                    it.height.toFloat() / maxHeight.toFloat()
                )
                val resizedBitmap = Bitmap.createScaledBitmap(
                    mSelectedImage!!,
                    (it.width / scaleFactor).toInt(),
                    (it.height / scaleFactor).toInt(),
                    true
                )
                mBinding.imageView.setImageBitmap(resizedBitmap)
                mSelectedImage = resizedBitmap
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing
    }

    // Gets the targeted width / height.
    private fun getTargetedWidthHeight(): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode: Int = getImageMaxWidth()!!
        val maxHeightForPortraitMode: Int = getImageMaxHeight()!!
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }

    // Functions for loading images from app assets.

    // Functions for loading images from app assets.
    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxWidth(): Int? {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = mBinding.imageView.width
        }
        return mImageMaxWidth
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxHeight(): Int? {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight = mBinding.imageView.height
        }
        return mImageMaxHeight
    }

    companion object {
        fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
            val assetManager = context.assets
            val `is`: InputStream
            var bitmap: Bitmap? = null
            try {
                `is` = assetManager.open(filePath!!)
                bitmap = BitmapFactory.decodeStream(`is`)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return bitmap
        }
    }
}