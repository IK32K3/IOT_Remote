package com.example.iot.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.data.local.RemoteProfile
import com.example.iot.databinding.FragmentSaveRemoteBinding
import com.example.iot.domain.usecase.SaveRemoteUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SaveRemoteFragment : Fragment() {
    private var _b: FragmentSaveRemoteBinding? = null
    private val b get() = _b!!

    @Inject lateinit var save: SaveRemoteUseCase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentSaveRemoteBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getString("type").orEmpty()
        val brand = arguments?.getString("brand").orEmpty()
        val index = arguments?.getInt("index") ?: 1
        val nodeId = arguments?.getString("nodeId").orEmpty()

        b.inputName.setText("$brand $type")
        b.btnSave.setOnClickListener {
            val name = b.inputName.text?.toString().orEmpty()
            val room = b.inputRoom.text?.toString().orEmpty()
            val deviceType = when (type.lowercase()) {
                "tv", "tivi" -> "TV"
                "fan", "quạt điện" -> "FAN"
                "ac", "máy lạnh" -> "AC"
                "stb", "stb/sat" -> "STB"
                else -> "AC"
            }
            viewLifecycleOwner.lifecycleScope.launch {
                save(
                    RemoteProfile(
                        name = name.ifBlank { "$brand $type" },
                        room = room.ifBlank { "Mặc định" },
                        brand = brand,
                        type = type,
                        nodeId = nodeId,
                        codeSetIndex = index,
                        deviceType = deviceType   // 👈 thêm dòng này
                    )
                )
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
