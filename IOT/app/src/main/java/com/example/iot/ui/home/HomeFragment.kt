package com.example.iot.ui.home


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.databinding.FragmentHomeBinding
import com.example.iot.domain.model.DeviceType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private val vm: HomeViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Top bar Home: tiêu đề + subtitle, KHÔNG có mũi tên back
        binding.topBar.title = getString(R.string.title_home)               // "Điều khiển từ xa"
        binding.topBar.subtitle = "Trang chủ"
        binding.topBar.navigationIcon = null
        binding.topBar.inflateMenu(R.menu.menu_home)
        binding.topBar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                findNavController().navigate(R.id.settingsFragment); true
            } else false
        }

        val adapter = RemoteAdapter(
            emptyList(),
            onClick = { card: RemoteCardUi ->
                val args = Bundle().apply { putString("remoteId", card.id) }
                when (card.deviceType) {
                    DeviceType.AC -> findNavController().navigate(R.id.acControlFragment, args)
                    DeviceType.TV -> findNavController().navigate(R.id.tvControlFragment, args)
                    DeviceType.FAN -> findNavController().navigate(R.id.fanControlFragment, args)
                    DeviceType.STB -> findNavController().navigate(R.id.stbControlFragment, args)
                    DeviceType.DVD -> findNavController().navigate(R.id.dvdControlFragment, args)
                    DeviceType.PROJECTOR -> findNavController().navigate(R.id.projectorControlFragment, args)
                    else -> findNavController().navigate(R.id.acControlFragment, args)
                }
            },
            onPower = { card: RemoteCardUi ->
                vm.sendPower(card)
            }
        )
        binding.recyclerRemotes.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.remoteCards.collectLatest { cards ->
                    adapter.submit(cards)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.isEspOnline.collectLatest { online ->
                    android.util.Log.d("MQTT", "Home observe anyNodeOnline=$online")
                    binding.bannerOffline.visibility = if (online) View.GONE else View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.remoteCards.collectLatest { list ->
                    adapter.submit(list)
                }
            }
        }

        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_deviceType)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
