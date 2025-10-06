package com.example.iot.feature.control.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.iot.R
import com.example.iot.databinding.FragmentControlTvBinding
import com.example.iot.feature.control.common.BaseControlFragment
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TvControlFragment : BaseControlFragment<FragmentControlTvBinding>() {

    override val vm: TvControlViewModel by viewModels()
    private var showingDigits = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentControlTvBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load(remoteId)

        // Topbar
        val toolbar: MaterialToolbar = b.topBar.root
        toolbar.title = getString(R.string.app_name)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        bindToolbar(toolbar)

        // Banner online/offline
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.isNodeOnline.collect { online ->
                    b.bannerOffline.visibility = if (online) View.GONE else View.VISIBLE
                }
            }
        }

        // Toggle 123 <-> Menu (2 chế độ layout)
        fun renderMode() {
            b.groupBasic.visibility  = if (showingDigits) View.GONE else View.VISIBLE
            b.groupDigits.visibility = if (showingDigits) View.VISIBLE else View.GONE
        }
        b.link123.setOnClickListener { showingDigits = true; renderMode() }
        b.linkMenu.setOnClickListener { showingDigits = false; renderMode() }
        renderMode()

        // ========= Gán click cho tất cả nút =========

        // Top row
        b.btnPower.setOnClickListener { vm.togglePower() }
        b.btnMute.setOnClickListener  { vm.toggleMute() }
        b.btnTvAv.setOnClickListener  { vm.tvAv() }        // hoặc vm.input()

        // Pills
        b.btnVolUp.setOnClickListener   { vm.volUp() }
        b.btnVolDown.setOnClickListener { vm.volDown() }
        b.btnChUp.setOnClickListener    { vm.chUp() }
        b.btnChDown.setOnClickListener  { vm.chDown() }

        // Center round buttons
        b.btnMenu.setOnClickListener { vm.menu() }
        b.btnExit.setOnClickListener { vm.exit() }

        // Floating faded buttons
        b.btnHome.setOnClickListener { vm.home() }
        b.btnBack.setOnClickListener { vm.back() }

        // D-pad
        b.btnOk.setOnClickListener   { vm.ok() }
        b.btnUp.setOnClickListener   { vm.dirUp() }
        b.btnDown.setOnClickListener { vm.dirDown() }
        b.btnLeft.setOnClickListener { vm.dirLeft() }
        b.btnRight.setOnClickListener{ vm.dirRight() }

        // Digits grid
        b.btnDigit0.setOnClickListener { vm.digit(0) }
        b.btnDigit1.setOnClickListener { vm.digit(1) }
        b.btnDigit2.setOnClickListener { vm.digit(2) }
        b.btnDigit3.setOnClickListener { vm.digit(3) }
        b.btnDigit4.setOnClickListener { vm.digit(4) }
        b.btnDigit5.setOnClickListener { vm.digit(5) }
        b.btnDigit6.setOnClickListener { vm.digit(6) }
        b.btnDigit7.setOnClickListener { vm.digit(7) }
        b.btnDigit8.setOnClickListener { vm.digit(8) }
        b.btnDigit9.setOnClickListener { vm.digit(9) }
        b.btnDash.setOnClickListener   { vm.dash() }         // −/−−
        b.btnDigitBack.setOnClickListener { vm.back() }      // back ở layout digits

    }
}
