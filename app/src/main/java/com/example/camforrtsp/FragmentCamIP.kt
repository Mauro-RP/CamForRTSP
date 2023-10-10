package com.example.camforrtsp

import android.annotation.SuppressLint
import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.camforrtsp.R.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class FragmentCamIP : Fragment() {

    private lateinit var videoView: VLCVideoLayout
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var ipAddressEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

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
            * ejemplos de ip:
            * usuario: admin
            * pass: admin12345
            * ip: 192.168.100.33
            * rtsp://admin:admin12345@192.168.100.33:554/h264/ch1/main/av_stream
            * rtsp://admin:admin12345@192.168.100.33:554/profile
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

        return rootView
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
