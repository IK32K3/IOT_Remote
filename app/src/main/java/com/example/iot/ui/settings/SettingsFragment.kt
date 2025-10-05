package com.example.iot.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.iot.R
import com.example.iot.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!
    private val vm: SettingsViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val toolbar = b.topBar.root
        toolbar.title = getString(R.string.app_name)
        toolbar.subtitle = "Cài đặt"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { st ->
                    b.edtHost.setText(st.host)
                    b.edtPort.setText(st.port.toString())
                    b.edtNode.setText(st.node)
                }
            }
        }
        b.btnSave.setOnClickListener {
            vm.save(
                b.edtHost.text?.toString().orEmpty(),
                b.edtPort.text?.toString()?.toIntOrNull() ?: 1883,
                b.edtNode.text?.toString().orEmpty()
            )
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
