package com.example.camforrtsp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.camforrtsp.R.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class FragmentCamIP : Fragment() {

    private lateinit var videoView: VLCVideoLayout
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var ipAddressEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var qrCodeTextView: TextView

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView



    @SuppressLint("AuthLeak", "MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(layout.fragment_cam_i_p, container, false)

        videoView = rootView.findViewById(R.id.videoView)
        val playButton = rootView.findViewById<Button>(R.id.btnPlay)
        ipAddressEditText = rootView.findViewById(R.id.ipAddressEditText)
        usernameEditText = rootView.findViewById(R.id.usernameEditText)
        passwordEditText = rootView.findViewById(R.id.passwordEditText)
        qrCodeTextView = rootView.findViewById(R.id.qrCodeTextView)

        // Configuracion libVLC
        val options = ArrayList<String>()
        options.add("--no-audio")
        options.add("--rtsp-tcp")
        libVLC = LibVLC(requireContext(), options)
        mediaPlayer = MediaPlayer(libVLC)

        playButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            /*
            * usuario: admin
            * pass: admin12345
            * ip: 192.168.100.33
            * rtsp://admin:admin12345@192.168.100.33:554/h264/ch1/main/av_stream
            * rtsp://admin:admin12345@192.168.100.33:554/profile1

            val ipAddress = "192.168.100.35"
            val username = "admin"
            val password = "Camara.1"
            */


            if (ipAddress.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                val rtspUrl = "rtsp://$username:$password@$ipAddress:554/profile1"

                // Configurar y reproducir el video
                val media = Media(libVLC, Uri.parse(rtspUrl))
                mediaPlayer.media = media
                media.release()
                mediaPlayer.attachViews(videoView, null, false, false)

                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.play()
                } else {
                    mediaPlayer.stop()
                    mediaPlayer.detachViews()
                }
            } else {
                // En caso en que los campos estén vacíos
                val errorMessage = "Por favor, complete todos los campos."
                showToast(errorMessage)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        previewView = rootView.findViewById(R.id.cameraPreview)

        // Inicializa CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor(), QRCodeAnalyzer({ barcodes ->
                        // Manejar los códigos de barras detectados
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { it1 -> showToast(it1) }
                        }
                    },  requireView()))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                // Manejar errores
            }

        }, ContextCompat.getMainExecutor(requireContext()))

        return rootView
    }

    private class QRCodeAnalyzer(private val listener: (List<Barcode>) -> Unit, private val rootView: View) : ImageAnalysis.Analyzer {
        private val barcodeScanner = BarcodeScanning.getClient()

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image

            if (mediaImage != null) {

                // Obtener las dimensiones y la posición del área de interés
                val areaOfInterestWidth = 150
                val areaOfInterestHeight = 150
                val areaOfInterestX = 200
                val areaOfInterestY = 350

                // Obtener el View que representa el recuadro superpuesto

                val qrCaptureFrame: View = rootView.findViewById(R.id.qrCaptureFrame)

                // Ajustar dinámicamente las propiedades del View para que coincida con el área de interés
                qrCaptureFrame.layoutParams.width = areaOfInterestWidth
                qrCaptureFrame.layoutParams.height = areaOfInterestHeight
                qrCaptureFrame.x = areaOfInterestX.toFloat()
                qrCaptureFrame.y = areaOfInterestY.toFloat()
                qrCaptureFrame.requestLayout()

                // Obtener los datos de píxeles de la imagen
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                // Crear un mapa de bits a partir de los datos de píxeles
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

                // Recortar el mapa de bits
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width / 2, bitmap.height / 4)

                // Crear una instancia de InputImage a partir del mapa de bits recortado
                val inputImage = InputImage.fromBitmap(croppedBitmap, image.imageInfo.rotationDegrees)
                //val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        // Manejar los códigos de barras detectados
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { rawValue ->
                                listener(barcodes)
                                (rootView.context as? FragmentCamIP)?.updateQRCodeTextView(rawValue)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            }
        }

    }

    private fun updateQRCodeTextView(qrCode: String) {
        qrCodeTextView.text = qrCode
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        // No detenemos la reproducción de video aquí
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Al salir de la vista del fragmento, se detiene la reproducción de video
        if (mediaPlayer.isPlaying) {
            mediaPlayer.detachViews()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        mediaPlayer.stop()
        libVLC.release()
    }
}

